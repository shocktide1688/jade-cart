package com.jade.cart.app;

import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.trade.entity.Cart;
import com.jade.cart.trade.entity.Customer;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.entity.OrderItem;
import com.jade.cart.trade.entity.WebhookEvent;
import com.jade.cart.trade.service.CartService;
import com.jade.cart.trade.service.OrderService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交易核心路径测试 (P3-14): 防超卖并发 / 订单状态机 / webhook 幂等 / 预留过期
 * 独立库 jade_test, Flyway 自动迁移
 */
@QuarkusTest
class OrderFlowTest {

    @Inject
    CatalogService catalogService;

    @Inject
    CartService cartService;

    @Inject
    OrderService orderService;

    @Inject
    UserTransaction utx;

    @BeforeEach
    @Transactional
    void clean() {
        // 顺序: 先子表后父表
        WebhookEvent.deleteAll();
        OrderItem.deleteAll();
        Order.deleteAll();
        com.jade.cart.trade.entity.CartItem.deleteAll();
        Cart.deleteAll();
        Customer.deleteAll();
        Inventory.deleteAll();
        Variant.deleteAll();
        com.jade.cart.catalog.entity.ProductOptionValue.deleteAll();
        com.jade.cart.catalog.entity.ProductOption.deleteAll();
        com.jade.cart.catalog.entity.ProductCategoryLink.deleteAll();
        Product.deleteAll();
        com.jade.cart.catalog.entity.Category.deleteAll();
    }

    /** 建一个 ACTIVE 商品 + 单变体 + 指定库存, 返回变体 id */
    @Transactional
    Long seedProduct(int stock) {
        Product p = catalogService.createProduct("Test Tee " + System.nanoTime(), null,
                null, null, Product.STATUS_ACTIVE, false, 1900L, null);
        Variant v = Variant.listByProduct(p.id).get(0);
        // createVariant 已自动建库存行, 这里只改数量
        catalogService.setInventory(v.id, stock);
        return v.id;
    }

    @Test
    void oversell_concurrentReserve_onlyOneWins() throws Exception {
        Long variantId = seedProduct(2); // 库存 2, 每人抢 2 → 只允许 1 人成功

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();

        List<Future<Boolean>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                utx.begin();
                try {
                    boolean ok = Inventory.reserve(variantId, 2);
                    utx.commit();
                    if (ok) {
                        wins.incrementAndGet();
                    }
                    return ok;
                } catch (Exception e) {
                    try { utx.rollback(); } catch (Exception ignored) {}
                    return false;
                }
            }));
        }
        ready.await();
        start.countDown();
        for (Future<Boolean> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(1, wins.get(), "库存 2/每人 2: 8 线程并发只允许 1 个成功");
        Inventory inv = Inventory.byVariant(variantId).orElseThrow();
        assertEquals(2, inv.reserved, "预留只能被占用一次");
    }

    @Test
    void stateMachine_illegalTransitions_rejected() throws Exception {
        Long variantId = seedProduct(10);
        String token = cartService.newToken();
        cartService.addItem(token, variantId, 2);
        Order order = orderService.checkout(token, "t1@example.com", null, "US");

        // PENDING 不能直接履约
        utx.begin();
        boolean illegal = Order.transition(order.id, Order.STATUS_PAID, Order.STATUS_FULFILLED,
                LocalDateTime.now());
        utx.commit();
        assertFalse(illegal, "PENDING→FULFILLED 必须被拒绝");
        // 已取消订单不能再取消
        orderService.cancel(order.publicId);
        utx.begin();
        boolean doubleCancel = Order.transition(order.id, Order.STATUS_PENDING, Order.STATUS_CANCELLED,
                LocalDateTime.now());
        utx.commit();
        assertFalse(doubleCancel, "重复取消必须被拒绝");

        Order reloaded = Order.findById(order.id);
        assertEquals(Order.STATUS_CANCELLED, reloaded.status);
        // 取消释放预留
        Inventory inv = Inventory.byVariant(variantId).orElseThrow();
        assertEquals(0, inv.reserved, "取消必须释放预留");
    }

    @Test
    void webhook_idempotent_doubleMarkPaid_commitsStockOnce() throws Exception {
        Long variantId = seedProduct(10);
        String token = cartService.newToken();
        cartService.addItem(token, variantId, 3);
        Order order = orderService.checkout(token, "t2@example.com", null, "US");

        String eventId = "evt_test_idem_1";
        boolean first = orderService.markPaid("mock", eventId, order.publicId,
                order.totalCents, order.currency, "ref_1");
        boolean second = orderService.markPaid("mock", eventId, order.publicId,
                order.totalCents, order.currency, "ref_1");

        assertTrue(first, "首次回调应消费");
        assertTrue(second, "重复回调按幂等成功返回");

        assertEquals(1, WebhookEvent.count("eventId", eventId), "事件表只落一条");
        assertEquals(1, Order.count("publicId = ?1 and status = ?2",
                order.publicId, Order.STATUS_PAID), "订单只被支付一次");
        Inventory inv = Inventory.byVariant(variantId).orElseThrow();
        assertEquals(7, inv.quantity, "库存只核销一次 (10-3)");
        assertEquals(0, inv.reserved, "预留清零且不重复扣");
    }

    @Test
    void reservationExpiry_cancelledAndStockReleased() throws Exception {
        Long variantId = seedProduct(5);
        String token = cartService.newToken();
        cartService.addItem(token, variantId, 2);
        Order order = orderService.checkout(token, "t3@example.com", null, "US");

        // 强制过期
        utx.begin();
        Order o = Order.findById(order.id);
        o.reservationExpiresAt = LocalDateTime.now().minusMinutes(1);
        utx.commit();

        int cancelled = orderService.cancelExpiredReservations();
        assertTrue(cancelled >= 1, "过期订单被清扫");

        Order reloaded = Order.findById(order.id);
        assertEquals(Order.STATUS_CANCELLED, reloaded.status);
        Inventory inv = Inventory.byVariant(variantId).orElseThrow();
        assertEquals(0, inv.reserved, "过期后预留释放");
        assertEquals(5, inv.quantity, "在库不变");
    }

    @Test
    void latePayment_onCancelledOrder_goesToReview() throws Exception {
        Long variantId = seedProduct(10);
        String token = cartService.newToken();
        cartService.addItem(token, variantId, 1);
        Order order = orderService.checkout(token, "t4@example.com", null, "US");
        orderService.cancel(order.publicId);

        boolean consumed = orderService.markPaid("mock", "evt_late_1", order.publicId,
                order.totalCents, order.currency, "ref_late");
        assertTrue(consumed, "迟到款事件按已消费返回");

        Order reloaded = Order.findById(order.id);
        assertEquals(Order.STATUS_PAYMENT_REVIEW, reloaded.status, "迟到款转人工审核");
    }
}

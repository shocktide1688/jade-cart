package com.jade.cart.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.settings.entity.StoreSetting;
import com.jade.cart.trade.entity.Cart;
import com.jade.cart.trade.entity.Customer;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.entity.OrderItem;
import com.jade.cart.trade.entity.WebhookEvent;
import com.jade.cart.trade.payment.PaymentSettings;
import com.jade.cart.trade.service.CartService;
import com.jade.cart.trade.service.OrderService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 支付增强测试 (P4): 支付方式按市场路由 (stripe_payment_methods) / 支付对账扫描路径 */
@QuarkusTest
class PaymentReconcileTest {

    @Inject CatalogService catalogService;

    @Inject CartService cartService;

    @Inject OrderService orderService;

    @Inject PaymentSettings paymentSettings;

    @BeforeEach
    @Transactional
    void clean() {
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
        StoreSetting.delete("key", PaymentSettings.K_STRIPE_METHODS);
    }

    @AfterEach
    @Transactional
    void restore() {
        StoreSetting.delete("key", PaymentSettings.K_STRIPE_METHODS);
    }

    // ---------- 支付方式路由 ----------

    @Test
    void stripeMethods_unconfigured_defaultsToCard() {
        assertEquals(List.of("card"), paymentSettings.stripeMethods("US", "USD"));
        assertEquals(List.of("card"), paymentSettings.stripeMethods("EU", "EUR"));
    }

    @Test
    @Transactional
    void stripeMethods_perMarketRouting_andEurFilter() {
        StoreSetting.put(
                PaymentSettings.K_STRIPE_METHODS, "US=card,klarna;EU=card,ideal,bancontact,klarna");

        assertEquals(List.of("card", "klarna"), paymentSettings.stripeMethods("US", "USD"));
        assertEquals(
                List.of("card", "ideal", "bancontact", "klarna"),
                paymentSettings.stripeMethods("EU", "EUR"));
        // EUR-only 方式 (ideal/bancontact) 在非 EUR 订单自动剔除, 不拖垮 USD 市场
        assertEquals(List.of("card", "klarna"), paymentSettings.stripeMethods("EU", "USD"));
        // 未配置的市场回退 card
        assertEquals(List.of("card"), paymentSettings.stripeMethods("JP", "JPY"));
    }

    @Test
    @Transactional
    void stripeMethods_defaultSegment_appliesToUnlistedMarkets() {
        StoreSetting.put(PaymentSettings.K_STRIPE_METHODS, "DEFAULT=card,link;EU=card,ideal");

        assertEquals(List.of("card", "link"), paymentSettings.stripeMethods("US", "USD"));
        assertEquals(List.of("card", "ideal"), paymentSettings.stripeMethods("EU", "EUR"));
    }

    // ---------- 对账扫描 ----------

    @Test
    void reconcile_mockOrders_skippedAndUntouched() {
        Product p =
                catalogService.createProduct(
                        "Recon Tee " + System.nanoTime(),
                        null,
                        null,
                        null,
                        Product.STATUS_ACTIVE,
                        false,
                        1900L,
                        null,
                        null,
                        null,
                        null);
        Variant v = Variant.listByProduct(p.id).get(0);
        catalogService.setInventory(v.id, 5);

        String token = cartService.newToken();
        cartService.addItem(token, v.id, 1);
        Order order = orderService.checkout(token, "recon@example.com", null, "US");
        orderService.attachPaymentSession(order.publicId, "mock_" + order.publicId);

        // mock 渠道不支持主动查询 → 对账扫描不误伤
        int recovered = orderService.reconcilePayments();
        assertEquals(0, recovered, "mock 订单不应被对账处理");

        Order reloaded = Order.findById(order.id);
        assertEquals(Order.STATUS_PENDING, reloaded.status, "订单状态不被对账改动");
        assertTrue(reloaded.paymentSessionRef != null, "会话号已落库");
    }
}

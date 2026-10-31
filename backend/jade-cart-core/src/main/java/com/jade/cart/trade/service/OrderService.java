package com.jade.cart.trade.service;

import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.mail.service.OutboxService;
import com.jade.cart.shipping.entity.Shipment;
import com.jade.cart.shipping.service.ShippingService;
import com.jade.cart.trade.dto.OrderView;
import com.jade.cart.trade.entity.Cart;
import com.jade.cart.trade.entity.CartItem;
import com.jade.cart.trade.entity.Customer;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.entity.OrderItem;
import com.jade.cart.trade.entity.WebhookEvent;
import com.jade.cart.trade.payment.PaymentProvider;
import com.jade.cart.trade.payment.PaymentSettings;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 订单: 结算单事务 (预留库存→运费快照→客户 upsert→订单快照→购物车 CONVERTED) 支付回调: webhook 幂等表 + 金额/币种复核 + 条件状态流转 + 预留转实扣 +
 * 邮件入队 渠道选择: stripe 已配置 → paypal 已配置 → mock 兜底
 */
@ApplicationScoped
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NUM_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Inject CatalogService catalogService;

    @Inject ShippingService shippingService;

    @Inject PaymentSettings paymentSettings;

    @Inject com.jade.cart.settings.service.MarketService marketService;

    @Inject OutboxService outboxService;

    @Inject Instance<PaymentProvider> paymentProviders;

    @ConfigProperty(name = "jade.cart.reservation-minutes", defaultValue = "30")
    int reservationMinutes;

    @Deprecated // 由 marketService.currency() 接管, 保留字段兼容旧配置引用
    @ConfigProperty(name = "jade.cart.currency", defaultValue = "USD")
    String currency;

    // ---------- 渠道选择 ----------

    /** 按配置顺序选首个可用渠道: stripe → paypal → mock */
    public PaymentProvider selectProvider() {
        for (PaymentProvider p : paymentProviders) {
            if ("mock".equals(p.name())) {
                continue;
            }
            if (p.isConfigured()) {
                return p;
            }
        }
        return provider("mock").orElseThrow();
    }

    public Optional<PaymentProvider> provider(String name) {
        return paymentProviders.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    public PaymentProvider providerOf(Order order) {
        return provider(order.paymentProvider == null ? "mock" : order.paymentProvider)
                .orElseThrow(() -> new BizException("支付渠道不可用: " + order.paymentProvider));
    }

    // ---------- 结算 ----------

    /** 结算 (单事务): 逐行条件 UPDATE 预留, 任一行不足整单回滚 (防超卖) */
    @Transactional
    public Order checkout(String token, String email, String shipName, String country) {
        Cart cart = Cart.activeByToken(token).orElseThrow(() -> new BizException("购物车为空"));
        List<CartItem> items = cart.items();
        if (items.isEmpty()) {
            throw new BizException("购物车为空");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BizException("请输入有效邮箱");
        }
        String shipCountry =
                country == null || country.isBlank()
                        ? "US"
                        : country.trim().toUpperCase(Locale.ROOT);

        long subtotal = 0;
        Order order = new Order();
        order.email = email.trim();
        order.shipName = shipName;
        order.currency = marketService.currency();
        order.shippingCountry = shipCountry;

        // ① 预留库存 (条件 UPDATE, 0 行 = 超卖拦截)
        List<OrderItem> snapshots = new java.util.ArrayList<>();
        for (CartItem item : items) {
            Variant variant = Variant.findById(item.variantId);
            if (variant == null || variant.deleted) {
                throw new BizException("购物车中存在已下架商品, 请移除后重试");
            }
            if (!Inventory.reserve(variant.id, item.quantity)) {
                throw new BizException("库存不足: " + variant.sku);
            }
            Product product = Product.findById(variant.productId);
            OrderItem line = new OrderItem();
            line.productTitle = product == null ? variant.sku : product.title;
            line.variantId = variant.id;
            line.variantSpec = catalogService.specText(variant.id);
            line.sku = variant.sku;
            line.unitPriceCents = variant.priceCents;
            line.quantity = item.quantity;
            line.lineTotalCents = variant.priceCents * item.quantity;
            subtotal += line.lineTotalCents;
            snapshots.add(line);
        }

        // ② 运费快照 (分区匹配 + 满额免邮)
        order.subtotalCents = subtotal;
        order.shippingCents = shippingService.shippingFor(shipCountry, subtotal);
        order.totalCents = subtotal + order.shippingCents;

        // ③ 客户 upsert (按邮箱)
        Customer customer =
                Customer.byEmail(order.email)
                        .orElseGet(
                                () -> {
                                    Customer c = new Customer();
                                    c.email = order.email;
                                    c.name = shipName;
                                    c.persist();
                                    return c;
                                });

        // ④ 订单落库 (30 分钟预留期限)
        order.customerId = customer.id;
        order.orderNumber = nextOrderNumber();
        order.reservationExpiresAt = LocalDateTime.now().plusMinutes(reservationMinutes);
        order.paymentProvider = selectProvider().name();
        order.persist();

        // ⑤ 订单行快照
        for (OrderItem line : snapshots) {
            line.orderId = order.id;
            line.persist();
        }

        // ⑥ 购物车 CONVERTED
        cart.status = Cart.STATUS_CONVERTED;
        cart.persist();

        // ⑦ 下单确认邮件入队 (同事务, outbox 唯一约束幂等)
        outboxService.enqueueOrderConfirm(order);

        Log.infof(
                "checkout ok: order=%s amount=%d (ship=%d) via %s",
                order.orderNumber, order.totalCents, order.shippingCents, order.paymentProvider);
        return order;
    }

    // ---------- 支付回调 ----------

    /**
     * markPaid (kartwo 同款路径): ① webhook 幂等表去重 → ② 金额/币种复核 → ③ 条件流转 PENDING→PAID → ④ 预留转实扣 → ⑤
     * 支付确认邮件入队; 迟到款转 PAYMENT_REVIEW
     */
    @Transactional
    public boolean markPaid(
            String provider,
            String eventId,
            String orderPublicId,
            long amountCents,
            String webhookCurrency,
            String paymentRef) {
        // ① 幂等: 已处理过直接成功
        if (!WebhookEvent.insertIfAbsent(
                provider,
                eventId,
                orderPublicId,
                "amount=" + amountCents + " currency=" + webhookCurrency)) {
            Log.infof("webhook duplicate ignored: %s/%s", provider, eventId);
            return true;
        }

        Optional<Order> found = Order.byPublicId(orderPublicId);
        if (found.isEmpty()) {
            Log.errorf(
                    "webhook for unknown order: %s/%s order=%s", provider, eventId, orderPublicId);
            return true;
        }
        Order order = found.get();

        // ② 金额/币种第二道校验
        if (order.totalCents != amountCents) {
            Log.errorf(
                    "webhook amount mismatch: order=%s expect=%d got=%d (需人工核对)",
                    order.orderNumber, order.totalCents, amountCents);
            return true;
        }

        // ③ 条件流转 PENDING → PAID
        boolean paid =
                Order.transition(
                        order.id, Order.STATUS_PENDING, Order.STATUS_PAID, LocalDateTime.now());
        if (!paid) {
            if (Order.STATUS_CANCELLED.equals(order.status)) {
                boolean review =
                        Order.transition(
                                order.id,
                                Order.STATUS_CANCELLED,
                                Order.STATUS_PAYMENT_REVIEW,
                                LocalDateTime.now());
                Log.warnf(
                        "late payment on cancelled order=%s -> PAYMENT_REVIEW=%b (需人工退款, 禁止发货)",
                        order.orderNumber, review);
                return true;
            }
            Log.warnf(
                    "webhook on non-pending order=%s status=%s ignored",
                    order.orderNumber, order.status);
            return true;
        }
        // 条件 UPDATE 绕过持久化上下文, 同步内存实体避免脏检查把旧状态刷回去
        order.status = Order.STATUS_PAID;
        order.paidAt = LocalDateTime.now();

        // ④ 预留转实扣
        for (OrderItem line : order.items()) {
            if (line.variantId != null
                    && !Inventory.commitReserved(line.variantId, line.quantity)) {
                Log.errorf(
                        "commit reserved failed: order=%s variant=%d qty=%d (需人工修库存)",
                        order.orderNumber, line.variantId, line.quantity);
            }
        }

        order.paymentRef = paymentRef;
        order.persist();

        // ⑤ 支付确认邮件入队
        outboxService.enqueuePaymentConfirm(order);

        Log.infof("order paid: %s via %s ref=%s", order.orderNumber, provider, paymentRef);
        return true;
    }

    // ---------- 管理操作 ----------

    /** 履约: PAID → FULFILLED + 发货追踪 + 邮件入队 */
    @Transactional
    public Order fulfill(
            String publicId, String carrier, String trackingNumber, String trackingUrl) {
        Order order = mustFind(publicId);
        if (!Order.transition(
                order.id, Order.STATUS_PAID, Order.STATUS_FULFILLED, LocalDateTime.now())) {
            throw new BizException("只有已支付订单才能履约 (当前: " + order.status + ")");
        }
        order.status = Order.STATUS_FULFILLED;
        order.fulfilledAt = LocalDateTime.now();

        Shipment shipment = Shipment.byOrder(order.id).orElse(new Shipment());
        shipment.orderId = order.id;
        shipment.carrier = carrier;
        shipment.trackingNumber = trackingNumber;
        shipment.trackingUrl = trackingUrl;
        shipment.persist();

        outboxService.enqueueShipped(order, carrier, trackingNumber, trackingUrl);
        Log.infof(
                "order fulfilled: %s carrier=%s tracking=%s",
                order.orderNumber, carrier, trackingNumber);
        return Order.findById(order.id);
    }

    @Transactional
    public Order cancel(String publicId) {
        Order order = mustFind(publicId);
        if (!Order.transition(
                order.id, Order.STATUS_PENDING, Order.STATUS_CANCELLED, LocalDateTime.now())) {
            throw new BizException("只有待支付订单才能取消 (当前: " + order.status + ")");
        }
        order.status = Order.STATUS_CANCELLED;
        order.cancelledAt = LocalDateTime.now();
        releaseReserved(order);
        Log.infof("order cancelled: %s", order.orderNumber);
        return Order.findById(order.id);
    }

    /** 退款: 先调网关 (mock 直转), 成功后流转状态 */
    @Transactional
    public Order refund(String publicId) {
        Order order = mustFind(publicId);
        boolean canRefund =
                Order.STATUS_PAID.equals(order.status)
                        || Order.STATUS_PAYMENT_REVIEW.equals(order.status);
        if (!canRefund) {
            throw new BizException("只有已支付/待审核订单才能退款 (当前: " + order.status + ")");
        }
        PaymentProvider gateway = providerOf(order);
        PaymentProvider.RefundResult result = gateway.refund(order); // 抛异常 = 退款失败, 状态不变

        boolean ok =
                Order.transition(
                        order.id, Order.STATUS_PAID, Order.STATUS_REFUNDED, LocalDateTime.now());
        if (!ok) {
            ok =
                    Order.transition(
                            order.id,
                            Order.STATUS_PAYMENT_REVIEW,
                            Order.STATUS_REFUNDED,
                            LocalDateTime.now());
        }
        if (!ok) {
            throw new BizException("订单状态已变化, 请刷新重试");
        }
        order.status = Order.STATUS_REFUNDED;
        order.refundedAt = LocalDateTime.now();
        Log.infof(
                "order refunded: %s via %s refund=%s",
                order.orderNumber, gateway.name(), result.providerRefundId());
        return Order.findById(order.id);
    }

    /** 预留过期清扫: 过期 PENDING → CANCELLED + 释放 reserved */
    @Transactional
    public int cancelExpiredReservations() {
        List<Order> expired =
                Order.<Order>find(
                                "status = ?1 and reservationExpiresAt < ?2",
                                Order.STATUS_PENDING,
                                LocalDateTime.now())
                        .list();
        for (Order order : expired) {
            if (Order.transition(
                    order.id, Order.STATUS_PENDING, Order.STATUS_CANCELLED, LocalDateTime.now())) {
                releaseReserved(order);
                Log.infof("order reservation expired, cancelled: %s", order.orderNumber);
            }
        }
        return expired.size();
    }

    // ---------- 查询/视图 ----------

    public List<Order> pageOrders(String status, String keyword, int page, int size) {
        StringBuilder where = new StringBuilder("1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (status != null && !status.isBlank()) {
            where.append(" and status = :st");
            params.put("st", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (lower(orderNumber) like :kw or lower(email) like :kw)");
            params.put("kw", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        }
        return Order.find(where.toString() + " order by createdAt desc", params)
                .page(Math.max(page - 1, 0), size)
                .list();
    }

    public long countOrders(String status, String keyword) {
        StringBuilder where = new StringBuilder("1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (status != null && !status.isBlank()) {
            where.append(" and status = :st");
            params.put("st", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (lower(orderNumber) like :kw or lower(email) like :kw)");
            params.put("kw", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        }
        return Order.count(where.toString(), params);
    }

    public OrderView toView(Order order) {
        Shipment shipment = Shipment.byOrder(order.id).orElse(null);
        return new OrderView(
                order.publicId,
                order.orderNumber,
                order.email,
                order.shipName,
                order.status,
                order.subtotalCents,
                order.shippingCents,
                order.totalCents,
                order.currency,
                order.shippingCountry,
                order.paymentProvider,
                order.paymentRef,
                order.reservationExpiresAt,
                order.paidAt,
                order.fulfilledAt,
                order.cancelledAt,
                order.refundedAt,
                order.createdAt,
                order.items().stream()
                        .map(
                                line ->
                                        new OrderView.ItemView(
                                                line.variantId,
                                                line.productTitle,
                                                line.variantSpec,
                                                line.sku,
                                                line.unitPriceCents,
                                                line.quantity,
                                                line.lineTotalCents))
                        .toList(),
                shipment == null
                        ? null
                        : new OrderView.ShipmentView(
                                shipment.carrier, shipment.trackingNumber, shipment.trackingUrl));
    }

    // ---------- 内部 ----------

    private Order mustFind(String publicId) {
        return Order.byPublicId(publicId).orElseThrow(() -> new BizException("订单不存在"));
    }

    private void releaseReserved(Order order) {
        for (OrderItem line : order.items()) {
            if (line.variantId != null) {
                Inventory.release(line.variantId, line.quantity);
            }
        }
    }

    private String nextOrderNumber() {
        for (int i = 0; i < 5; i++) {
            String candidate =
                    "JC"
                            + LocalDateTime.now().format(NUM_FMT)
                            + String.format("%04d", RANDOM.nextInt(10000));
            if (Order.count("orderNumber", candidate) == 0) {
                return candidate;
            }
        }
        throw new BizException("订单号生成失败, 请重试");
    }
}

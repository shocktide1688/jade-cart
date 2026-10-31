package com.jade.cart.trade.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 订单状态机:
 *
 * <pre>
 * PENDING --支付--> PAID --履约--> FULFILLED
 *    |--超时/取消--> CANCELLED    PAID/REVIEW --退款--> REFUNDED
 *    '--迟到款--> PAYMENT_REVIEW (需人工处理, 禁止发货)
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_order")
public class Order extends PanacheEntityBase {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_FULFILLED = "FULFILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_PAYMENT_REVIEW = "PAYMENT_REVIEW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    public String orderNumber;

    @Column(name = "customer_id")
    public Long customerId;

    @Column(nullable = false, length = 200)
    public String email;

    @Column(name = "ship_name", length = 100)
    public String shipName;

    @Column(nullable = false, length = 30)
    public String status = STATUS_PENDING;

    @Column(name = "subtotal_cents", nullable = false)
    public Long subtotalCents = 0L;

    /** P1 运费分区接入, P0 恒 0 */
    @Column(name = "shipping_cents", nullable = false)
    public Long shippingCents = 0L;

    @Column(name = "total_cents", nullable = false)
    public Long totalCents = 0L;

    @Column(nullable = false, length = 10)
    public String currency = "USD";

    /** 收货国家 (ISO2), 运费分区匹配用 */
    @Column(name = "shipping_country", nullable = false, length = 2)
    public String shippingCountry = "US";

    @Column(name = "payment_provider", length = 30)
    public String paymentProvider;

    @Column(name = "payment_ref", length = 120)
    public String paymentRef;

    @Column(name = "reservation_expires_at")
    public LocalDateTime reservationExpiresAt;

    @Column(name = "paid_at")
    public LocalDateTime paidAt;

    @Column(name = "fulfilled_at")
    public LocalDateTime fulfilledAt;

    @Column(name = "cancelled_at")
    public LocalDateTime cancelledAt;

    @Column(name = "refunded_at")
    public LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public List<OrderItem> items() {
        return OrderItem.listByOrder(id);
    }

    public static Optional<Order> byPublicId(String publicId) {
        return find("publicId", publicId).firstResultOptional();
    }

    /** 条件状态流转: 返回 false = 状态已变化 (并发/迟到) */
    public static boolean transition(Long id, String from, String to, LocalDateTime at) {
        String atColumn =
                switch (to) {
                    case STATUS_PAID -> ", paidAt = ?3";
                    case STATUS_FULFILLED -> ", fulfilledAt = ?3";
                    case STATUS_CANCELLED -> ", cancelledAt = ?3";
                    case STATUS_REFUNDED -> ", refundedAt = ?3";
                    default -> "";
                };
        if (atColumn.isEmpty()) {
            return update("status = ?2 where id = ?1 and status = ?3", id, to, from) == 1;
        }
        return update("status = ?2" + atColumn + " where id = ?1 and status = ?4", id, to, at, from)
                == 1;
    }
}

package com.jade.cart.shipping.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

/** 发货追踪 (单次履约: 一单一shipment) */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_shipment")
public class Shipment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    public Long orderId;

    @Column(length = 100)
    public String carrier;

    @Column(name = "tracking_number", length = 120)
    public String trackingNumber;

    @Column(name = "tracking_url", length = 300)
    public String trackingUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    public static Optional<Shipment> byOrder(Long orderId) {
        return find("orderId", orderId).firstResultOptional();
    }
}

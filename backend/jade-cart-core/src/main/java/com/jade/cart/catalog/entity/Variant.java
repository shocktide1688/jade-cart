package com.jade.cart.catalog.entity;

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

/** 变体 (SKU): 价格挂在变体上; 无变体商品自动带一条默认变体 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_variant")
public class Variant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(name = "product_id", nullable = false)
    public Long productId;

    @Column(nullable = false, unique = true, length = 120)
    public String sku;

    /** 整数分 */
    @Column(name = "price_cents", nullable = false)
    public Long priceCents = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(nullable = false)
    public boolean deleted = false;

    public static List<Variant> listByProduct(Long productId) {
        return list("productId = ?1 and deleted = false order by id asc", productId);
    }

    public static Optional<Variant> findBySku(String sku) {
        return find("sku = ?1 and deleted = false", sku).firstResultOptional();
    }
}

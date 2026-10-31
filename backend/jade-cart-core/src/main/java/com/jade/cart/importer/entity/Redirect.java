package com.jade.cart.importer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Shopify 旧 handle → 新商品页 301 (仅导入执行时写入, 不覆盖)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_redirect")
public class Redirect extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 200)
    public String handle;

    @Column(nullable = false, length = 300)
    public String target;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    public static Optional<Redirect> byHandle(String handle) {
        return find("handle", handle).firstResultOptional();
    }
}

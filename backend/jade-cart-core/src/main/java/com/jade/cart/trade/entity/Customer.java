package com.jade.cart.trade.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 客户: 访客下单按邮箱 upsert, 无密码 (kartwo 同款)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_customer")
public class Customer extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 200)
    public String email;

    @Column(length = 100)
    public String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static Optional<Customer> byEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}

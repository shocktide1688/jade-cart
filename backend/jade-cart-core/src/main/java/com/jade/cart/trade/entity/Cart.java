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

/** 匿名购物车 (cookie token 标识); 结算成功后置 CONVERTED, 同 token 下次再建新 ACTIVE */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_cart")
public class Cart extends PanacheEntityBase {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CONVERTED = "CONVERTED";
    public static final String STATUS_ABANDONED = "ABANDONED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 64)
    public String token;

    @Column(nullable = false, length = 20)
    public String status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static Optional<Cart> activeByToken(String token) {
        return find("token = ?1 and status = ?2", token, STATUS_ACTIVE).firstResultOptional();
    }

    public List<CartItem> items() {
        return CartItem.listByCart(id);
    }
}

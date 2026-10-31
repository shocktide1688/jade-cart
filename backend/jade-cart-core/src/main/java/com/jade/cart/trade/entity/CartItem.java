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
import java.util.List;
import java.util.Optional;

/**
 * 购物车行: 不存单价快照 (kartwo 同款), 结算时落 order_item
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_cart_item")
public class CartItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "cart_id", nullable = false)
    public Long cartId;

    @Column(name = "variant_id", nullable = false)
    public Long variantId;

    @Column(nullable = false)
    public Integer quantity = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static List<CartItem> listByCart(Long cartId) {
        return list("cartId = ?1 order by id asc", cartId);
    }

    public static Optional<CartItem> byCartAndVariant(Long cartId, Long variantId) {
        return find("cartId = ?1 and variantId = ?2", cartId, variantId).firstResultOptional();
    }

    public static long deleteByCartAndVariant(Long cartId, Long variantId) {
        return delete("cartId = ?1 and variantId = ?2", cartId, variantId);
    }
}

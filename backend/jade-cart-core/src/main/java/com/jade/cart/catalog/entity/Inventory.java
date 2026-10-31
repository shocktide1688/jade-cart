package com.jade.cart.catalog.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 库存: quantity 实物在库 + reserved 未付款占用, 可售 = quantity - reserved
 *
 * 修改走条件 UPDATE (防超卖), 不走实体字段赋值:
 *  - 结算预留: reserved += n 仅当 quantity - reserved >= n
 *  - 支付完成: quantity -= n 且 reserved -= n 仅当 reserved >= n
 *  - 取消/过期: reserved = greatest(reserved - n, 0)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_inventory")
public class Inventory extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "variant_id", nullable = false, unique = true)
    public Long variantId;

    @Column(nullable = false)
    public Integer quantity = 0;

    @Column(nullable = false)
    public Integer reserved = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static Optional<Inventory> byVariant(Long variantId) {
        return find("variantId", variantId).firstResultOptional();
    }

    public int available() {
        return Math.max(quantity - reserved, 0);
    }

    /** 结算预留: 返回 false = 库存不足 (0 行更新) */
    public static boolean reserve(Long variantId, int qty) {
        return update("reserved = reserved + ?1, updatedAt = CURRENT_TIMESTAMP " +
                "WHERE variantId = ?2 AND quantity - reserved >= ?1", qty, variantId) == 1;
    }

    /** 支付完成: 预留转实扣 */
    public static boolean commitReserved(Long variantId, int qty) {
        return update("quantity = quantity - ?1, reserved = reserved - ?1, updatedAt = CURRENT_TIMESTAMP " +
                "WHERE variantId = ?2 AND reserved >= ?1", qty, variantId) == 1;
    }

    /** 释放预留 (取消/超时) */
    public static void release(Long variantId, int qty) {
        update("reserved = GREATEST(reserved - ?1, 0), updatedAt = CURRENT_TIMESTAMP WHERE variantId = ?2", qty, variantId);
    }

    /** 管理端设置在库数量 (可售占用不受影响) */
    public static void setQuantity(Long variantId, int quantity) {
        update("quantity = ?1, updatedAt = CURRENT_TIMESTAMP WHERE variantId = ?2", quantity, variantId);
    }
}

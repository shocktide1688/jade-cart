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

import java.util.List;

/**
 * 订单行: 结算时落快照 (标题/规格/SKU/单价), 与商品后续改动解耦
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_order_item")
public class OrderItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "variant_id")
    public Long variantId;

    @Column(name = "product_title", nullable = false, length = 200)
    public String productTitle;

    @Column(name = "variant_spec", length = 300)
    public String variantSpec;

    @Column(length = 120)
    public String sku;

    @Column(name = "unit_price_cents", nullable = false)
    public Long unitPriceCents;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "line_total_cents", nullable = false)
    public Long lineTotalCents;

    public static List<OrderItem> listByOrder(Long orderId) {
        return list("orderId = ?1 order by id asc", orderId);
    }
}

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

import java.util.List;

/**
 * 商品变体轴 (如 color / size)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_product_option")
public class ProductOption extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "product_id", nullable = false)
    public Long productId;

    /** 规范化轴名, 变体组合签名的一部分 */
    @Column(name = "option_key", nullable = false, length = 50)
    public String optionKey;

    @Column(name = "display_name", nullable = false, length = 100)
    public String displayName;

    @Column(name = "sort_order", nullable = false)
    public Integer sortOrder = 0;

    public static List<ProductOption> listByProduct(Long productId) {
        return list("productId = ?1 order by sortOrder asc, id asc", productId);
    }
}

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
 * 商品 × 分类 多对多关联
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_product_category")
public class ProductCategoryLink extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "product_id", nullable = false)
    public Long productId;

    @Column(name = "category_id", nullable = false)
    public Long categoryId;

    public static List<ProductCategoryLink> listByProduct(Long productId) {
        return list("productId", productId);
    }

    public static void unlinkProduct(Long productId) {
        delete("productId", productId);
    }
}

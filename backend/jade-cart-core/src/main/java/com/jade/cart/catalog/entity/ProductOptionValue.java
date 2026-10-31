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
 * 变体轴取值 (如 color=Red)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_product_option_value")
public class ProductOptionValue extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "option_id", nullable = false)
    public Long optionId;

    @Column(nullable = false, length = 100)
    public String value;

    @Column(name = "sort_order", nullable = false)
    public Integer sortOrder = 0;

    public static List<ProductOptionValue> listByOption(Long optionId) {
        return list("optionId = ?1 order by sortOrder asc, id asc", optionId);
    }
}

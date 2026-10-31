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
 * 变体 × 轴取值 关联; 组合签名 = 排序后的 "optionKey=value" 对
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_variant_option_value")
public class VariantOptionValue extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "variant_id", nullable = false)
    public Long variantId;

    @Column(name = "option_id", nullable = false)
    public Long optionId;

    @Column(nullable = false, length = 100)
    public String value;

    public static List<VariantOptionValue> listByVariant(Long variantId) {
        return list("variantId", variantId);
    }
}

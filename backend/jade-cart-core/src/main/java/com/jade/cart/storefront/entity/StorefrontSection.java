package com.jade.cart.storefront.entity;

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
import java.util.List;

/**
 * 店面装修区块: 页面 = 有序 Section 列表 (type → SectionRenderer 注册表渲染)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_storefront_section")
public class StorefrontSection extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 目前仅 home, 预留多页 */
    @Column(nullable = false, length = 50)
    public String page = "home";

    @Column(nullable = false)
    public Integer position = 0;

    @Column(nullable = false, length = 40)
    public String type;

    /** JSON 配置 (渲染端白名单字段解析) */
    @Column(columnDefinition = "TEXT")
    public String config;

    @Column(nullable = false)
    public boolean visible = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static List<StorefrontSection> listVisible(String page) {
        return list("page = ?1 and visible = true order by position asc, id asc", page);
    }

    public static List<StorefrontSection> listPage(String page) {
        return list("page = ?1 order by position asc, id asc", page);
    }
}

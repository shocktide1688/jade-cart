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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品 (状态机 DRAFT/ACTIVE/ARCHIVED; 首页精选 featured)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_product")
public class Product extends PanacheEntityBase {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 200)
    public String title;

    @Column(nullable = false, unique = true, length = 200)
    public String slug;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "seo_description", length = 500)
    public String seoDescription;

    /** SEO 关键词 (逗号分隔, 店面 meta keywords) */
    @Column(name = "seo_keywords", length = 300)
    public String seoKeywords;

    @Column(nullable = false, length = 20)
    public String status = STATUS_DRAFT;

    @Column(nullable = false)
    public boolean featured = false;

    /** 商品主图 (相对 media 根目录路径) */
    @Column(name = "main_image_path", length = 300)
    public String mainImagePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(nullable = false)
    public boolean deleted = false;

    public static Product findBySlug(String slug) {
        return find("slug = ?1 and deleted = false", slug).firstResult();
    }

    /** 店面可售: ACTIVE */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}

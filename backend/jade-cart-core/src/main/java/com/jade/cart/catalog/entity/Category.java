package com.jade.cart.catalog.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 商品分类 (父子层级, slug 唯一) */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_category")
public class Category extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false, unique = true, length = 140)
    public String slug;

    @Column(name = "parent_id")
    public Long parentId;

    @Column(name = "sort_order", nullable = false)
    public Integer sortOrder = 0;

    /** 1 启用 0 停用 */
    @Column(nullable = false)
    public Integer status = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(nullable = false)
    public boolean deleted = false;

    public static List<Category> listActive() {
        return list("status = 1 and deleted = false order by sortOrder asc, id asc");
    }

    public static List<Category> listAllLive() {
        return list("deleted = false order by sortOrder asc, id asc");
    }

    public static Category findBySlug(String slug) {
        return find("slug = ?1 and deleted = false", slug).firstResult();
    }
}

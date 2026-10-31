package com.jade.cart.blog.entity;

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
import java.util.Optional;
import java.util.UUID;

/**
 * 博客文章 (SEO 内容载体): 受限 Markdown + 封面 + SEO 描述
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_blog_post")
public class BlogPost extends PanacheEntityBase {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 200)
    public String slug;

    @Column(nullable = false, length = 200)
    public String title;

    /** 列表摘要 (空则取正文前段) */
    @Column(columnDefinition = "VARCHAR(400)")
    public String excerpt;

    @Column(name = "body_markdown", columnDefinition = "TEXT")
    public String bodyMarkdown;

    @Column(name = "body_html", columnDefinition = "TEXT")
    public String bodyHtml;

    /** 封面图 (相对 media 路径) */
    @Column(name = "cover_path", length = 300)
    public String coverPath;

    @Column(name = "seo_description", length = 500)
    public String seoDescription;

    @Column(nullable = false, length = 20)
    public String status = STATUS_DRAFT;

    @Column(name = "published_at")
    public LocalDateTime publishedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static List<BlogPost> listActive() {
        return list("status = ?1 order by publishedAt desc nulls last, id desc", STATUS_ACTIVE);
    }

    public static Optional<BlogPost> activeBySlug(String slug) {
        return find("slug = ?1 and status = ?2", slug, STATUS_ACTIVE).firstResultOptional();
    }
}

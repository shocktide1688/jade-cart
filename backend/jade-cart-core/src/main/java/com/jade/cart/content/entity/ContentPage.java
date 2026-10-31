package com.jade.cart.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UpdateTimestamp;

/** 内容页 (受限 Markdown → 安全 HTML; 页脚政策页由生成器幂等写入) */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_content_page")
public class ContentPage extends PanacheEntityBase {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DRAFT = "DRAFT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 140)
    public String slug;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(name = "body_markdown", columnDefinition = "TEXT")
    public String bodyMarkdown;

    @Column(name = "body_html", columnDefinition = "TEXT")
    public String bodyHtml;

    @Column(nullable = false, length = 20)
    public String status = STATUS_ACTIVE;

    /** 政策生成器产物 ( regeneration 时覆盖) */
    @Column(nullable = false)
    public boolean policy = false;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static Optional<ContentPage> activeBySlug(String slug) {
        return find("slug = ?1 and status = ?2", slug, STATUS_ACTIVE).firstResultOptional();
    }

    public static List<ContentPage> listActive() {
        return list("status = ?1 order by id asc", STATUS_ACTIVE);
    }
}

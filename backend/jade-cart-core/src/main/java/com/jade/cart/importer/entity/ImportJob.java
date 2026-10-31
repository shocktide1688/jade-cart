package com.jade.cart.importer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

/** 导入任务: source_sha256 唯一 = 同文件幂等; 干跑预览 → 确认执行 (kartwo 0012 对齐) */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_import_job")
public class ImportJob extends PanacheEntityBase {

    public static final String STATUS_PREVIEW = "PREVIEW";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 200)
    public String filename;

    @Column(name = "source_format", nullable = false, length = 20)
    public String sourceFormat = "generic";

    @Column(name = "source_sha256", nullable = false, unique = true, length = 64)
    public String sourceSha256;

    @Column(nullable = false, length = 20)
    public String status = STATUS_PREVIEW;

    @Column(name = "total_rows", nullable = false)
    public Integer totalRows = 0;

    @Column(name = "error_rows", nullable = false)
    public Integer errorRows = 0;

    @Column(name = "product_count", nullable = false)
    public Integer productCount = 0;

    @Column(name = "imported_count", nullable = false)
    public Integer importedCount = 0;

    /** 解析结果 JSON (产品数组 + 行级错误), 确认时据此执行 */
    @Column(name = "preview_json", columnDefinition = "TEXT")
    public String previewJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;

    public static Optional<ImportJob> bySha256(String sha256) {
        return find("sourceSha256", sha256).firstResultOptional();
    }
}

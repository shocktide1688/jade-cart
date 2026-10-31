package com.jade.cart.media.entity;

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

/** 媒体资产: 磁盘内容哈希命名存储, DB 只存相对路径 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_media_asset")
public class MediaAsset extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 200)
    public String filename;

    /** SHA-256 内容哈希 (hex) */
    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    public String contentHash;

    @Column(name = "content_type", nullable = false, length = 100)
    public String contentType;

    @Column(name = "size_bytes", nullable = false)
    public Long sizeBytes;

    @Column(nullable = false)
    public Integer width = 0;

    @Column(nullable = false)
    public Integer height = 0;

    /** 相对 media 根目录的路径 */
    @Column(nullable = false, length = 300)
    public String path;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    public static Optional<MediaAsset> byHash(String hash) {
        return find("contentHash", hash).firstResultOptional();
    }
}

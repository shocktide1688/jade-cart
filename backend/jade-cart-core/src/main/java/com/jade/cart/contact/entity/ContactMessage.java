package com.jade.cart.contact.entity;

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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 客服留言 (私密工单; 到期匿名化 IP, 不删除)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_contact_message")
public class ContactMessage extends PanacheEntityBase {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    public String publicId = UUID.randomUUID().toString();

    @Column(length = 100)
    public String name;

    @Column(nullable = false, length = 200)
    public String email;

    @Column(nullable = false, length = 40)
    public String topic = "general";

    @Column(nullable = false, columnDefinition = "TEXT")
    public String message;

    @Column(nullable = false, length = 20)
    public String status = STATUS_NEW;

    /** 客户端 IP 的 SHA-256 (不存明文, 到期置空) */
    @Column(name = "ip_hash", length = 64)
    public String ipHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "processed_at")
    public LocalDateTime processedAt;

    @Column(name = "anonymized_at")
    public LocalDateTime anonymizedAt;
}

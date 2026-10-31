package com.jade.cart.mail.entity;

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
import java.util.List;

/**
 * 邮件 outbox: 与业务同事务入队, worker 异步投递 (指数退避→死信)
 * (order_id, kind) 唯一约束保证幂等
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_email_outbox")
public class EmailOutbox extends PanacheEntityBase {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DEAD = "DEAD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id")
    public Long orderId;

    @Column(nullable = false, length = 40)
    public String kind;

    @Column(nullable = false, length = 200)
    public String recipient;

    @Column(nullable = false, length = 300)
    public String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    public String bodyHtml;

    @Column(nullable = false, length = 20)
    public String status = STATUS_PENDING;

    @Column(nullable = false)
    public Integer attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    public LocalDateTime nextAttemptAt = LocalDateTime.now();

    @Column(name = "last_error", columnDefinition = "TEXT")
    public String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "sent_at")
    public LocalDateTime sentAt;

    /** 投递到期且未死信的消息 */
    public static List<EmailOutbox> listDue(int limit) {
        return find("status in ?1 and nextAttemptAt <= ?2 order by nextAttemptAt asc",
                List.of(STATUS_PENDING, STATUS_FAILED), LocalDateTime.now())
                .page(0, limit).list();
    }

    /** 指数退避: 1m/5m/30m/2h/6h, 第 6 次进死信 */
    public static final long[] BACKOFF_MINUTES = {1, 5, 30, 120, 360};
    public static final int MAX_ATTEMPTS = 6;
}

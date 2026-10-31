package com.jade.cart.trade.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

/** 支付回调幂等表: (provider, event_id) 唯一约束, 插入冲突 = 事件已处理 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_webhook_event")
public class WebhookEvent extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 30)
    public String provider;

    @Column(name = "event_id", nullable = false, length = 120)
    public String eventId;

    @Column(name = "order_public_id", length = 36)
    public String orderPublicId;

    @Column(columnDefinition = "TEXT")
    public String payload;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false)
    public LocalDateTime processedAt;

    /** 幂等入库: true = 新事件, false = 已处理过 (直接返回成功) 先查后插, 唯一约束兜底并发窗口 (冲突时事务回滚, 上层返回错误让网关重试) */
    public static boolean insertIfAbsent(
            String provider, String eventId, String orderPublicId, String payload) {
        if (count("provider = ?1 and eventId = ?2", provider, eventId) > 0) {
            return false;
        }
        WebhookEvent event = new WebhookEvent();
        event.provider = provider;
        event.eventId = eventId;
        event.orderPublicId = orderPublicId;
        event.payload = payload;
        event.persist();
        return true;
    }
}

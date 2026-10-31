package com.jade.cart.contact.service;

import com.jade.cart.contact.entity.ContactMessage;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

/** 客服留言: 提交 / 状态管理 / 到期匿名化 (IP 哈希置空, 不删除) */
@ApplicationScoped
public class ContactService {

    /** 留言保留 30 天后匿名化 (kartwo 对齐: 到期匿名化, 无删除操作) */
    private static final int RETENTION_DAYS = 30;

    @Transactional
    public ContactMessage submit(
            String name, String email, String topic, String message, String ipHash) {
        ContactMessage msg = new ContactMessage();
        msg.name = name;
        msg.email = email.trim();
        msg.topic = topic;
        msg.message = message.trim();
        msg.ipHash = ipHash;
        msg.persist();
        Log.infof("contact submitted: topic=%s email=%s", topic, msg.email);
        return msg;
    }

    /** 同 IP 近一小时留言数 (限频) */
    public long recentCountByIp(String ipHash) {
        return ContactMessage.count(
                "ipHash = ?1 and createdAt > ?2", ipHash, LocalDateTime.now().minusHours(1));
    }

    public java.util.List<ContactMessage> page(String status, int pageIdx, int size) {
        StringBuilder where = new StringBuilder("1=1");
        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        if (status != null && !status.isBlank()) {
            where.append(" and status = :st");
            params.put("st", status);
        }
        return ContactMessage.find(where + " order by createdAt desc", params)
                .page(Math.max(pageIdx - 1, 0), size)
                .list();
    }

    public long count(String status) {
        if (status == null || status.isBlank()) {
            return ContactMessage.count();
        }
        return ContactMessage.count("status", status);
    }

    @Transactional
    public ContactMessage transition(String publicId, String to) {
        ContactMessage msg = ContactMessage.find("publicId", publicId).firstResult();
        if (msg == null) {
            throw new com.jade.common.exception.BizException("留言不存在");
        }
        if (!ContactMessage.STATUS_PROCESSING.equals(to)
                && !ContactMessage.STATUS_CLOSED.equals(to)
                && !ContactMessage.STATUS_NEW.equals(to)) {
            throw new com.jade.common.exception.BizException("非法状态");
        }
        msg.status = to;
        if (ContactMessage.STATUS_CLOSED.equals(to)
                || ContactMessage.STATUS_PROCESSING.equals(to)) {
            msg.processedAt = LocalDateTime.now();
        }
        msg.persist();
        return msg;
    }

    @Transactional
    public long anonymizeExpiredNow() {
        return ContactMessage.update(
                "ipHash = null, anonymizedAt = ?1 "
                        + "WHERE anonymizedAt IS NULL AND createdAt < ?2",
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(RETENTION_DAYS));
    }
}

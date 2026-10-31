package com.jade.cart.contact.rest;

import com.jade.cart.contact.entity.ContactMessage;
import com.jade.common.api.R;
import io.quarkus.logging.Log;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/**
 * 店面客服留言 (公开提交; IP 只存哈希)
 */
@Path("/api/store/contact")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "店面-客服")
public class StoreContactController {

    private static final Set<String> TOPICS = Set.of("general", "order", "shipping", "return", "other");
    private static final int MAX_PER_IP_PER_HOUR = 10;

    @Inject
    com.jade.cart.contact.service.ContactService contactService;

    public record ContactReq(String name, String email, String topic, String message, String website) {}

    @POST
    @Transactional
    @Operation(summary = "提交留言 (限频: 同 IP 每小时 10 条)")
    public R<Void> submit(@Context HttpHeaders headers, ContactReq req) {
        // 蜜罐: 机器人填了隐藏字段直接丢弃
        if (req.website() != null && !req.website().isBlank()) {
            return R.ok();
        }
        if (req.email() == null || !req.email().contains("@")) {
            return R.fail(400, "请输入有效邮箱");
        }
        if (req.message() == null || req.message().isBlank() || req.message().length() > 5000) {
            return R.fail(400, "留言内容 1~5000 字");
        }
        String topic = req.topic() == null || !TOPICS.contains(req.topic()) ? "general" : req.topic();

        String ipHash = ipHash(clientIp(headers));
        if (contactService.recentCountByIp(ipHash) >= MAX_PER_IP_PER_HOUR) {
            Log.warnf("contact rate limited: ipHash=%s", ipHash.substring(0, 8));
            return R.fail(429, "提交过于频繁, 请稍后再试");
        }
        contactService.submit(req.name(), req.email(), topic, req.message(), ipHash);
        return R.ok();
    }

    // ---------- 内部 ----------

    private String clientIp(HttpHeaders headers) {
        String xff = headers.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return "unknown";
    }

    private String ipHash(String ip) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unknown";
        }
    }
}

package com.jade.cart.trade.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.trade.entity.Order;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Stripe 直连 (无 SDK): Checkout Session 创建 + webhook 验签 + 退款
 * markPaid 路径与 Mock 共用 (StoreCheckoutController.webhookStripe)
 */
@ApplicationScoped
public class StripeProvider implements PaymentProvider {

    private static final String API = "https://api.stripe.com/v1";

    @Inject
    PaymentSettings settings;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public boolean isConfigured() {
        return settings.stripeConfigured();
    }

    @Override
    public Initiation createPayment(Order order) {
        String secret = settings.stripeSecret().orElseThrow();
        String base = settings.storeBaseUrl();
        String form = "mode=payment"
                + "&client_reference_id=" + order.publicId
                + "&success_url=" + enc(base + "/orders/" + order.publicId + "?paid=1")
                + "&cancel_url=" + enc(base + "/orders/" + order.publicId)
                + "&line_items[0][quantity]=1"
                + "&line_items[0][price_data][currency]=" + enc(order.currency.toLowerCase())
                + "&line_items[0][price_data][unit_amount]=" + order.totalCents
                + "&line_items[0][price_data][product_data][name]=" + enc("Order " + order.orderNumber);
        JsonNode resp = post(API + "/checkout/sessions", secret, form);
        return new Initiation(resp.path("url").asText(), resp.path("id").asText());
    }

    /** 验签 (Stripe-Signature: t=...,v1=...), 返回解析后的事件 JSON; 失败抛异常拒绝处理 */
    public JsonNode verifyWebhook(String body, String signatureHeader) {
        String secret = settings.stripeWebhookSecret()
                .orElseThrow(() -> new BizException("Stripe webhook secret 未配置"));
        if (signatureHeader == null || !signatureHeader.contains("t=")) {
            throw new BizException("缺少 Stripe-Signature");
        }
        long timestamp = 0;
        String provided = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.split("=", 2);
            if ("t".equals(kv[0])) {
                timestamp = Long.parseLong(kv[1]);
            } else if ("v1".equals(kv[0])) {
                provided = kv[1];
            }
        }
        if (provided == null || Math.abs(System.currentTimeMillis() / 1000 - timestamp) > 300) {
            throw new BizException("Stripe 签名无效或时间窗超限");
        }
        String expected = hmac(secret, timestamp + "." + body);
        if (!constantTimeEquals(expected, provided)) {
            throw new BizException("Stripe 签名校验失败");
        }
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new BizException("Stripe 事件 JSON 解析失败");
        }
    }

    @Override
    public RefundResult refund(Order order) {
        if (order.paymentRef == null || order.paymentRef.isBlank()) {
            throw new BizException("缺少 Stripe payment_intent, 无法退款");
        }
        JsonNode resp = post(API + "/refunds", settings.stripeSecret().orElseThrow(),
                "payment_intent=" + enc(order.paymentRef));
        Log.infof("stripe refund: order=%s refund=%s", order.orderNumber, resp.path("id").asText());
        return new RefundResult(resp.path("id").asText(), resp.toString());
    }

    // ---------- 内部 ----------

    private JsonNode post(String url, String secret, String form) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + secret)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(resp.body());
            if (resp.statusCode() >= 300) {
                String msg = json.path("error").path("message").asText(resp.body());
                throw new BizException("Stripe API 错误: " + msg);
            }
            return json;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Stripe 请求失败: " + e.getMessage());
        }
    }

    private String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

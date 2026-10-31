package com.jade.cart.trade.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.trade.entity.Order;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * PayPal 直连 (无 SDK): Orders API v2 托管收银台
 * 确认双路: 顾客回跳页面 capture (主) + webhook CAPTURE.COMPLETED 验签后 markPaid (备)
 */
@ApplicationScoped
public class PaypalProvider implements PaymentProvider {

    @Inject
    PaymentSettings settings;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "paypal";
    }

    @Override
    public boolean isConfigured() {
        return settings.paypalConfigured();
    }

    @Override
    public Initiation createPayment(Order order) {
        String base = settings.storeBaseUrl();
        String body = """
                {
                  "intent": "CAPTURE",
                  "purchase_units": [{
                    "reference_id": "%s",
                    "custom_id": "%s",
                    "amount": {"currency_code": "%s", "value": "%s"}
                  }],
                  "application_context": {
                    "brand_name": "Jade Cart",
                    "user_action": "PAY_NOW",
                    "return_url": "%s",
                    "cancel_url": "%s"
                  }
                }
                """.formatted(order.publicId, order.publicId, order.currency,
                decimal(order.totalCents),
                base + "/pay/paypal/" + order.publicId,
                base + "/orders/" + order.publicId);

        JsonNode resp = post("/v2/checkout/orders", body);
        String approveUrl = null;
        String orderId = resp.path("id").asText();
        for (JsonNode link : resp.path("links")) {
            if ("approve".equals(link.path("rel").asText())) {
                approveUrl = link.path("href").asText();
            }
        }
        if (approveUrl == null) {
            throw new BizException("PayPal 未返回 approve 链接");
        }
        return new Initiation(approveUrl, orderId);
    }

    /** 顾客回跳后 capture; COMPLETED 时返回 capture 信息供 markPaid */
    public JsonNode capture(String paypalOrderId) {
        return post("/v2/checkout/orders/" + paypalOrderId + "/capture", "{}");
    }

    /** webhook 验签: 调 verify-webhook-signature API, true = 可信 */
    public boolean verifyWebhook(String body, String transmissionId, String transmissionTime,
                                 String certUrl, String authAlgo, String transmissionSig,
                                 String authCertUrl, String webhookId) {
        String payload = """
                {
                  "auth_algo": "%s", "cert_url": "%s", "transmission_id": "%s",
                  "transmission_sig": "%s", "transmission_time": "%s",
                  "webhook_id": "%s", "webhook_event": %s
                }
                """.formatted(authAlgo, certUrl, transmissionId, transmissionSig,
                transmissionTime, webhookId, body);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.paypalApiBase()
                            + "/v1/notifications/verify-webhook-signature"))
                    .header("Authorization", "Bearer " + token())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            return mapper.readTree(resp.body()).path("verification_status").asText()
                    .equals("SUCCESS");
        } catch (Exception e) {
            Log.errorf(e, "paypal webhook verify failed");
            return false;
        }
    }

    @Override
    public RefundResult refund(Order order) {
        if (order.paymentRef == null || order.paymentRef.isBlank()) {
            throw new BizException("缺少 PayPal capture id, 无法退款");
        }
        JsonNode resp = post("/v2/payments/captures/" + order.paymentRef + "/refund", "{}");
        Log.infof("paypal refund: order=%s refund=%s", order.orderNumber, resp.path("id").asText());
        return new RefundResult(resp.path("id").asText(), resp.toString());
    }

    // ---------- 内部 ----------

    private volatile String cachedToken;
    private volatile long tokenExpiresAt;

    private String token() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedToken;
        }
        String id = settings.get(PaymentSettings.K_PAYPAL_ID).orElseThrow();
        String secret = settings.get(PaymentSettings.K_PAYPAL_SECRET).orElseThrow();
        String basic = Base64.getEncoder().encodeToString(
                (id + ":" + secret).getBytes(StandardCharsets.UTF_8));
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.paypalApiBase()
                            + "/v1/oauth2/token"))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(resp.body());
            cachedToken = json.path("access_token").asText(null);
            if (cachedToken == null) {
                throw new BizException("PayPal OAuth 失败: " + resp.body());
            }
            tokenExpiresAt = System.currentTimeMillis()
                    + (json.path("expires_in").asLong(3600) - 60) * 1000;
            return cachedToken;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("PayPal OAuth 请求失败: " + e.getMessage());
        }
    }

    private JsonNode post(String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.paypalApiBase() + path))
                    .header("Authorization", "Bearer " + token())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(resp.body());
            if (resp.statusCode() >= 300) {
                String msg = json.path("message").asText(resp.body());
                throw new BizException("PayPal API 错误: " + msg);
            }
            return json;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("PayPal 请求失败: " + e.getMessage());
        }
    }

    private String decimal(long cents) {
        return "%d.%02d".formatted(cents / 100, cents % 100);
    }
}

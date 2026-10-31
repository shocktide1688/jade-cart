package com.jade.cart.trade.rest;

import com.jade.cart.trade.dto.OrderView;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.payment.PaymentProvider;
import com.jade.cart.trade.service.CartService;
import com.jade.cart.trade.service.OrderService;
import com.jade.common.api.R;
import io.quarkus.logging.Log;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.util.Locale;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 店面结算 + 订单查询 + Mock 收银台 (公开)
 *
 * <p>Mock 支付流程: checkout 返回 payUrl=/pay/mock/{publicId} → 店面确认 → /mock-pay/{publicId}/confirm
 * 模拟网关回调 → markPaid 幂等路径
 */
@Path("/api/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "店面-结算")
public class StoreCheckoutController {

    @Inject CartService cartService;

    @Inject OrderService orderService;

    public record CheckoutReq(String email, String shipName, String country) {}

    public record CheckoutResult(
            String orderPublicId,
            String orderNumber,
            Long totalCents,
            String currency,
            String payUrl) {}

    public record WebhookReq(
            String eventId,
            String orderPublicId,
            Long amountCents,
            String currency,
            String paymentRef) {}

    @POST
    @Path("/checkout")
    @Transactional
    @Operation(summary = "结算: 预留库存→建单→返回收银台跳转地址")
    public Response checkout(@Context HttpHeaders headers, CheckoutReq req) {
        String token = CartService.readToken(headers).orElse(null);
        if (token == null) {
            return Response.ok(R.fail(400, "购物车为空")).build();
        }
        Order order = orderService.checkout(token, req.email(), req.shipName(), req.country());
        PaymentProvider provider = orderService.providerOf(order);
        PaymentProvider.Initiation initiation = provider.createPayment(order);

        return Response.ok(
                        R.ok(
                                new CheckoutResult(
                                        order.publicId,
                                        order.orderNumber,
                                        order.totalCents,
                                        order.currency,
                                        initiation.redirectUrl())))
                .cookie(
                        new NewCookie.Builder(CartService.CART_COOKIE)
                                .value(token)
                                .path("/")
                                .maxAge(60 * 60 * 24 * 30)
                                .httpOnly(true)
                                .build())
                .build();
    }

    @GET
    @Path("/orders/{publicId}")
    @Operation(summary = "订单状态查询 (店面, 按 publicId)")
    public R<OrderView> order(@PathParam("publicId") String publicId) {
        return Order.byPublicId(publicId)
                .map(orderService::toView)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "订单不存在"));
    }

    /** Mock 网关确认 (模拟顾客在收银台点"支付成功", 网关向商户回调) */
    @POST
    @Path("/mock-pay/{publicId}/confirm")
    @Transactional
    @Operation(summary = "Mock: 确认支付 (走 webhook 幂等路径)")
    public R<Boolean> confirmMockPay(@PathParam("publicId") String publicId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return R.fail(404, "订单不存在");
        }
        // 模拟网关行为: 用网关侧已知金额/币种生成事件回调商户
        String eventId =
                "evt_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        boolean consumed =
                orderService.markPaid(
                        "mock",
                        eventId,
                        publicId,
                        order.totalCents,
                        order.currency,
                        "mock_" + eventId);
        return R.ok(consumed);
    }

    // ---------- 真实网关 webhook (P1) ----------

    @Inject com.jade.cart.trade.payment.StripeProvider stripeProvider;

    @Inject com.jade.cart.trade.payment.PaypalProvider paypalProvider;

    @Inject com.jade.cart.trade.payment.PaymentSettings paymentSettings;

    /** Stripe webhook: 验签 (Stripe-Signature) → checkout.session.completed → markPaid */
    @POST
    @Path("/webhooks/stripe")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "Stripe webhook 回调 (验签+幂等)")
    public R<Boolean> webhookStripe(
            String rawBody, @jakarta.ws.rs.HeaderParam("Stripe-Signature") String signature) {
        try {
            com.fasterxml.jackson.databind.JsonNode event =
                    stripeProvider.verifyWebhook(rawBody, signature);
            String type = event.path("type").asText();
            if (!"checkout.session.completed".equals(type)) {
                return R.ok(true); // 非支付完成事件不处理
            }
            com.fasterxml.jackson.databind.JsonNode session = event.path("data").path("object");
            String orderPublicId = session.path("client_reference_id").asText(null);
            long amount = session.path("amount_total").asLong(0);
            String currency = session.path("currency").asText("usd").toUpperCase(Locale.ROOT);
            String paymentIntent = session.path("payment_intent").asText("");
            if (orderPublicId == null || orderPublicId.isBlank()) {
                Log.warn("stripe webhook: session without client_reference_id");
                return R.ok(true);
            }
            return R.ok(
                    orderService.markPaid(
                            "stripe",
                            event.path("id").asText(),
                            orderPublicId,
                            amount,
                            currency,
                            paymentIntent));
        } catch (com.jade.common.exception.BizException e) {
            Log.warnf("stripe webhook rejected: %s", e.getMessage());
            return R.fail(400, e.getMessage());
        }
    }

    public record PaypalHeaders(
            @jakarta.ws.rs.HeaderParam("Paypal-Transmission-Id") String transmissionId,
            @jakarta.ws.rs.HeaderParam("Paypal-Transmission-Time") String transmissionTime,
            @jakarta.ws.rs.HeaderParam("Paypal-Cert-Url") String certUrl,
            @jakarta.ws.rs.HeaderParam("Paypal-Auth-Algo") String authAlgo,
            @jakarta.ws.rs.HeaderParam("Paypal-Transmission-Sig") String transmissionSig) {}

    /** PayPal webhook: verify-webhook-signature API 验签 → CAPTURE.COMPLETED → markPaid */
    @POST
    @Path("/webhooks/paypal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "PayPal webhook 回调 (验签+幂等)")
    public R<Boolean> webhookPaypal(String rawBody, @jakarta.ws.rs.BeanParam PaypalHeaders h) {
        String webhookId =
                paymentSettings
                        .get(com.jade.cart.trade.payment.PaymentSettings.K_PAYPAL_WEBHOOK_ID)
                        .orElse(null);
        if (webhookId == null) {
            return R.fail(400, "PayPal webhook id 未配置");
        }
        if (!paypalProvider.verifyWebhook(
                rawBody,
                h.transmissionId(),
                h.transmissionTime(),
                h.certUrl(),
                h.authAlgo(),
                h.transmissionSig(),
                h.certUrl(),
                webhookId)) {
            return R.fail(400, "PayPal 签名校验失败");
        }
        try {
            com.fasterxml.jackson.databind.JsonNode event =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawBody);
            if (!"PAYMENT.CAPTURE.COMPLETED".equals(event.path("event_type").asText())) {
                return R.ok(true);
            }
            com.fasterxml.jackson.databind.JsonNode resource = event.path("resource");
            String customId = resource.path("custom_id").asText(null);
            String captureId = resource.path("id").asText();
            long amountCents = Math.round(resource.path("amount").path("value").asDouble(0) * 100);
            String currency = resource.path("amount").path("currency_code").asText("USD");
            if (customId == null || customId.isBlank()) {
                Log.warn("paypal webhook: capture without custom_id");
                return R.ok(true);
            }
            return R.ok(
                    orderService.markPaid(
                            "paypal",
                            event.path("id").asText(),
                            customId,
                            amountCents,
                            currency,
                            captureId));
        } catch (com.jade.common.exception.BizException e) {
            return R.fail(400, e.getMessage());
        } catch (Exception e) {
            return R.fail(400, "PayPal 事件解析失败");
        }
    }

    /** Mock webhook 端点 (与真实网关回调同形状: 公开 + 幂等, 供脚本/curl 演练) */
    @POST
    @Path("/webhooks/mock")
    @Transactional
    @Operation(summary = "Mock webhook 回调端点 (幂等)")
    public R<Boolean> webhook(WebhookReq req) {
        if (req.eventId() == null || req.orderPublicId() == null || req.amountCents() == null) {
            return R.fail(400, "eventId/orderPublicId/amountCents 必填");
        }
        return R.ok(
                orderService.markPaid(
                        "mock",
                        req.eventId(),
                        req.orderPublicId(),
                        req.amountCents(),
                        req.currency() == null ? "USD" : req.currency(),
                        req.paymentRef() == null ? "mock_" + req.eventId() : req.paymentRef()));
    }
}

package com.jade.cart.trade.payment;

import com.jade.cart.trade.entity.Order;

/**
 * 支付网关抽象 (kartwo PaymentProvider 同形状): createPayment 发起 → 网关回调 webhook → PaymentService.markPaid;
 * refund 网关退款
 *
 * <p>P0 内置 MockProvider; P1 增加真实 Stripe / PayPal (REST 直连, 无 SDK)
 */
public interface PaymentProvider {

    /** provider 标识, 存 order.payment_provider */
    String name();

    /** 是否已配置可用 (密钥齐全); 未配置时结账不选择该渠道 */
    boolean isConfigured();

    /** 发起支付: 返回收银台跳转地址 + 网关侧流水号 */
    Initiation createPayment(Order order);

    /** 网关退款; 抛异常 = 退款失败 (订单状态不变) */
    RefundResult refund(Order order);

    record Initiation(String redirectUrl, String providerRef) {}

    record RefundResult(String providerRefundId, String raw) {}
}

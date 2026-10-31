package com.jade.cart.trade.payment;

import com.jade.cart.trade.entity.Order;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mock 收银台: 跳转到本站 /pay/mock/{publicId} 页面, 页面确认后走 webhook 幂等路径 markPaid (与真实网关回调同一条代码路径)
 * 未配置任何真实网关时的兜底渠道
 */
@ApplicationScoped
public class MockPaymentProvider implements PaymentProvider {

    public static final String NAME = "mock";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public Initiation createPayment(Order order) {
        return new Initiation("/pay/mock/" + order.publicId, "mock_" + order.publicId);
    }

    @Override
    public RefundResult refund(Order order) {
        return new RefundResult("mock_refund_" + order.publicId, "{}");
    }
}

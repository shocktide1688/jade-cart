package com.jade.cart.app.job;

import com.jade.cart.trade.service.OrderService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 支付对账 (每 5 分钟): webhook 丢失兜底 — 主动查询 Stripe/PayPal 网关, 已支付订单走 markPaid 幂等路径补账; 已取消订单的迟到款自动转
 * PAYMENT_REVIEW 人工处理
 */
@ApplicationScoped
public class PaymentReconcileJob {

    @Inject OrderService orderService;

    @Scheduled(every = "300s", delay = 60, delayUnit = java.util.concurrent.TimeUnit.SECONDS)
    void reconcile() {
        int recovered = orderService.reconcilePayments();
        if (recovered > 0) {
            Log.infof("payment reconcile recovered %d orders", recovered);
        }
    }
}

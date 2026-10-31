package com.jade.cart.app.job;

import com.jade.cart.trade.service.OrderService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** 订单预留过期清扫 (kartwo 同款: 每分钟扫, 过期 PENDING → CANCELLED + 释放 reserved) */
@ApplicationScoped
public class ReservationSweeper {

    @Inject OrderService orderService;

    @Scheduled(every = "60s", delay = 15, delayUnit = java.util.concurrent.TimeUnit.SECONDS)
    void sweep() {
        int cancelled = orderService.cancelExpiredReservations();
        if (cancelled > 0) {
            Log.infof("reservation sweeper cancelled %d expired orders", cancelled);
        }
    }
}

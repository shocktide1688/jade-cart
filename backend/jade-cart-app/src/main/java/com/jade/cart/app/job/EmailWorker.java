package com.jade.cart.app.job;

import com.jade.cart.mail.service.OutboxService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** 邮件 outbox worker: 20s 轮询投递到期消息 (指数退避在 OutboxService 内) */
@ApplicationScoped
public class EmailWorker {

    @Inject OutboxService outboxService;

    @Scheduled(every = "20s", delay = 30, delayUnit = java.util.concurrent.TimeUnit.SECONDS)
    void flush() {
        int n = outboxService.flushDue(20);
        if (n > 0) {
            Log.infof("email worker flushed %d due messages", n);
        }
    }
}

package com.jade.cart.app.job;

import com.jade.cart.contact.service.ContactService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** 客服留言保留期任务: 每日 03:00 匿名化过期留言的 IP (不删除) */
@ApplicationScoped
public class ContactRetentionJob {

    @Inject ContactService contactService;

    @Scheduled(cron = "0 0 3 * * ?")
    void anonymizeExpired() {
        long n = contactService.anonymizeExpiredNow();
        if (n > 0) {
            Log.infof("contact anonymized %d expired messages", n);
        }
    }
}

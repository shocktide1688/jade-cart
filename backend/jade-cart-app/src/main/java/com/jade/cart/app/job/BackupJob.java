package com.jade.cart.app.job;

import com.jade.cart.backup.service.BackupService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 每日 04:00 全量备份 + 保留轮换 ( kartwo backup scheduler 对齐)
 */
@ApplicationScoped
public class BackupJob {

    @Inject
    BackupService backupService;

    @Scheduled(cron = "0 0 4 * * ?")
    void dailyBackup() {
        try {
            backupService.createBackup();
        } catch (Exception e) {
            Log.errorf(e, "daily backup failed");
        }
    }
}

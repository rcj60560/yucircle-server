package com.yucircle.task;

import com.yucircle.service.BwfSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * BWF 定时同步（bwf.sync.enabled=false 可整体关掉）。
 * 周一 10:00 排名+赛程；每日 22:00 当日赛果（级别达标的自动带逐分）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BwfSyncTasks {

    private final BwfSyncService syncService;

    @Value("${bwf.sync.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "0 0 10 ? * MON")
    public void weeklySync() {
        if (!enabled) {
            return;
        }
        try {
            log.info("[bwf] 周更开始: rankings={} 条, schedule={} 站",
                    syncService.syncRankings(), syncService.syncSchedule());
        } catch (Exception e) {
            log.error("[bwf] 周更失败", e);
        }
    }

    @Scheduled(cron = "0 0 22 * * *")
    public void dailyMatchSync() {
        if (!enabled) {
            return;
        }
        try {
            log.info("[bwf] 日更开始: {} 场", syncService.syncDayMatches(LocalDate.now()));
        } catch (Exception e) {
            log.error("[bwf] 日更失败", e);
        }
    }
}

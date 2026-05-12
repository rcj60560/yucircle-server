package com.yucircle.task;

import com.yucircle.service.ActivityService;
import com.yucircle.service.BroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private BroadcastService broadcastService;

    // Clean up presence records older than 30 seconds every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void cleanupExpiredPresence() {
        try {
            activityService.cleanupExpiredPresence();
        } catch (Exception e) {
            System.err.println("Error cleaning up expired presence: " + e.getMessage());
        }
    }

    // Clean up expired reserved slots every 30 seconds
    @Scheduled(fixedRate = 30000)
    public void cleanupExpiredSlots() {
        try {
            activityService.cleanupExpiredSlots();
        } catch (Exception e) {
            System.err.println("Error cleaning up expired slots: " + e.getMessage());
        }
    }

    // Clean up old messages every hour
    @Scheduled(fixedRate = 3600000)
    public void cleanupOldMessages() {
        try {
            activityService.cleanupOldMessages();
        } catch (Exception e) {
            System.err.println("Error cleaning up old messages: " + e.getMessage());
        }
    }

    // Clean up expired broadcasts every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredBroadcasts() {
        try {
            broadcastService.cleanupExpiredBroadcasts();
        } catch (Exception e) {
            System.err.println("Error cleaning up expired broadcasts: " + e.getMessage());
        }
    }
}

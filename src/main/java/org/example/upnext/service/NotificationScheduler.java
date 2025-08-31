package org.example.upnext.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final NotificationGeneratorService notificationGenerator = new NotificationGeneratorService();

    public static void startScheduler() {
        // Run every day at a specific time (e.g., 8 AM)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                notificationGenerator.checkAndGenerateDueSoonNotifications();
                notificationGenerator.checkAndGenerateDeadlinePassedNotifications();
                System.out.println("Notification generation completed at: " + java.time.LocalDateTime.now());
            } catch (Exception e) {
                System.err.println("Error in notification generation: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.DAYS);
    }

    public static void shutdown() {
        scheduler.shutdown();
    }
}
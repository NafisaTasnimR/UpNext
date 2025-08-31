package org.example.upnext.service;

import org.example.upnext.dao.TaskDAO;
import org.example.upnext.dao.NotificationDAO;
import org.example.upnext.dao.impl.TaskDAOImpl;
import org.example.upnext.dao.impl.NotificationDAOImpl;
import org.example.upnext.model.Task;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class NotificationGeneratorService {
    private final TaskDAO taskDAO;
    private final NotificationDAO notificationDAO;

    public NotificationGeneratorService() {
        this.taskDAO = new TaskDAOImpl();
        this.notificationDAO = new NotificationDAOImpl();
    }

    public void checkAndGenerateDueSoonNotifications() {
        try {
            LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
            List<Task> dueSoonTasks = taskDAO.findTasksDueOn(threeDaysFromNow);

            for (Task task : dueSoonTasks) {
                // ONLY notify the assignee - REMOVED the creator notification part
                if (task.getAssigneeId() != null) {
                    createDueSoonNotification(task.getTaskId(), task.getAssigneeId());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating due soon notifications: " + e.getMessage());
        }
    }

    public void checkAndGenerateDeadlinePassedNotifications() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<Task> deadlinePassedTasks = taskDAO.findTasksDueOn(yesterday);

            for (Task task : deadlinePassedTasks) {
                if ("TODO".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus())) {
                    // ONLY notify the assignee - REMOVED the creator notification part
                    if (task.getAssigneeId() != null) {
                        createDeadlinePassedNotification(task.getTaskId(), task.getAssigneeId());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating deadline passed notifications: " + e.getMessage());
        }
    }

    private void createDueSoonNotification(long taskId, long userId) {
        try {
            if (!notificationDAO.notificationExists(taskId, userId, "DUE_SOON_3D")) {
                notificationDAO.createDueSoonNotification(taskId, userId);
            }
        } catch (SQLException e) {
            System.err.println("Error creating due soon notification: " + e.getMessage());
        }
    }

    private void createDeadlinePassedNotification(long taskId, long userId) {
        try {
            if (!notificationDAO.notificationExists(taskId, userId, "DEADLINE_PASSED")) {
                notificationDAO.createDeadlinePassedNotification(taskId, userId);
            }
        } catch (SQLException e) {
            System.err.println("Error creating deadline passed notification: " + e.getMessage());
        }
    }
}
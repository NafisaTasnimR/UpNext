package org.example.upnext.dao;

import org.example.upnext.model.Notification;
import org.example.upnext.model.Task;
import java.sql.SQLException;
import java.util.List;

public interface NotificationDAO {
    // feeds
    List<Notification> findForProjectAsAdmin(long projectId);
    List<Notification> findUserInbox(long projectId, long userId);
    List<Notification> findAssignedByMe(long projectId, long creatorUserId);

    // overdue
    List<Task> findOverdueForAdmin(long projectId);
    List<Task> findOverdueForUserInbox(long projectId, long userId);
    List<Task> findOverdueForAssignedByMe(long projectId, long userId);

    // notification generation
    void createDueSoonNotification(long taskId, long userId) throws SQLException;
    void createDeadlinePassedNotification(long taskId, long userId) throws SQLException;
    boolean notificationExists(long taskId, long userId, String type) throws SQLException;
}
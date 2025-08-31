package org.example.upnext.service;

import org.example.upnext.dao.NotificationDAO;
import org.example.upnext.model.Notification;
import org.example.upnext.model.Task;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    public enum Role { ADMIN, MANAGER, MEMBER }

    private final NotificationDAO dao;

    public NotificationService(NotificationDAO dao) {
        this.dao = dao;
    }

    // ===== feeds =====
    public List<Notification> getInbox(long projectId, long userId, Role role) {
        return switch (role) {
            case ADMIN   -> dao.findForProjectAsAdmin(projectId);
            case MANAGER, MEMBER -> dao.findUserInbox(projectId, userId);
        };
    }

    public List<Notification> getAssignedByMe(long projectId, long userId, Role role) {
        return switch (role) {
            case ADMIN, MANAGER -> dao.findAssignedByMe(projectId, userId);
            default -> List.of();
        };
    }

    // ===== overdue =====
    public List<Task> getOverdue(long projectId, long userId, Role role) {
        return switch (role) {
            case ADMIN   -> dao.findOverdueForAdmin(projectId);
            case MEMBER  -> dao.findOverdueForUserInbox(projectId, userId);
            case MANAGER -> {
                var inbox  = dao.findOverdueForUserInbox(projectId, userId);
                var byMe   = dao.findOverdueForAssignedByMe(projectId, userId);
                // simple dedupe (by taskId)
                List<Task> out = new ArrayList<>(inbox);
                for (Task t : byMe) {
                    boolean exists = out.stream().anyMatch(x -> x.getTaskId().equals(t.getTaskId()));
                    if (!exists) out.add(t);
                }
                yield out;
            }
        };
    }
}
package org.example.upnext.dao.impl;

import org.example.upnext.config.Db;
import org.example.upnext.dao.NotificationDAO;
import org.example.upnext.model.Notification;
import org.example.upnext.model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAOImpl implements NotificationDAO {

    // ===== helpers =====
    private static String ns(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return rs.wasNull() ? null : s;
    }

    @Override
    public void createDueSoonNotification(long taskId, long userId) throws SQLException {
        String sql = """
            INSERT INTO NOTIFICATIONS (NOTIF_ID, USER_ID, TASK_ID, MESSAGE, TYPE, CREATED_AT)
            SELECT NOTIFICATIONS_SEQ.NEXTVAL, ?, ?, 'Task due in 3 days: ' || TITLE, 'DUE_SOON_3D', SYSDATE
            FROM TASKS WHERE TASK_ID = ?
            """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, taskId);
            ps.setLong(3, taskId);
            ps.executeUpdate();
        }
    }

    @Override
    public void createDeadlinePassedNotification(long taskId, long userId) throws SQLException {
        String sql = """
            INSERT INTO NOTIFICATIONS (NOTIF_ID, USER_ID, TASK_ID, MESSAGE, TYPE, CREATED_AT)
            SELECT NOTIFICATIONS_SEQ.NEXTVAL, ?, ?, 'Task deadline passed: ' || TITLE, 'DEADLINE_PASSED', SYSDATE
            FROM TASKS WHERE TASK_ID = ?
            """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, taskId);
            ps.setLong(3, taskId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean notificationExists(long taskId, long userId, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE TASK_ID = ? AND USER_ID = ? AND TYPE = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("NOTIF_ID"));
        n.setUserId(rs.getLong("USER_ID"));
        n.setTaskId(rs.getObject("TASK_ID") == null ? null : rs.getLong("TASK_ID"));
        n.setMessage(rs.getString("MESSAGE"));
        n.setType(ns(rs, "TYPE"));
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        n.setCreatedAt(ts == null ? null : ts.toLocalDateTime());

        // joined
        n.setProjectId(rs.getLong("PROJECT_ID"));
        n.setTaskTitle(ns(rs, "TASK_TITLE"));
        Date dd = rs.getDate("DUE_DATE");
        n.setDueDate(dd == null ? null : dd.toLocalDate());
        n.setTaskStatus(ns(rs, "STATUS"));
        return n;
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setTaskId(rs.getLong("TASK_ID"));
        t.setProjectId(rs.getLong("PROJECT_ID"));
        t.setTitle(rs.getString("TITLE"));
        t.setStatus(rs.getString("STATUS"));
        Date dd = rs.getDate("DUE_DATE");
        t.setDueDate(dd == null ? null : dd.toLocalDate());
        t.setAssigneeId(rs.getObject("ASSIGNEE_ID") == null ? null : rs.getLong("ASSIGNEE_ID"));
        return t;
    }

    private static final String BASE_SELECT = """
        SELECT n.NOTIF_ID, n.USER_ID, n.TASK_ID, n.MESSAGE, n.CREATED_AT, n.TYPE,
               t.PROJECT_ID, t.TITLE AS TASK_TITLE, t.DUE_DATE, t.STATUS
          FROM NOTIFICATIONS n
          JOIN TASKS t ON t.TASK_ID = n.TASK_ID
        """;

    @Override
    public List<Notification> findForProjectAsAdmin(long projectId) {
        String sql = BASE_SELECT + " WHERE t.PROJECT_ID = ? ORDER BY n.CREATED_AT DESC";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> out = new ArrayList<>();
                while (rs.next()) out.add(mapNotification(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findForProjectAsAdmin failed", e);
        }
    }

    @Override
    public List<Notification> findUserInbox(long projectId, long userId) {
        String sql = BASE_SELECT +
                " WHERE t.PROJECT_ID = ? AND n.USER_ID = ? ORDER BY n.CREATED_AT DESC";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> out = new ArrayList<>();
                while (rs.next()) out.add(mapNotification(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findUserInbox failed", e);
        }
    }

    @Override
    public List<Notification> findAssignedByMe(long projectId, long creatorUserId) {
        // NOTE: requires TASKS.CREATED_BY to be populated
        String sql = BASE_SELECT +
                " WHERE t.PROJECT_ID = ? AND t.CREATED_BY = ? ORDER BY n.CREATED_AT DESC";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, creatorUserId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> out = new ArrayList<>();
                while (rs.next()) out.add(mapNotification(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAssignedByMe failed", e);
        }
    }

    @Override
    public List<Task> findOverdueForAdmin(long projectId) {
        String sql = """
            SELECT t.TASK_ID, t.PROJECT_ID, t.TITLE, t.STATUS, t.ASSIGNEE_ID, t.DUE_DATE
              FROM TASKS t
             WHERE t.PROJECT_ID = ?
               AND t.DUE_DATE IS NOT NULL
               AND t.STATUS NOT IN ('DONE','CANCELLED')
               AND TRUNC(t.DUE_DATE) < TRUNC(SYSDATE)
             ORDER BY t.DUE_DATE ASC
        """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> out = new ArrayList<>();
                while (rs.next()) out.add(mapTask(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findOverdueForAdmin failed", e);
        }
    }

    @Override
    public List<Task> findOverdueForUserInbox(long projectId, long userId) {
        String sql = """
            SELECT t.TASK_ID, t.PROJECT_ID, t.TITLE, t.STATUS, t.ASSIGNEE_ID, t.DUE_DATE
              FROM TASKS t
             WHERE t.PROJECT_ID = ?
               AND t.ASSIGNEE_ID = ?
               AND t.DUE_DATE IS NOT NULL
               AND t.STATUS NOT IN ('DONE','CANCELLED')
               AND TRUNC(t.DUE_DATE) < TRUNC(SYSDATE)
             ORDER BY t.DUE_DATE ASC
        """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> out = new ArrayList<>();
                while (rs.next()) out.add(mapTask(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findOverdueForUserInbox failed", e);
        }
    }

    @Override
    public List<Task> findOverdueForAssignedByMe(long projectId, long userId) {
        String sql = """
            SELECT t.TASK_ID, t.PROJECT_ID, t.TITLE, t.STATUS, t.ASSIGNEE_ID, t.DUE_DATE
              FROM TASKS t
             WHERE t.PROJECT_ID = ?
               AND t.CREATED_BY = ?
               AND t.DUE_DATE IS NOT NULL
               AND t.STATUS NOT IN ('DONE','CANCELLED')
               AND TRUNC(t.DUE_DATE) < TRUNC(SYSDATE)
             ORDER BY t.DUE_DATE ASC
        """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> out = new ArrayList<>();
                while (rs.next()) out.add(mapTask(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findOverdueForAssignedByMe failed", e);
        }
    }
}
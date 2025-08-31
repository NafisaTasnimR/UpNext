package org.example.upnext.dao.impl;

import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.TaskDAO;
import org.example.upnext.model.Task;

import java.sql.*;
import java.sql.Date;
import java.util.*;


public class TaskDAOImpl extends BaseDAO implements TaskDAO {

    private Task map(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setTaskId(rs.getLong("TASK_ID"));
        t.setProjectId(rs.getLong("PROJECT_ID"));
        long p = rs.getLong("PARENT_TASK_ID"); if (!rs.wasNull()) t.setParentTaskId(p);
        t.setTitle(rs.getString("TITLE"));
        t.setDescription(rs.getString("DESCRIPTION"));
        long a = rs.getLong("ASSIGNEE_ID"); if (!rs.wasNull()) t.setAssigneeId(a);
        t.setStatus(rs.getString("STATUS"));
        t.setPriority(rs.getString("PRIORITY"));
        Date sd = rs.getDate("START_DATE"); if (sd != null) t.setStartDate(sd.toLocalDate());
        Date dd = rs.getDate("DUE_DATE");   if (dd != null) t.setDueDate(dd.toLocalDate());
        t.setProgressPct(rs.getDouble("PROGRESS_PCT"));
        double eh = rs.getDouble("ESTIMATED_HOURS"); if (!rs.wasNull()) t.setEstimatedHours(eh);
        double ah = rs.getDouble("ACTUAL_HOURS");    if (!rs.wasNull()) t.setActualHours(ah);
        t.setBlocked("Y".equals(rs.getString("IS_BLOCKED")));
        t.setAssigneeName(rs.getString("assignee_name"));

        return t;
    }

    @Override
    public long create(Task t) throws SQLException {
        String sql = """
            INSERT INTO TASKS (TASK_ID, PROJECT_ID, PARENT_TASK_ID, TITLE, DESCRIPTION, ASSIGNEE_ID,
                               STATUS, PRIORITY, START_DATE, DUE_DATE, PROGRESS_PCT, ESTIMATED_HOURS, ACTUAL_HOURS)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            long id = nextVal(c, "TASKS_SEQ");
            ps.setLong(1, id);
            ps.setLong(2, t.getProjectId());
            if (t.getParentTaskId() != null) ps.setLong(3, t.getParentTaskId()); else ps.setNull(3, Types.NUMERIC);
            ps.setString(4, t.getTitle());
            ps.setString(5, t.getDescription());
            if (t.getAssigneeId() != null) ps.setLong(6, t.getAssigneeId()); else ps.setNull(6, Types.NUMERIC);
            ps.setString(7, t.getStatus() == null ? "TODO" : t.getStatus());
            ps.setString(8, t.getPriority() == null ? "MEDIUM" : t.getPriority());
            if (t.getStartDate() != null) ps.setDate(9, Date.valueOf(t.getStartDate())); else ps.setNull(9, Types.DATE);
            if (t.getDueDate() != null)   ps.setDate(10, Date.valueOf(t.getDueDate())); else ps.setNull(10, Types.DATE);
            ps.setDouble(11, t.getProgressPct());
            if (t.getEstimatedHours() != null) ps.setDouble(12, t.getEstimatedHours()); else ps.setNull(12, Types.NUMERIC);
            if (t.getActualHours() != null)    ps.setDouble(13, t.getActualHours());    else ps.setNull(13, Types.NUMERIC);
            ps.executeUpdate();
            return id;
        }
    }

    @Override
    public void update(Task t) throws SQLException {
        String sql = """
            UPDATE TASKS
               SET PROJECT_ID=?, PARENT_TASK_ID=?, TITLE=?, DESCRIPTION=?, ASSIGNEE_ID=?,
                   STATUS=?, PRIORITY=?, START_DATE=?, DUE_DATE=?, PROGRESS_PCT=?, ESTIMATED_HOURS=?, ACTUAL_HOURS=?
             WHERE TASK_ID=?
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, t.getProjectId());
            if (t.getParentTaskId() != null) ps.setLong(2, t.getParentTaskId()); else ps.setNull(2, Types.NUMERIC);
            ps.setString(3, t.getTitle());
            ps.setString(4, t.getDescription());
            if (t.getAssigneeId() != null) ps.setLong(5, t.getAssigneeId()); else ps.setNull(5, Types.NUMERIC);
            ps.setString(6, t.getStatus());
            ps.setString(7, t.getPriority());
            if (t.getStartDate() != null) ps.setDate(8, Date.valueOf(t.getStartDate())); else ps.setNull(8, Types.DATE);
            if (t.getDueDate() != null)   ps.setDate(9, Date.valueOf(t.getDueDate()));   else ps.setNull(9, Types.DATE);
            ps.setDouble(10, t.getProgressPct());
            if (t.getEstimatedHours() != null) ps.setDouble(11, t.getEstimatedHours()); else ps.setNull(11, Types.NUMERIC);
            if (t.getActualHours() != null)    ps.setDouble(12, t.getActualHours());    else ps.setNull(12, Types.NUMERIC);
            ps.setLong(13, t.getTaskId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(long taskId) throws SQLException {
        final String subtree = """
        SELECT TASK_ID FROM TASKS
         START WITH TASK_ID = ?
         CONNECT BY PRIOR TASK_ID = PARENT_TASK_ID
    """;

        try (Connection c = getConn()) {
            c.setAutoCommit(false);
            try {
                // With your FKs on CASCADE, one delete over the subtree is enough.
                try (PreparedStatement p = c.prepareStatement(
                        "DELETE FROM TASKS WHERE TASK_ID IN (" + subtree + ")")) {
                    p.setLong(1, taskId);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    @Override
    public Optional<Task> findById(long id) throws SQLException {
        String sql = """
        SELECT t.*,
               u.username AS assignee_name
        FROM TASKS t
        LEFT JOIN USERS u ON t.ASSIGNEE_ID = u.USER_ID
        WHERE t.TASK_ID = ?
        """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Task> findByProject(long projectId) throws SQLException {
        String sql = """
        SELECT t.*,
               u.username AS assignee_name
        FROM tasks t
        LEFT JOIN users u ON t.assignee_id = u.user_id
        WHERE t.project_id = ?
        ORDER BY t.task_id
    """;

        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }


    @Override public List<Task> findChildren(long parentTaskId) throws SQLException {
        String sql = "SELECT * FROM TASKS WHERE PARENT_TASK_ID=? ORDER BY TASK_ID";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parentTaskId); try (ResultSet rs = ps.executeQuery()) {
                List<Task> list = new ArrayList<>(); while (rs.next()) list.add(map(rs)); return list;
            }
        }
    }

    @Override public List<Task> findBlocked(long projectId) throws SQLException {
        String sql = """
            SELECT T.*
              FROM VW_BLOCKED_TASKS V
              JOIN TASKS T ON T.TASK_ID = V.TASK_ID
             WHERE T.PROJECT_ID=?
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId); try (ResultSet rs = ps.executeQuery()) {
                List<Task> list = new ArrayList<>(); while (rs.next()) list.add(map(rs)); return list;
            }
        }
    }

    @Override public void updateStatus(long taskId, String status) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("UPDATE TASKS SET STATUS=? WHERE TASK_ID=?")) {
            ps.setString(1, status); ps.setLong(2, taskId); ps.executeUpdate();
        }
    }

    @Override public void setProgress(long taskId, double pct) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("UPDATE TASKS SET PROGRESS_PCT=? WHERE TASK_ID=?")) {
            ps.setDouble(1, pct); ps.setLong(2, taskId); ps.executeUpdate();
        }
    }

    @Override
    public void assignTo(long taskId, long userId) throws SQLException {
        String sql = "UPDATE TASKS SET ASSIGNEE_ID=? WHERE TASK_ID=?";
        try (var c = getConn(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId); ps.setLong(2, taskId); ps.executeUpdate();
        }
    }

}


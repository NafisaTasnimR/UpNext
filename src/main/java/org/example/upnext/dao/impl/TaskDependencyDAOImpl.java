package org.example.upnext.dao.impl;

import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.TaskDependencyDAO;
import org.example.upnext.model.Task;
import org.example.upnext.model.TaskDependency;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.sql.Date; // This is java.sql.Date

public class TaskDependencyDAOImpl extends BaseDAO implements TaskDependencyDAO {

    private TaskDependency map(ResultSet rs) throws SQLException {
        TaskDependency d = new TaskDependency();
        d.setDepId(rs.getLong("DEP_ID"));
        d.setPredecessorTaskId(rs.getLong("PREDECESSOR_TASK_ID"));
        d.setSuccessorTaskId(rs.getLong("SUCCESSOR_TASK_ID"));
        Timestamp t = rs.getTimestamp("CREATED_AT");
        if (t != null) d.setCreatedAt(t.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        return d;
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task t = new Task();

        // Map all columns from your TASKS table
        t.setTaskId(rs.getLong("TASK_ID"));
        t.setProjectId(rs.getLong("PROJECT_ID"));

        // Handle nullable parent task ID
        long parentTaskId = rs.getLong("PARENT_TASK_ID");
        if (!rs.wasNull()) {
            t.setParentTaskId(parentTaskId);
        } else {
            t.setParentTaskId(null);
        }

        t.setTitle(rs.getString("TITLE"));
        t.setDescription(rs.getString("DESCRIPTION"));

        // Handle nullable assignee ID
        long assigneeId = rs.getLong("ASSIGNEE_ID");
        if (!rs.wasNull()) {
            t.setAssigneeId(assigneeId);
        } else {
            t.setAssigneeId(null);
        }

        t.setStatus(rs.getString("STATUS"));
        t.setPriority(rs.getString("PRIORITY"));

        // Handle date fields
        Date startDate = rs.getDate("START_DATE");
        if (startDate != null) {
            t.setStartDate(startDate.toLocalDate());
        } else {
            t.setStartDate(null);
        }

        Date dueDate = rs.getDate("DUE_DATE");
        if (dueDate != null) {
            t.setDueDate(dueDate.toLocalDate());
        } else {
            t.setDueDate(null);
        }

        t.setProgressPct(rs.getDouble("PROGRESS_PCT"));

        // Handle nullable numeric fields
        double estimatedHours = rs.getDouble("ESTIMATED_HOURS");
        if (!rs.wasNull()) {
            t.setEstimatedHours(estimatedHours);
        } else {
            t.setEstimatedHours(null);
        }

        double actualHours = rs.getDouble("ACTUAL_HOURS");
        if (!rs.wasNull()) {
            t.setActualHours(actualHours);
        } else {
            t.setActualHours(null);
        }

        // Handle IS_BLOCKED char field
        String isBlocked = rs.getString("IS_BLOCKED");
        t.setBlocked("Y".equals(isBlocked));

        // Handle timestamp fields
        Timestamp createdAt = rs.getTimestamp("CREATED_AT");
        if (createdAt != null) {
            t.setCreatedAt(createdAt.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        }

        Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
        if (updatedAt != null) {
            t.setUpdatedAt(updatedAt.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        }

        // Map the joined assignee name (if your query includes it)
        try {
            // This will work if your query includes a join to USERS table
            t.setAssigneeName(rs.getString("assignee_name"));
        } catch (SQLException e) {
            // Column not present in this result set, which is fine
            t.setAssigneeName(null);
        }

        return t;
    }

    // Helper method in TaskDependencyDAOImpl
    private int getPriorityValue(String priority) {
        if (priority == null) return 0;
        switch (priority.toUpperCase()) {
            case "CRITICAL": return 4;
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    @Override public long create(TaskDependency d) throws SQLException {
        String sql = "INSERT INTO TASK_DEPENDENCIES (DEP_ID, PREDECESSOR_TASK_ID, SUCCESSOR_TASK_ID) VALUES (?,?,?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            long id = nextVal(c, "TASK_DEP_SEQ");
            ps.setLong(1, id);
            ps.setLong(2, d.getPredecessorTaskId());
            ps.setLong(3, d.getSuccessorTaskId());
            ps.executeUpdate();
            return id;
        }
    }

    @Override public void delete(long depId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM TASK_DEPENDENCIES WHERE DEP_ID=?")) {
            ps.setLong(1, depId); ps.executeUpdate();
        }
    }

    @Override public List<TaskDependency> findForSuccessor(long succId) throws SQLException {
        String sql = "SELECT * FROM TASK_DEPENDENCIES WHERE SUCCESSOR_TASK_ID=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, succId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TaskDependency> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override public boolean hasUnfinishedPredecessor(long succId) throws SQLException {
        String sql = """
            SELECT COUNT(*)
              FROM TASK_DEPENDENCIES D
              JOIN TASKS P ON P.TASK_ID = D.PREDECESSOR_TASK_ID
             WHERE D.SUCCESSOR_TASK_ID=? AND P.STATUS <> 'DONE'
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, succId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public List<Task> getHigherPriorityUnfinishedTasks(long taskId, long parentTaskId, String currentPriority) throws SQLException {
        String sql = """
        SELECT t.*, u.username AS assignee_name
        FROM TASKS t
        LEFT JOIN USERS u ON t.ASSIGNEE_ID = u.USER_ID
        WHERE t.PARENT_TASK_ID = ? 
          AND t.TASK_ID != ? 
          AND t.STATUS NOT IN ('DONE', 'CANCELLED')
          AND (
            (t.PRIORITY = 'CRITICAL' AND ? != 'CRITICAL') OR
            (t.PRIORITY = 'HIGH' AND ? IN ('MEDIUM', 'LOW')) OR
            (t.PRIORITY = 'MEDIUM' AND ? = 'LOW')
          )
        ORDER BY 
          CASE t.PRIORITY 
            WHEN 'CRITICAL' THEN 4
            WHEN 'HIGH' THEN 3
            WHEN 'MEDIUM' THEN 2
            WHEN 'LOW' THEN 1
            ELSE 0
          END DESC
        """;

        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parentTaskId);
            ps.setLong(2, taskId);
            ps.setString(3, currentPriority);
            ps.setString(4, currentPriority);
            ps.setString(5, currentPriority);

            try (ResultSet rs = ps.executeQuery()) {
                List<Task> higherPriorityTasks = new ArrayList<>();
                while (rs.next()) {
                    higherPriorityTasks.add(mapTask(rs)); // You'll need a mapTask method
                }
                return higherPriorityTasks;
            }
        }
    }
}


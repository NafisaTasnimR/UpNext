package org.example.upnext.dao.impl;

import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.TaskDependencyDAO;
import org.example.upnext.model.TaskDependency;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

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
}


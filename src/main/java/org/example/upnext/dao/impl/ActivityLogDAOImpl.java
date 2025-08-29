package org.example.upnext.dao.impl;

import org.example.upnext.dao.ActivityLogDAO;
import org.example.upnext.dao.BaseDAO;
import org.example.upnext.model.ActivityLog;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class ActivityLogDAOImpl extends BaseDAO implements ActivityLogDAO {

    private ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog a = new ActivityLog();
        a.setLogId(rs.getLong("LOG_ID"));
        Timestamp ts = rs.getTimestamp("OCCURRED_AT");
        if (ts != null) a.setOccurredAt(ts.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        a.setEntityType(rs.getString("ENTITY_TYPE"));
        a.setEntityId(rs.getLong("ENTITY_ID"));
        a.setAction(rs.getString("ACTION"));
        a.setPerformedBy(rs.getString("PERFORMED_BY"));
        long p = rs.getLong("PROJECT_ID"); if (!rs.wasNull()) a.setProjectId(p);
        long t = rs.getLong("TASK_ID");    if (!rs.wasNull()) a.setTaskId(t);
        a.setDetails(rs.getString("DETAILS"));
        return a;
    }

    @Override public List<ActivityLog> findByProject(long pid) throws SQLException {
        String sql = "SELECT * FROM ACTIVITY_LOGS WHERE PROJECT_ID=? ORDER BY OCCURRED_AT DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override public List<ActivityLog> findByTask(long tid) throws SQLException {
        String sql = "SELECT * FROM ACTIVITY_LOGS WHERE TASK_ID=? ORDER BY OCCURRED_AT DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tid);
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }
}


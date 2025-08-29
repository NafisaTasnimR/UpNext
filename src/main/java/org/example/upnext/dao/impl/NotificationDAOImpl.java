package org.example.upnext.dao.impl;

import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.NotificationDAO;
import org.example.upnext.model.Notification;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class NotificationDAOImpl extends BaseDAO implements NotificationDAO {

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotifId(rs.getLong("NOTIF_ID"));
        n.setUserId(rs.getLong("USER_ID"));
        long t = rs.getLong("TASK_ID"); if (!rs.wasNull()) n.setTaskId(t);
        n.setMessage(rs.getString("MESSAGE"));
        n.setRead("Y".equals(rs.getString("IS_READ")));
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        if (ts != null) n.setCreatedAt(ts.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        return n;
    }

    @Override public List<Notification> findUnreadByUser(long userId) throws SQLException {
        String sql = "SELECT * FROM NOTIFICATIONS WHERE USER_ID=? AND IS_READ='N' ORDER BY CREATED_AT DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override public void markRead(long notifId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("UPDATE NOTIFICATIONS SET IS_READ='Y' WHERE NOTIF_ID=?")) {
            ps.setLong(1, notifId); ps.executeUpdate();
        }
    }
}


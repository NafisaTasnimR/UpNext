package org.example.upnext.dao;


import org.example.upnext.model.Notification;

import java.sql.SQLException;
import java.util.List;

public interface NotificationDAO {
    List<Notification> findUnreadByUser(long userId) throws SQLException;
    void markRead(long notifId) throws SQLException;
}


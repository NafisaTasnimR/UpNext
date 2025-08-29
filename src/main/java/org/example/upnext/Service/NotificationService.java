package org.example.upnext.Service;


import org.example.upnext.dao.NotificationDAO;
import org.example.upnext.model.Notification;


import java.sql.SQLException;
import java.util.List;


public class NotificationService {
    private final NotificationDAO dao;
    public NotificationService(NotificationDAO dao) { this.dao = dao; }


    public List<Notification> unread(long userId) throws SQLException { return dao.findUnreadByUser(userId); }
    public void markRead(long notifId) throws SQLException { dao.markRead(notifId); }
}
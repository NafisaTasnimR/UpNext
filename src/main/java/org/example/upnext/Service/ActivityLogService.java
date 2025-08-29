package org.example.upnext.Service;


import org.example.upnext.dao.ActivityLogDAO;
import org.example.upnext.model.ActivityLog;


import java.sql.SQLException;
import java.util.List;


public class ActivityLogService {
    private final ActivityLogDAO dao;
    public ActivityLogService(ActivityLogDAO dao) { this.dao = dao; }


    public List<ActivityLog> forProject(long projectId) throws SQLException { return dao.findByProject(projectId); }
    public List<ActivityLog> forTask(long taskId) throws SQLException { return dao.findByTask(taskId); }
}
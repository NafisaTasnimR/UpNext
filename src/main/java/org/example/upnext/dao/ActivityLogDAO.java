package org.example.upnext.dao;

import org.example.upnext.model.ActivityLog;

import java.sql.SQLException;
import java.util.List;

public interface ActivityLogDAO {
    List<ActivityLog> findByProject(long projectId) throws SQLException;
    List<ActivityLog> findByTask(long taskId) throws SQLException;
}


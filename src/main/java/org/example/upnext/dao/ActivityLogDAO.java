package org.example.upnext.dao;

import org.example.upnext.model.ActivityLog;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ActivityLogDAO {
    List<ActivityLog> findByProject(long projectId) throws SQLException;
    List<ActivityLog> findByTask(long taskId) throws SQLException;
    Optional<String> findProjectCreator(long projectId) throws SQLException;
    Optional<String> findTaskCreator(long taskId) throws SQLException;
}


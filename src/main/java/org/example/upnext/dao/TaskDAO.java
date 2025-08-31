package org.example.upnext.dao;

import org.example.upnext.model.Task;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface TaskDAO {
    long create(Task t) throws SQLException;
    void update(Task t) throws SQLException;
    void delete(long taskId) throws SQLException;

    Optional<Task> findById(long taskId) throws SQLException;
    List<Task> findByProject(long projectId) throws SQLException;
    List<Task> findChildren(long parentTaskId) throws SQLException;
    List<Task> findBlocked(long projectId) throws SQLException;
    void assignTo(long taskId, long userId) throws java.sql.SQLException;
    void updateStatus(long taskId, String status) throws SQLException;
    void setProgress(long taskId, double pct) throws SQLException;
}


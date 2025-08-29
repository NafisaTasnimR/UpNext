package org.example.upnext.dao;


import org.example.upnext.model.TaskDependency;

import java.sql.SQLException;
import java.util.List;

public interface TaskDependencyDAO {
    long create(TaskDependency d) throws SQLException;
    void delete(long depId) throws SQLException;
    List<TaskDependency> findForSuccessor(long successorTaskId) throws SQLException;
    boolean hasUnfinishedPredecessor(long successorTaskId) throws SQLException;
}


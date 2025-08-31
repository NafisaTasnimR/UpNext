package org.example.upnext.dao;

import org.example.upnext.model.Project;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ProjectDAO {
    long create(Project p) throws SQLException;
    Optional<Project> findById(long id) throws SQLException;
    List<Project> findByOwner(long ownerId) throws SQLException;
    List<Project> findAll() throws SQLException;
    void update(Project p) throws SQLException;
    void delete(long id) throws SQLException;
    List<Project> findByManager(long managerId) throws SQLException;
    void updateStatus(long projectId, String status) throws SQLException;
    void assignManager(long projectId, long managerId) throws SQLException;
    java.util.List<org.example.upnext.model.Project> findByMember(long userId) throws java.sql.SQLException;
}


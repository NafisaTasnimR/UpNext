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
}


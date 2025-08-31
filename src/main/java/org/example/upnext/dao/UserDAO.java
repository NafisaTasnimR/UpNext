package org.example.upnext.dao;

import org.example.upnext.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    long create(User u) throws SQLException;
    Optional<User> findById(long id) throws SQLException;
    Optional<User> findByUsername(String username) throws SQLException;
    Optional<User> findByEmail(String email) throws SQLException;
    List<User> findAll() throws SQLException;
    void update(User u) throws SQLException;
    void delete(long id) throws SQLException;
    java.util.List<org.example.upnext.model.User> findManagers() throws java.sql.SQLException;
    java.util.List<org.example.upnext.model.User> findMembers() throws java.sql.SQLException;
    java.util.List<org.example.upnext.model.User> findMembersByProject(long projectId) throws java.sql.SQLException;

}


package org.example.upnext.Service;


import org.example.upnext.dao.UserDAO;
import org.example.upnext.model.User;


import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public class UserService {
    private final UserDAO userDAO;
    public UserService(UserDAO userDAO) { this.userDAO = userDAO; }


    public long create(User u) throws SQLException { return userDAO.create(u); }
    public Optional<User> byId(long id) throws SQLException { return userDAO.findById(id); }
    public Optional<User> byUsername(String name) throws SQLException { return userDAO.findByUsername(name); }
    public Optional<User> byEmail(String email) throws SQLException { return userDAO.findByEmail(email); }
    public List<User> all() throws SQLException { return userDAO.findAll(); }
    public void update(User u) throws SQLException { userDAO.update(u); }
    public void delete(long id) throws SQLException { userDAO.delete(id); }


    public void changeStatus(long id, String status) throws SQLException {
        User u = userDAO.findById(id).orElseThrow(() -> new SQLException("User not found"));
        u.setStatus(status); userDAO.update(u);
    }


    public void changeRole(long id, String globalRole) throws SQLException {
        User u = userDAO.findById(id).orElseThrow(() -> new SQLException("User not found"));
        u.setGlobalRole(globalRole); userDAO.update(u);
    }
}
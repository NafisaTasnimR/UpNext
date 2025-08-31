package org.example.upnext.dao.impl;


import org.example.upnext.config.Db;
import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.UserDAO;
import org.example.upnext.model.User;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class UserDAOImpl extends BaseDAO implements UserDAO {

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("USER_ID"));
        u.setUsername(rs.getString("USERNAME"));
        u.setEmail(rs.getString("EMAIL"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        u.setGlobalRole(rs.getString("GLOBAL_ROLE"));
        u.setStatus(rs.getString("STATUS"));
        Timestamp c = rs.getTimestamp("CREATED_AT");
        Timestamp m = rs.getTimestamp("UPDATED_AT");
        if (c != null) u.setCreatedAt(c.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        if (m != null) u.setUpdatedAt(m.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        return u;
    }

    @Override
    public long create(User u) throws SQLException {
        String sql = "INSERT INTO USERS (USER_ID, USERNAME, EMAIL, PASSWORD_HASH, GLOBAL_ROLE, STATUS) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            long id = nextVal(conn, "USERS_SEQ");
            ps.setLong(1, id);
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getGlobalRole());
            ps.setString(6, u.getStatus() == null ? "ACTIVE" : u.getStatus());
            ps.executeUpdate();
            return id;
        }
    }

    @Override public Optional<User> findById(long id) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM USERS WHERE USER_ID=?")) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    @Override public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM USERS WHERE USERNAME=?")) {
            ps.setString(1, username); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    @Override public Optional<User> findByEmail(String email) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM USERS WHERE EMAIL=?")) {
            ps.setString(1, email); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    @Override public List<User> findAll() throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM USERS ORDER BY USERNAME");
             ResultSet rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    @Override public void update(User u) throws SQLException {
        String sql = "UPDATE USERS SET USERNAME=?, EMAIL=?, PASSWORD_HASH=?, GLOBAL_ROLE=?, STATUS=? WHERE USER_ID=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getGlobalRole());
            ps.setString(5, u.getStatus());
            ps.setLong(6, u.getUserId());
            ps.executeUpdate();
        }
    }

    @Override public void delete(long id) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM USERS WHERE USER_ID=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }
    @Override
    public List<User> findManagers() throws SQLException {
        String sql = "SELECT * FROM USERS WHERE UPPER(GLOBAL_ROLE)='MANAGER'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    @Override
    public List<User> findMembers() throws SQLException {
        String sql = "SELECT * FROM USERS WHERE UPPER(GLOBAL_ROLE)='MEMBER'";
        try (var c = Db.getConnection(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>(); while (rs.next()) list.add(map(rs)); return list;
        }
    }

    @Override
    public List<User> findMembersByProject(long projectId) throws SQLException {
        String sql = """
      SELECT u.* FROM USERS u
      JOIN PROJECT_MEMBERS pm ON pm.USER_ID=u.USER_ID
      WHERE pm.PROJECT_ID=?
    """;
        try (var c = Db.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (var rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>(); while (rs.next()) list.add(map(rs)); return list;
            }
        }
    }


}


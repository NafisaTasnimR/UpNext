package org.example.upnext.dao.impl;

import org.example.upnext.config.Db;
import org.example.upnext.dao.ProjectMemberDAO;
import org.example.upnext.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectMemberDAOImpl implements ProjectMemberDAO {

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("USER_ID"));
        u.setUsername(rs.getString("USERNAME").trim());
        u.setEmail(rs.getString("EMAIL"));
        u.setGlobalRole(rs.getString("GLOBAL_ROLE"));
        u.setStatus(rs.getString("STATUS"));
        return u;
    }

    @Override
    public void addMember(long projectId, long userId, String role) throws SQLException {
        String sql = """
            MERGE INTO PROJECT_MEMBERS pm
            USING dual
            ON (pm.PROJECT_ID=? AND pm.USER_ID=?)
            WHEN MATCHED THEN
                UPDATE SET PROJECT_ROLE=?, UPDATED_AT=SYSTIMESTAMP
            WHEN NOT MATCHED THEN
                INSERT (PROJECT_MEMBER_ID, PROJECT_ID, USER_ID, PROJECT_ROLE, CREATED_AT)
                VALUES (PROJECT_MEMBERS_SEQ.NEXTVAL, ?, ?, ?, SYSTIMESTAMP)
        """;
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            ps.setString(3, role);
            ps.setLong(4, projectId);
            ps.setLong(5, userId);
            ps.setString(6, role);
            ps.executeUpdate();
        }
    }

    @Override
    public void removeMember(long projectId, long userId) throws SQLException {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM PROJECT_MEMBERS WHERE PROJECT_ID=? AND USER_ID=?")) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> listMembers(long projectId) throws SQLException {
        String sql = """
      SELECT u.* FROM USERS u
      JOIN PROJECT_MEMBERS pm ON pm.USER_ID=u.USER_ID
      WHERE pm.PROJECT_ID=?
    """;
        try (var c = Db.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (var rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getLong("USER_ID"));
                    u.setUsername(rs.getString("USERNAME"));
                    u.setEmail(rs.getString("EMAIL"));
                    u.setGlobalRole(rs.getString("GLOBAL_ROLE"));
                    u.setStatus(rs.getString("STATUS"));
                    list.add(u);
                }
                return list;
            }
        }
    }


    @Override
    public boolean isMember(long projectId, long userId) throws SQLException {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM PROJECT_MEMBERS WHERE PROJECT_ID=? AND USER_ID=?")) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean hasRole(long projectId, long userId, String role) throws SQLException {
        String sql = "SELECT 1 FROM PROJECT_MEMBERS WHERE PROJECT_ID=? AND USER_ID=? AND UPPER(PROJECT_ROLE)=UPPER(?)";
        try (var c = Db.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            ps.setString(3, role);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public List<Long> listMemberIds(long projectId) throws SQLException {
        String sql = "SELECT USER_ID FROM PROJECT_MEMBERS WHERE PROJECT_ID=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getLong("USER_ID"));
                }
                return ids;
            }
        }
    }


}

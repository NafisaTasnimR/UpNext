package org.example.upnext.dao.impl;

import org.example.upnext.config.Db;
import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.ProjectDAO;
import org.example.upnext.model.Project;

import java.sql.*;
import java.sql.Date;
import java.util.*;


public class ProjectDAOImpl extends BaseDAO implements ProjectDAO {

    private Project map(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getLong("PROJECT_ID"));
        p.setName(rs.getString("NAME"));
        p.setDescription(rs.getString("DESCRIPTION"));
        p.setOwnerId(rs.getLong("OWNER_ID"));
        Date sd = rs.getDate("START_DATE"); if (sd != null) p.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("END_DATE");   if (ed != null) p.setEndDate(ed.toLocalDate());
        p.setStatus(rs.getString("STATUS"));
        return p;
    }

    @Override
    public long create(Project p) throws SQLException {
        String sql = "INSERT INTO PROJECTS (PROJECT_ID, NAME, DESCRIPTION, OWNER_ID, START_DATE, END_DATE, STATUS) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            long id = nextVal(c, "PROJECTS_SEQ");
            ps.setLong(1, id);
            ps.setString(2, p.getName());
            ps.setString(3, p.getDescription());
            ps.setLong(4, p.getOwnerId());
            if (p.getStartDate() != null) ps.setDate(5, Date.valueOf(p.getStartDate())); else ps.setNull(5, Types.DATE);
            if (p.getEndDate() != null)   ps.setDate(6, Date.valueOf(p.getEndDate()));   else ps.setNull(6, Types.DATE);
            ps.setString(7, p.getStatus() == null ? "PLANNING" : p.getStatus());
            ps.executeUpdate();
            return id;
        }
    }

    @Override public Optional<Project> findById(long id) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM PROJECTS WHERE PROJECT_ID=?")) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    @Override public List<Project> findByOwner(long ownerId) throws SQLException {
        String sql = "SELECT * FROM PROJECTS WHERE OWNER_ID=? ORDER BY PROJECT_ID DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Project> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<Project> findAll() throws SQLException {
        String sql = "SELECT * FROM PROJECTS ORDER BY CREATED_AT DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Project> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    @Override public void update(Project p) throws SQLException {
        String sql = "UPDATE PROJECTS SET NAME=?, DESCRIPTION=?, OWNER_ID=?, START_DATE=?, END_DATE=?, STATUS=? WHERE PROJECT_ID=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setLong(3, p.getOwnerId());
            if (p.getStartDate() != null) ps.setDate(4, Date.valueOf(p.getStartDate())); else ps.setNull(4, Types.DATE);
            if (p.getEndDate() != null)   ps.setDate(5, Date.valueOf(p.getEndDate()));   else ps.setNull(5, Types.DATE);
            ps.setString(6, p.getStatus());
            ps.setLong(7, p.getProjectId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(long projectId) throws SQLException {
        try (Connection c = getConn();
             PreparedStatement p = c.prepareStatement(
                     "DELETE FROM PROJECTS WHERE PROJECT_ID=?")) {
            p.setLong(1, projectId);
            p.executeUpdate();
        }
    }

    @Override
    public List<Project> findByManager(long managerId) throws SQLException {
        // FIXED QUERY: More comprehensive approach to find manager projects
        String sql = """
        SELECT DISTINCT p.* 
        FROM PROJECTS p
        WHERE p.OWNER_ID = ?                               -- Project owner
           OR p.ASSIGNED_MANAGER_ID = ?                    -- Direct assignment field  
           OR EXISTS (
               SELECT 1 FROM PROJECT_MEMBERS pm 
               WHERE pm.PROJECT_ID = p.PROJECT_ID 
                 AND pm.USER_ID = ? 
                 AND UPPER(pm.PROJECT_ROLE) = 'MANAGER'    -- Only MANAGER role, not MEMBER
           )
           OR EXISTS (
               SELECT 1 FROM TASKS t
               WHERE t.PROJECT_ID = p.PROJECT_ID
                 AND t.ASSIGNEE_ID = ?                     -- Manager has tasks assigned
           )
        ORDER BY p.PROJECT_ID DESC
        """;

        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, managerId);  // owner check
            ps.setLong(2, managerId);  // assigned manager check
            ps.setLong(3, managerId);  // PROJECT_MEMBERS manager role check
            ps.setLong(4, managerId);  // has assigned tasks check
            try (ResultSet rs = ps.executeQuery()) {
                List<Project> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override
    public void updateStatus(long projectId, String status) throws SQLException {
        String sql = "UPDATE PROJECTS SET STATUS = ?, UPDATED_AT = SYSTIMESTAMP WHERE PROJECT_ID = ?";

        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, projectId);
            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No project found with ID " + projectId);
            }
        }
    }

    @Override
    public void assignManager(long projectId, long managerId) throws SQLException {
        String sql = "UPDATE PROJECTS SET ASSIGNED_MANAGER_ID=? WHERE PROJECT_ID=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, managerId);
            ps.setLong(2, projectId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Project> findByMember(long userId) throws SQLException {
        String sql = """
      SELECT p.*
        FROM PROJECTS p
        JOIN PROJECT_MEMBERS pm ON pm.PROJECT_ID = p.PROJECT_ID
       WHERE pm.USER_ID = ?
    """;
        try (var c = getConn(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                List<Project> list = new ArrayList<>(); while (rs.next()) list.add(map(rs)); return list;
            }
        }
    }
}
package org.example.upnext.dao.impl;


import org.example.upnext.dao.BaseDAO;
import org.example.upnext.dao.ProjectMemberDAO;
import org.example.upnext.model.ProjectMember;

import java.sql.*;
import java.util.*;

public class ProjectMemberDAOImpl extends BaseDAO implements ProjectMemberDAO {

    private ProjectMember map(ResultSet rs) throws SQLException {
        ProjectMember pm = new ProjectMember();
        pm.setProjectMemberId(rs.getLong("PROJECT_MEMBER_ID"));
        pm.setProjectId(rs.getLong("PROJECT_ID"));
        pm.setUserId(rs.getLong("USER_ID"));
        pm.setProjectRole(rs.getString("PROJECT_ROLE"));
        return pm;
    }

    @Override public long add(ProjectMember pm) throws SQLException {
        String sql = "INSERT INTO PROJECT_MEMBERS (PROJECT_MEMBER_ID, PROJECT_ID, USER_ID, PROJECT_ROLE) VALUES (?,?,?,?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            long id = nextVal(c, "PROJECT_MEMBERS_SEQ");
            ps.setLong(1, id);
            ps.setLong(2, pm.getProjectId());
            ps.setLong(3, pm.getUserId());
            ps.setString(4, pm.getProjectRole());
            ps.executeUpdate();
            return id;
        }
    }

    @Override public void update(ProjectMember pm) throws SQLException {
        String sql = "UPDATE PROJECT_MEMBERS SET PROJECT_ID=?, USER_ID=?, PROJECT_ROLE=? WHERE PROJECT_MEMBER_ID=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pm.getProjectId());
            ps.setLong(2, pm.getUserId());
            ps.setString(3, pm.getProjectRole());
            ps.setLong(4, pm.getProjectMemberId());
            ps.executeUpdate();
        }
    }

    @Override public void remove(long id) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM PROJECT_MEMBERS WHERE PROJECT_MEMBER_ID=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }

    @Override public List<ProjectMember> membersOfProject(long projectId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM PROJECT_MEMBERS WHERE PROJECT_ID=?")) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProjectMember> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    @Override public List<ProjectMember> projectsOfUser(long userId) throws SQLException {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM PROJECT_MEMBERS WHERE USER_ID=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProjectMember> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }
}


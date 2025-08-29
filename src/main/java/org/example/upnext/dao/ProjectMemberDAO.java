package org.example.upnext.dao;


import org.example.upnext.model.ProjectMember;
import java.sql.SQLException;
import java.util.List;

public interface ProjectMemberDAO {
    long add(ProjectMember pm) throws SQLException;
    void update(ProjectMember pm) throws SQLException;
    void remove(long projectMemberId) throws SQLException;
    List<ProjectMember> membersOfProject(long projectId) throws SQLException;
    List<ProjectMember> projectsOfUser(long userId) throws SQLException;
}


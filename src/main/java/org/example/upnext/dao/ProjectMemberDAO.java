package org.example.upnext.dao;


import org.example.upnext.model.ProjectMember;
import org.example.upnext.model.User;

import java.sql.SQLException;
import java.util.List;

public interface ProjectMemberDAO {
    void addMember(long projectId, long userId, String role) throws SQLException;
    void removeMember(long projectId, long userId) throws SQLException;
    List<User> listMembers(long projectId) throws SQLException;
    boolean isMember(long projectId, long userId) throws SQLException;
    boolean hasRole(long projectId, long userId, String role) throws SQLException;


    List<Long> listMemberIds(long projectId)throws SQLException;
}
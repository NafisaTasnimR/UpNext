package org.example.upnext.service;

import org.example.upnext.dao.ProjectDAO;
import org.example.upnext.dao.TaskDAO;
import org.example.upnext.dao.ProjectMemberDAO;
import org.example.upnext.dao.impl.ProjectMemberDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.Task;
import org.example.upnext.model.Project;
import org.example.upnext.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProjectService {
    private final ProjectDAO projectDAO;
    private final TaskDAO taskDAO;
    private final ProjectMemberDAO pmDAO = new ProjectMemberDAOImpl();

    public ProjectService(ProjectDAO projectDAO, TaskDAO taskDAO) {
        this.projectDAO = projectDAO;
        this.taskDAO = taskDAO;
    }

    public long create(Project p) throws SQLException { return projectDAO.create(p); }
    public void update(Project p) throws SQLException { projectDAO.update(p); }
    public void delete(long projectId) throws SQLException { projectDAO.delete(projectId); }

    public Optional<Project> byId(long id) throws SQLException { return projectDAO.findById(id); }
    public List<Project> byOwner(long ownerId) throws SQLException { return projectDAO.findByOwner(ownerId); }
    public List<Project> all() throws SQLException { return projectDAO.findAll(); }
    public List<Project> byManager(long managerId) throws SQLException { return projectDAO.findByManager(managerId); }
    public List<Project> byMember(long userId) throws SQLException { return projectDAO.findByMember(userId); }

    public void assignManager(long projectId, long managerId) throws SQLException {
        pmDAO.addMember(projectId, managerId, "MANAGER");
    }

    public void addMember(long projectId, long userId) throws SQLException {
        pmDAO.addMember(projectId, userId, "MEMBER");
    }

    public void removeMember(long projectId, long userId) throws SQLException {
        pmDAO.removeMember(projectId, userId);
    }

    public List<Long> memberIds(long projectId) throws SQLException {
        return pmDAO.listMemberIds(projectId);
    }

    public List<User> projectMembers(long projectId) throws SQLException {
        return pmDAO.listMembers(projectId);
    }

    /** Average of task progress percentages. */
    public double calculateOverallProgress(long projectId) throws SQLException {
        List<Task> tasks = taskDAO.findByProject(projectId);
        if (tasks.isEmpty()) return 0.0;
        double sum = tasks.stream().mapToDouble(Task::getProgressPct).sum();
        return Math.round((sum / tasks.size()) * 100.0) / 100.0;
    }
    private static String roleOf(User u) {
        return u == null || u.getGlobalRole() == null ? "" : u.getGlobalRole().trim().toUpperCase();
    }

    /** Admin can delete only projects they created (ownerId == userId). */
    public boolean canDeleteProject(long projectId, User user) throws SQLException {
        if (!"ADMIN".equals(roleOf(user))) return false;
        var opt = projectDAO.findById(projectId);
        if (opt.isEmpty()) return false;
        Project p = opt.get();
        return p.getOwnerId() != null && p.getOwnerId().equals(user.getUserId());
    }

    /** Call this from the controller instead of plain delete(projectId). */
    public void deleteProjectWithAuth(long projectId, User user) throws SQLException {
        if (!canDeleteProject(projectId, user)) {
            throw new SecurityException("You are not allowed to delete this project.");
        }
        projectDAO.delete(projectId); // cascades are handled in DB
    }

}

    public boolean isManagerOfProject(long projectId, long userId) throws SQLException {
        return pmDAO.hasRole(projectId, userId, "MANAGER"); // uses PROJECT_MEMBERS.PROJECT_ROLE
    }

}

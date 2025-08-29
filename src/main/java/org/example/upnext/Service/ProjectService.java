package org.example.upnext.Service;


import org.example.upnext.dao.ProjectDAO;
import org.example.upnext.dao.TaskDAO;
import org.example.upnext.model.Project;
import org.example.upnext.model.Task;


import java.sql.SQLException;
import java.util.*;


public class ProjectService {
    private final ProjectDAO projectDAO;
    private final TaskDAO taskDAO;


    public ProjectService(ProjectDAO projectDAO, TaskDAO taskDAO) {
        this.projectDAO = projectDAO; this.taskDAO = taskDAO;
    }


    public long create(Project p) throws SQLException { return projectDAO.create(p); }
    public void update(Project p) throws SQLException { projectDAO.update(p); }
    public void delete(long projectId) throws SQLException { projectDAO.delete(projectId); }


    public Optional<Project> byId(long id) throws SQLException { return projectDAO.findById(id); }
    public List<Project> byOwner(long ownerId) throws SQLException { return projectDAO.findByOwner(ownerId); }
    public List<Project> all() throws SQLException { return projectDAO.findAll(); }


    /** Average of task progress percentages; DB triggers keep parents rolled-up. */
    public double calculateOverallProgress(long projectId) throws SQLException {
        List<Task> tasks = taskDAO.findByProject(projectId);
        if (tasks.isEmpty()) return 0.0;
        double sum = tasks.stream().mapToDouble(Task::getProgressPct).sum();
        return Math.round((sum / tasks.size()) * 100.0) / 100.0;
    }
}
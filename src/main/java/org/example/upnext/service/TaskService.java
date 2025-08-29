package org.example.upnext.service;


import org.example.upnext.dao.TaskDAO;
import org.example.upnext.dao.TaskDependencyDAO;
import org.example.upnext.model.Task;


import java.sql.SQLException;
import java.util.List;


public class TaskService {
    private final TaskDAO taskDAO;
    private final TaskDependencyDAO depDAO;


    public TaskService(TaskDAO taskDAO, TaskDependencyDAO depDAO) {
        this.taskDAO = taskDAO; this.depDAO = depDAO;
    }


    public long create(Task t) throws SQLException { return taskDAO.create(t); }
    public void update(Task t) throws SQLException { taskDAO.update(t); }
    public void delete(long taskId) throws SQLException { taskDAO.delete(taskId); }


    public Task get(long id) throws SQLException { return taskDAO.findById(id).orElseThrow(() -> new SQLException("Task not found")); }


    public List<Task> projectTasks(long projectId) throws SQLException { return taskDAO.findByProject(projectId); }
    public List<Task> children(long parentId) throws SQLException { return taskDAO.findChildren(parentId); }
    public List<Task> blocked(long projectId) throws SQLException { return taskDAO.findBlocked(projectId); }


    public boolean dependenciesSatisfied(long taskId) throws SQLException {
        return !depDAO.hasUnfinishedPredecessor(taskId);
    }


    public void start(long taskId) throws SQLException {
        if (!dependenciesSatisfied(taskId)) throw new SQLException("Task has unfinished dependencies");
        taskDAO.updateStatus(taskId, "IN_PROGRESS");
    }


    public void complete(long taskId) throws SQLException {
        if (!dependenciesSatisfied(taskId)) throw new SQLException("Task has unfinished dependencies");
        taskDAO.setProgress(taskId, 100.0);
        taskDAO.updateStatus(taskId, "DONE");
// Parent roll-up handled by DB compound trigger
    }


    public void setProgress(long taskId, double pct) throws SQLException {
        if (pct < 0 || pct > 100) throw new SQLException("Progress must be 0..100");
        taskDAO.setProgress(taskId, pct);
        if (pct == 100.0) taskDAO.updateStatus(taskId, "DONE");
    }
}
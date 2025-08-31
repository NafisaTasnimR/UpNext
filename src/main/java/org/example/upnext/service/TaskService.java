package org.example.upnext.service;


import org.example.upnext.dao.ProjectDAO;
import org.example.upnext.dao.TaskDAO;
import org.example.upnext.dao.TaskDependencyDAO;
import org.example.upnext.dao.impl.ProjectDAOImpl;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;
import java.util.Locale;


import java.sql.SQLException;
import java.util.List;


public class TaskService {
    private final TaskDAO taskDAO;

    private ProjectDAO projectDAO;

    private final TaskDependencyDAO depDAO;
    private org.example.upnext.dao.ActivityLogDAO activityLogDAO; // injected later



    public TaskService(TaskDAO taskDAO, TaskDependencyDAO depDAO) {
        this.taskDAO = taskDAO; this.depDAO = depDAO;
    }

    public void setActivityLogDAO(org.example.upnext.dao.ActivityLogDAO dao) {
        this.activityLogDAO = dao;
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

    private static String roleOf(User u) {
        return u == null || u.getGlobalRole() == null ? "" : u.getGlobalRole().trim().toUpperCase(Locale.ROOT);
    }

    private String findTaskCreator(long taskId) throws SQLException {
        if (activityLogDAO == null)
            throw new SQLException("ActivityLogDAO not configured in TaskService");
        // first INSERT log for this task
        return activityLogDAO.findTaskCreator(taskId).orElse(null);
    }

    /** Role rules:
     *  ADMIN  -> may delete tasks they created.
     *  MANAGER-> may delete tasks they created.
     *  MEMBER -> may delete only subtasks they created.
     */
    public boolean canDeleteTask(long taskId, User user) throws SQLException {
        var opt = taskDAO.findById(taskId);
        if (opt.isEmpty()) return false;
        var t = opt.get();

        boolean isTopLevel = (t.getParentTaskId() == null);
        String creator = findTaskCreator(taskId);
        if (creator == null) return false;

        String role = roleOf(user);
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
            return user.getUsername().equals(creator);
        } else if ("MEMBER".equals(role)) {
            return !isTopLevel && user.getUsername().equals(creator);
        }
        return false;
    }

    /** Use this instead of plain delete(taskId) when called from the UI. */
    public void deleteTaskWithAuth(long taskId, User user) throws SQLException {
        if (!canDeleteTask(taskId, user))
            throw new SecurityException("You are not allowed to delete this task.");
        // This uses your existing DAO delete; with your DB CASCADE, subtasks & logs go with it
        taskDAO.delete(taskId);
    }

    public void setProjectDAO(ProjectDAOImpl projectDAO) {
        this.projectDAO = projectDAO;
    }
}
package org.example.upnext.service;

import org.example.upnext.dao.ProjectDAO;
import org.example.upnext.dao.ProjectMemberDAO;
import org.example.upnext.dao.TaskDAO;
import org.example.upnext.dao.TaskDependencyDAO;
import org.example.upnext.dao.impl.ProjectDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TaskService {
    private final TaskDAO taskDAO;
    private ProjectDAO projectDAO;
    private final TaskDependencyDAO depDAO;
    private org.example.upnext.dao.ActivityLogDAO activityLogDAO;
    private final ProjectMemberDAO pmDAO = new org.example.upnext.dao.impl.ProjectMemberDAOImpl();
    private ProjectService projectService; // Enhanced integration

    public TaskService(TaskDAO taskDAO, TaskDependencyDAO depDAO) {
        this.taskDAO = taskDAO;
        this.depDAO = depDAO;
    }

    // ==============================================================================
    // Service Injection Methods
    // ==============================================================================

    public void setProjectService(ProjectService projectService) {
        this.projectService = projectService;
    }

    public void setActivityLogDAO(org.example.upnext.dao.ActivityLogDAO dao) {
        this.activityLogDAO = dao;
    }

    public void setProjectDAO(ProjectDAOImpl projectDAO) {
        this.projectDAO = projectDAO;
    }

    // ==============================================================================
    // Enhanced Validation Methods
    // ==============================================================================

    /**
     * Validates if a task can be created in this project with detailed error messages
     */
    public void validateTaskCreation(long projectId) throws SQLException {
        if (projectService == null) return; // Skip validation if projectService not set

        Project project = projectDAO.findById(projectId)
                .orElseThrow(() -> new SQLException("Project not found"));

        switch (project.getStatus()) {
            case "COMPLETED":
                throw new SQLException("Cannot create new tasks in a COMPLETED project.\n\n" +
                        "Project: " + project.getName() + "\n" +
                        "Status: COMPLETED\n\n" +
                        "To add new tasks, change the project status back to ACTIVE or PLANNING first.");

            case "CANCELLED":
                throw new SQLException("Cannot create new tasks in a CANCELLED project.\n\n" +
                        "Project: " + project.getName() + "\n" +
                        "Status: CANCELLED\n\n" +
                        "To add new tasks, change the project status back to ACTIVE or PLANNING first.");

            default:
                // PLANNING, ACTIVE, ON_HOLD are allowed
                return;
        }
    }

    /**
     * Validates if a subtask can be created under the given parent task
     */
    public void validateSubtaskCreation(long parentTaskId) throws SQLException {
        Task parent = get(parentTaskId);

        // Check project status first
        validateTaskCreation(parent.getProjectId());

        // Check if parent task allows subtasks
        switch (parent.getStatus()) {
            case "BLOCKED":
                throw new SQLException("Cannot create subtasks under a BLOCKED task.\n\n" +
                        "Parent Task: " + parent.getTitle() + "\n" +
                        "Status: BLOCKED\n\n" +
                        "Resolve the blocking issues first before creating subtasks.");

            case "CANCELLED":
                throw new SQLException("Cannot create subtasks under a CANCELLED task.\n\n" +
                        "Parent Task: " + parent.getTitle() + "\n" +
                        "Status: CANCELLED\n\n" +
                        "Change the parent task status first to create subtasks.");

            case "DONE":
                throw new SQLException("Cannot create subtasks under a COMPLETED task.\n\n" +
                        "Parent Task: " + parent.getTitle() + "\n" +
                        "Status: COMPLETED\n\n" +
                        "Creating subtasks would change the parent status back to IN_PROGRESS.\n" +
                        "Are you sure you want to reopen this completed task?");

            default:
                // TODO, IN_PROGRESS, ON_HOLD are allowed
                return;
        }
    }

    // ==============================================================================
    // Enhanced Task Creation with Date-Based Status Logic
    // ==============================================================================

    /**
     * Enhanced task creation that considers task dates for intelligent status determination
     */
    public long createTaskWithProjectStatus(long projectId, String title, String description,
                                            Long assigneeId, LocalDate startDate, LocalDate dueDate,
                                            String actingRole) throws SQLException {
        // Check if tasks can be created in this project
        if (projectService != null && !projectService.canCreateTasksInProject(projectId)) {
            Project project = projectDAO.findById(projectId)
                    .orElseThrow(() -> new SQLException("Project not found"));
            throw new SQLException("Cannot create new tasks in a " + project.getStatus() + " project.");
        }

        // Validate assignee is a project member (if specified)
        if (assigneeId != null && !pmDAO.isMember(projectId, assigneeId)) {
            throw new SQLException("Assignee must be a member of this project.");
        }

        // Determine task status based on project status AND task dates
        String taskStatus = projectService != null ?
                projectService.getNewTaskStatusForProject(projectId, startDate, dueDate) : "TODO";

        // Create the task
        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setAssigneeId(assigneeId);
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
        task.setStatus(taskStatus);
        task.setPriority("MEDIUM");
        task.setProgressPct(0.0);

        return taskDAO.create(task);
    }

    // Overloaded method for backward compatibility
    public long createTaskWithProjectStatus(long projectId, String title, String description,
                                            Long assigneeId, String actingRole) throws SQLException {
        return createTaskWithProjectStatus(projectId, title, description, assigneeId, null, null, actingRole);
    }

    /**
     * Enhanced task creation with full validation (recommended for UI controllers)
     */
    public long createTaskWithValidation(long projectId, String title, String description,
                                         Long assigneeId, LocalDate startDate, LocalDate dueDate,
                                         String actingRole) throws SQLException {
        // Full validation first
        validateTaskCreation(projectId);

        // Then use existing enhanced creation method
        return createTaskWithProjectStatus(projectId, title, description, assigneeId, startDate, dueDate, actingRole);
    }

    // ==============================================================================
    // Enhanced Subtask Creation with Date and Validation Support
    // ==============================================================================

    /**
     * Enhanced subtask creation with date consideration
     */
    public long createSubtaskWithDates(long parentTaskId, String title, String description,
                                       Long assigneeId, LocalDate startDate, LocalDate dueDate,
                                       long actingUserId, String actingGlobalRole) throws SQLException {
        Task parent = get(parentTaskId);

        // Check if tasks can be created in this project
        if (projectService != null && !projectService.canCreateTasksInProject(parent.getProjectId())) {
            Project project = projectDAO.findById(parent.getProjectId())
                    .orElseThrow(() -> new SQLException("Project not found"));
            throw new SQLException("Cannot create new subtasks in a " + project.getStatus() + " project.");
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(actingGlobalRole);
        boolean isManager = pmDAO.hasRole(parent.getProjectId(), actingUserId, "MANAGER");
        boolean isParentAssignee = parent.getAssigneeId() != null && parent.getAssigneeId().equals(actingUserId);

        if (!isAdmin && !isManager && !isParentAssignee) {
            throw new SQLException("Only Admin/Manager or the parent task's assignee can create subtasks.");
        }

        if (assigneeId != null && !pmDAO.isMember(parent.getProjectId(), assigneeId)) {
            throw new SQLException("Assignee must be a member of the project.");
        }

        // Determine subtask status based on project status AND dates
        String subtaskStatus = projectService != null ?
                projectService.getNewTaskStatusForProject(parent.getProjectId(), startDate, dueDate) : "TODO";

        Task sub = new Task();
        sub.setProjectId(parent.getProjectId());
        sub.setParentTaskId(parentTaskId);
        sub.setTitle(title);
        sub.setDescription(description);
        sub.setAssigneeId(assigneeId);
        sub.setStartDate(startDate);
        sub.setDueDate(dueDate);
        sub.setStatus(subtaskStatus);
        sub.setPriority("MEDIUM");
        sub.setProgressPct(0.0);

        long newId = taskDAO.create(sub);

        // If parent was DONE and now has a new child, ensure it's no longer marked DONE
        if ("DONE".equalsIgnoreCase(parent.getStatus())) {
            taskDAO.updateStatus(parentTaskId, "IN_PROGRESS");
        }

        return newId;
    }

    /**
     * Enhanced subtask creation with full validation (recommended for UI controllers)
     */
    public long createSubtaskWithValidation(long parentTaskId, String title, String description,
                                            Long assigneeId, LocalDate startDate, LocalDate dueDate,
                                            long actingUserId, String actingGlobalRole) throws SQLException {
        // Full validation first
        validateSubtaskCreation(parentTaskId);

        // Then use existing enhanced creation method
        return createSubtaskWithDates(parentTaskId, title, description, assigneeId,
                startDate, dueDate, actingUserId, actingGlobalRole);
    }

    // ==============================================================================
    // Core CRUD Operations (preserved from original)
    // ==============================================================================

    public long create(Task t) throws SQLException {
        return taskDAO.create(t);
    }

    public void update(Task t) throws SQLException {
        taskDAO.update(t);
    }

    public void delete(long taskId) throws SQLException {
        taskDAO.delete(taskId);
    }

    public Task get(long id) throws SQLException {
        return taskDAO.findById(id).orElseThrow(() -> new SQLException("Task not found"));
    }

    // ==============================================================================
    // Query Methods (preserved from original)
    // ==============================================================================

    public List<Task> projectTasks(long projectId) throws SQLException {
        return taskDAO.findByProject(projectId);
    }

    public List<Task> children(long parentId) throws SQLException {
        return taskDAO.findChildren(parentId);
    }

    public List<Task> blocked(long projectId) throws SQLException {
        return taskDAO.findBlocked(projectId);
    }

    public List<Task> findByProject(long projectId) throws SQLException {
        return taskDAO.findByProject(projectId);
    }

    // ==============================================================================
    // Dependencies Management (preserved from original)
    // ==============================================================================

    public boolean dependenciesSatisfied(long taskId) throws SQLException {
        if (depDAO == null) return true;
        return !depDAO.hasUnfinishedPredecessor(taskId);
    }

    // ==============================================================================
    // Task Status Management (enhanced with project status integration)
    // ==============================================================================

    public void start(long taskId) throws SQLException {
        if (!dependenciesSatisfied(taskId)) {
            throw new SQLException("Task has unfinished dependencies");
        }
        taskDAO.updateStatus(taskId, "IN_PROGRESS");
    }

    public void complete(long taskId) throws SQLException {
        if (!dependenciesSatisfied(taskId)) {
            throw new SQLException("Task has unfinished dependencies");
        }
        taskDAO.setProgress(taskId, 100.0);
        taskDAO.updateStatus(taskId, "DONE");

        // Auto-update project status if all tasks are complete
        Task task = get(taskId);
        if (projectService != null) {
            projectService.updateProjectStatusAutomatically(task.getProjectId());
        }
    }

    public void setProgress(long taskId, double pct) throws SQLException {
        if (pct < 0 || pct > 100) {
            throw new SQLException("Progress must be 0..100");
        }
        taskDAO.setProgress(taskId, pct);
        if (pct == 100.0) {
            taskDAO.updateStatus(taskId, "DONE");

            // Auto-update project status if all tasks are complete
            Task task = get(taskId);
            if (projectService != null) {
                projectService.updateProjectStatusAutomatically(task.getProjectId());
            }
        }
    }

    // ==============================================================================
    // Task Assignment (preserved from original)
    // ==============================================================================

    public void assignTask(long taskId, long assigneeId, long actingUserId, String actingRole) throws SQLException {
        Task t = get(taskId);
        if (!pmDAO.isMember(t.getProjectId(), assigneeId)) {
            throw new SQLException("Assignee must be a member of this project.");
        }
        boolean isAdmin = "ADMIN".equalsIgnoreCase(actingRole);
        boolean isManager = "MANAGER".equalsIgnoreCase(actingRole);
        if (!isAdmin && !isManager) {
            throw new SQLException("Only Admin or Manager can assign tasks.");
        }
        taskDAO.assignTo(taskId, assigneeId);
    }

    // ==============================================================================
    // Subtasks Management (original methods with enhanced validation)
    // ==============================================================================

    /**
     * Original subtask creation method - enhanced with project status validation
     */
    public long createSubtask(long parentTaskId, String title, String description,
                              Long assigneeId, long actingUserId, String actingGlobalRole) throws SQLException {
        // Use the enhanced method with null dates for backward compatibility
        return createSubtaskWithDates(parentTaskId, title, description, assigneeId,
                null, null, actingUserId, actingGlobalRole);
    }

    // ==============================================================================
    // Authentication and Authorization (preserved from original)
    // ==============================================================================

    private static String roleOf(User u) {
        return u == null || u.getGlobalRole() == null ? "" : u.getGlobalRole().trim().toUpperCase(Locale.ROOT);
    }

    private String findTaskCreator(long taskId) throws SQLException {
        if (activityLogDAO == null)
            throw new SQLException("ActivityLogDAO not configured in TaskService");
        return activityLogDAO.findTaskCreator(taskId).orElse(null);
    }

    /**
     * Role rules:
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

    /**
     * Use this instead of plain delete(taskId) when called from the UI.
     */
    public void deleteTaskWithAuth(long taskId, User user) throws SQLException {
        if (!canDeleteTask(taskId, user))
            throw new SecurityException("You are not allowed to delete this task.");
        taskDAO.delete(taskId);
    }

    // ==============================================================================
    // Deprecated Methods (preserved for backward compatibility)
    // ==============================================================================

    @Deprecated
    public void createTask(long projectId, String title, String description, long assigneeId, String actingRole) throws SQLException {
        // Check if assignee is a project member
        if (!pmDAO.isMember(projectId, assigneeId)) {
            throw new SQLException("Assignee must be a member of this project.");
        }

        // Use the enhanced method
        createTaskWithProjectStatus(projectId, title, description, assigneeId, actingRole);
    }
}
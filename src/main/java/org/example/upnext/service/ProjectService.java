package org.example.upnext.service;

import org.example.upnext.dao.ProjectDAO;
import org.example.upnext.dao.TaskDAO;
import org.example.upnext.dao.ProjectMemberDAO;
import org.example.upnext.dao.impl.ProjectMemberDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
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

    // ==============================================================================
    // Core CRUD Operations
    // ==============================================================================

    public long create(Project project) throws SQLException {
        // Auto-determine initial status based on start date
        if (project.getStatus() == null) {
            project.setStatus(determineInitialStatus(project));
        }
        return projectDAO.create(project);
    }

    public void update(Project project) throws SQLException {
        // Validate status transition before updating
        validateStatusTransition(project);
        projectDAO.update(project);

        // Update all tasks based on new project status
        updateTasksBasedOnProjectStatus(project.getProjectId(), project.getStatus());
    }

    public void delete(long projectId) throws SQLException {
        projectDAO.delete(projectId);
    }

    // ==============================================================================
    // Enhanced Status Management Logic with Date Consideration
    // ==============================================================================

    /**
     * Determines the initial status for a new project based on start date
     */
    private String determineInitialStatus(Project project) {
        LocalDate today = LocalDate.now();

        if (project.getStartDate() != null && today.isBefore(project.getStartDate())) {
            return "PLANNING";
        }
        return "ACTIVE";
    }

    /**
     * Gets the appropriate status for new tasks based on project status AND task dates
     */
    public String getNewTaskStatusForProject(long projectId, LocalDate taskStartDate, LocalDate taskDueDate) throws SQLException {
        Project project = projectDAO.findById(projectId)
                .orElseThrow(() -> new SQLException("Project not found"));

        LocalDate today = LocalDate.now();

        switch (project.getStatus()) {
            case "PLANNING":
                return "TODO"; // All tasks in planning are TODO

            case "ACTIVE":
                // In active projects, determine status based on task dates
                return determineTaskStatusFromDates(taskStartDate, taskDueDate, today);

            case "ON_HOLD":
                return "ON_HOLD";

            case "COMPLETED":
                throw new SQLException("Cannot create new tasks in a COMPLETED project");

            case "CANCELLED":
                throw new SQLException("Cannot create new tasks in a CANCELLED project");

            default:
                return "TODO";
        }
    }

    /**
     * Determines task status based on task dates relative to current date
     */
    private String determineTaskStatusFromDates(LocalDate taskStartDate, LocalDate taskDueDate, LocalDate today) {
        // If task has a start date
        if (taskStartDate != null) {
            if (today.isBefore(taskStartDate)) {
                // Task is scheduled for the future
                return "TODO";
            } else if (taskDueDate != null && today.isAfter(taskDueDate)) {
                // Task is overdue - but still set as IN_PROGRESS for user to handle
                return "IN_PROGRESS";
            } else {
                // Task start date has arrived (today >= start date)
                return "IN_PROGRESS";
            }
        }

        // If no start date but has due date
        if (taskDueDate != null) {
            if (today.isAfter(taskDueDate)) {
                // Overdue task without start date
                return "IN_PROGRESS";
            } else {
                // Due date in future, no start date specified - assume it can start now
                return "TODO";
            }
        }

        // No dates specified - default to TODO in active projects
        return "TODO";
    }

    // Overloaded method for backward compatibility (when no dates provided)
    public String getNewTaskStatusForProject(long projectId) throws SQLException {
        return getNewTaskStatusForProject(projectId, null, null);
    }

    /**
     * Updates project status automatically based on current date and conditions
     */
    public void updateProjectStatusAutomatically(long projectId) throws SQLException {
        Optional<Project> projectOpt = projectDAO.findById(projectId);
        if (projectOpt.isEmpty()) return;

        Project project = projectOpt.get();
        String currentStatus = project.getStatus();
        String newStatus = calculateAutoStatus(project);

        if (!newStatus.equals(currentStatus)) {
            project.setStatus(newStatus);
            projectDAO.updateStatus(projectId, newStatus);
            updateTasksBasedOnProjectStatus(projectId, newStatus);
        }
    }

    /**
     * Calculates what the project status should be automatically
     */
    private String calculateAutoStatus(Project project) throws SQLException {
        LocalDate today = LocalDate.now();
        String currentStatus = project.getStatus();

        // Don't auto-change manual statuses (ON_HOLD, CANCELLED)
        if ("ON_HOLD".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            return currentStatus;
        }

        // If project was manually completed, keep it
        if ("COMPLETED".equals(currentStatus)) {
            return currentStatus;
        }

        // Check if all tasks are completed (auto-complete project)
        List<Task> tasks = taskDAO.findByProject(project.getProjectId());
        if (!tasks.isEmpty()) {
            boolean allCompleted = tasks.stream().allMatch(t -> "DONE".equals(t.getStatus()));
            if (allCompleted) {
                return "COMPLETED";
            }
        }

        // Auto-transition from PLANNING to ACTIVE based on start date
        if (project.getStartDate() != null) {
            if (today.isBefore(project.getStartDate())) {
                return "PLANNING";
            } else if ("PLANNING".equals(currentStatus)) {
                return "ACTIVE";
            }
        }

        return currentStatus;
    }

    /**
     * Validates status transitions and prevents invalid changes
     */
    public void validateStatusTransition(Project project) throws SQLException {
        Optional<Project> currentOpt = projectDAO.findById(project.getProjectId());
        if (currentOpt.isEmpty()) return;

        Project current = currentOpt.get();
        String fromStatus = current.getStatus();
        String toStatus = project.getStatus();

        if (fromStatus.equals(toStatus)) return; // No change

        // Special validation for COMPLETED status
        if ("COMPLETED".equals(toStatus)) {
            List<Task> incompleteTasks = getIncompleteTasks(project.getProjectId());
            if (!incompleteTasks.isEmpty()) {
                throw new SQLException(
                        "Cannot mark project as COMPLETED. " + incompleteTasks.size() +
                                " task(s) are still incomplete. Complete all tasks first or mark them as CANCELLED."
                );
            }
        }

        // Validate other transitions (add more rules as needed)
        if ("CANCELLED".equals(fromStatus) && !"CANCELLED".equals(toStatus)) {
            throw new SQLException("Cannot change status from CANCELLED to " + toStatus);
        }
    }

    /**
     * Updates all tasks in a project based on the project's new status
     */
    private void updateTasksBasedOnProjectStatus(long projectId, String projectStatus) throws SQLException {
        List<Task> tasks = taskDAO.findByProject(projectId);

        for (Task task : tasks) {
            String newTaskStatus = determineTaskStatusFromProject(task.getStatus(), projectStatus, task.getStartDate(), task.getDueDate());
            if (!newTaskStatus.equals(task.getStatus())) {
                taskDAO.updateStatus(task.getTaskId(), newTaskStatus);
            }
        }
    }

    /**
     * Enhanced method that considers both project status and task dates
     */
    public String determineTaskStatusFromProject(String currentTaskStatus, String projectStatus,
                                                 LocalDate taskStartDate, LocalDate taskDueDate) {
        // Don't change already completed or cancelled tasks
        if ("DONE".equals(currentTaskStatus) || "CANCELLED".equals(currentTaskStatus)) {
            return currentTaskStatus;
        }

        LocalDate today = LocalDate.now();

        switch (projectStatus) {
            case "PLANNING":
                return "TODO";

            case "ACTIVE":
                // In active projects, use date-based logic
                return determineTaskStatusFromDates(taskStartDate, taskDueDate, today);

            case "ON_HOLD":
                return "ON_HOLD";

            case "CANCELLED":
                return "BLOCKED";

            case "COMPLETED":
                return currentTaskStatus;

            default:
                return currentTaskStatus;
        }
    }

    /**
     * Checks if new tasks can be created in this project
     */
    public boolean canCreateTasksInProject(long projectId) throws SQLException {
        Project project = projectDAO.findById(projectId)
                .orElseThrow(() -> new SQLException("Project not found"));

        return !"COMPLETED".equals(project.getStatus()) && !"CANCELLED".equals(project.getStatus());
    }

    /**
     * Gets all incomplete tasks in a project
     */
    public List<Task> getIncompleteTasks(long projectId) throws SQLException {
        return taskDAO.findByProject(projectId).stream()
                .filter(task -> !"DONE".equals(task.getStatus()) && !"CANCELLED".equals(task.getStatus()))
                .toList();
    }

    /**
     * Updates all project statuses automatically (call periodically)
     */
    public void updateAllProjectStatuses() throws SQLException {
        List<Project> allProjects = projectDAO.findAll();
        for (Project project : allProjects) {
            updateProjectStatusAutomatically(project.getProjectId());
        }
    }

    // ==============================================================================
    // Existing Methods (preserved for compatibility)
    // ==============================================================================

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

    public double calculateOverallProgress(long projectId) throws SQLException {
        List<Task> tasks = taskDAO.findByProject(projectId);
        if (tasks.isEmpty()) return 0.0;
        double sum = tasks.stream().mapToDouble(Task::getProgressPct).sum();
        return Math.round((sum / tasks.size()) * 100.0) / 100.0;
    }

    private static String roleOf(User u) {
        return u == null || u.getGlobalRole() == null ? "" : u.getGlobalRole().trim().toUpperCase();
    }

    public boolean canDeleteProject(long projectId, User user) throws SQLException {
        if (!"ADMIN".equals(roleOf(user))) return false;
        var opt = projectDAO.findById(projectId);
        if (opt.isEmpty()) return false;
        Project p = opt.get();
        return p.getOwnerId() != null && p.getOwnerId().equals(user.getUserId());
    }

    public void deleteProjectWithAuth(long projectId, User user) throws SQLException {
        if (!canDeleteProject(projectId, user)) {
            throw new SecurityException("You are not allowed to delete this project.");
        }
        projectDAO.delete(projectId);
    }

    public boolean isManagerOfProject(long projectId, long userId) throws SQLException {
        return pmDAO.hasRole(projectId, userId, "MANAGER");
    }

    // ==============================================================================
    // Deprecated methods for backward compatibility
    // ==============================================================================

    @Deprecated
    public void checkAndUpdateProjectStatus(long projectId) throws SQLException {
        updateProjectStatusAutomatically(projectId);
    }

    @Deprecated
    public void saveProject(Project project) throws SQLException {
        update(project);
    }
}
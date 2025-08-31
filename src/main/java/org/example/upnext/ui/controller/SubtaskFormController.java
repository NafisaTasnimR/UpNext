package org.example.upnext.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.upnext.dao.impl.ProjectDAOImpl;
import org.example.upnext.dao.impl.TaskDAOImpl;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;
import org.example.upnext.service.TaskService;
import org.example.upnext.service.ProjectService;
import org.example.upnext.model.Project;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class SubtaskFormController {
    @FXML private Label parentLabel, statusLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descArea;
    @FXML private ComboBox<String> priorityBox;  // Priority dropdown

    // Optional date fields if you want to keep them
    @FXML private DatePicker startPicker;
    @FXML private DatePicker duePicker;
    @FXML private ComboBox<User> assigneeBox;

    private TaskService taskService;
    private ProjectService projectService;
    private Task parent;
    private User actingUser;
    private List<User> projectMembers;

    @FXML
    public void initialize() {
        // Initialize services with proper wiring
        taskService = new TaskService(new TaskDAOImpl(), new org.example.upnext.dao.impl.TaskDependencyDAOImpl());
        projectService = new ProjectService(new ProjectDAOImpl(), new TaskDAOImpl());
        taskService.setProjectService(projectService);
        taskService.setProjectDAO(new ProjectDAOImpl());

        // Initialize priority box with all priority options
        List<String> priorities = Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL");
        priorityBox.getItems().setAll(priorities);
        priorityBox.getSelectionModel().select("MEDIUM"); // Default to MEDIUM

        // Initialize assignee box if it exists
        if (assigneeBox != null) {
            assigneeBox.setCellFactory(list -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? null : user.getUsername() + " (" + user.getEmail() + ")");
                }
            });
            assigneeBox.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? "Select Assignee" : user.getUsername());
                }
            });
        }
    }

    public void init(Task parentTask, User actingUser, List<User> projectMembers) {
        this.parent = parentTask;
        this.actingUser = actingUser;
        this.projectMembers = projectMembers;

        parentLabel.setText("Parent: " + parentTask.getTitle());

        // Populate assignee box if it exists
        if (assigneeBox != null && projectMembers != null) {
            assigneeBox.getItems().clear();
            assigneeBox.getItems().add(null); // Option for "unassigned"
            assigneeBox.getItems().addAll(projectMembers);
        }

        // Pre-validate and show status info
        try {
            taskService.validateSubtaskCreation(parentTask.getTaskId());
            updateStatusInfo();
        } catch (SQLException e) {
            // If validation fails, show error immediately and disable save
            showValidationError("Cannot Create Subtask", e.getMessage());
        }
    }

    private void updateStatusInfo() {
        try {
            if (projectService != null && parent != null) {
                LocalDate startDate = startPicker != null ? startPicker.getValue() : null;
                LocalDate dueDate = duePicker != null ? duePicker.getValue() : null;

                String subtaskStatus = projectService.getNewTaskStatusForProject(
                        parent.getProjectId(), startDate, dueDate);

                statusLabel.setText("Subtask status will be: " + subtaskStatus +
                        " | Priority: " + priorityBox.getValue() +
                        " (based on project status and dates)");
                statusLabel.setStyle("-fx-text-fill: #2E8B57;"); // Green text for good status
            }
        } catch (SQLException e) {
            statusLabel.setText("Could not determine subtask status: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #B22222;"); // Red text for errors
        }
    }

    @FXML
    private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            showValidationError("Validation Error", "Subtask title is required.");
            return;
        }

        String desc = descArea.getText();
        String priority = priorityBox.getValue(); // Get selected priority
        LocalDate startDate = startPicker != null ? startPicker.getValue() : null;
        LocalDate dueDate = duePicker != null ? duePicker.getValue() : null;
        User selectedAssignee = assigneeBox != null ? assigneeBox.getValue() : null;
        Long assigneeId = selectedAssignee != null ? selectedAssignee.getUserId() : null;

        try {
            // Create the subtask with the enhanced method
            long subtaskId = createSubtaskWithPriority(
                    parent.getTaskId(),
                    title,
                    desc,
                    priority,
                    assigneeId,
                    startDate,
                    dueDate,
                    actingUser.getUserId(),
                    actingUser.getGlobalRole()
            );

            showSuccessMessage("Subtask Created",
                    "Subtask '" + title + "' created successfully with ID: " + subtaskId +
                            "\nPriority: " + priority);
            close();

        } catch (SQLException e) {
            showValidationError("Cannot Create Subtask", e.getMessage());
        } catch (Exception e) {
            showValidationError("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Enhanced method to create subtask with priority
     */
    private long createSubtaskWithPriority(long parentTaskId, String title, String description,
                                           String priority, Long assigneeId, LocalDate startDate,
                                           LocalDate dueDate, long actingUserId, String actingGlobalRole)
            throws SQLException {

        // Get the parent task using taskService
        Task parent = taskService.get(parentTaskId);

        // Check if tasks can be created in this project using projectService
        if (projectService != null) {
            try {
                // Use the correct method signature - pass projectId (long) not Project object
                if (!projectService.canCreateTasksInProject(parent.getProjectId())) {
                    // Get project info for better error message
                    // Note: You may need to add a getProject method to ProjectService if it doesn't exist
                    throw new SQLException("Cannot create new subtasks in this project due to its status.");
                }
            } catch (Exception e) {
                throw new SQLException("Error checking project status: " + e.getMessage());
            }
        }

        // Check permissions
        boolean isAdmin = "ADMIN".equalsIgnoreCase(actingGlobalRole);
        boolean isManager = false;
        boolean isParentAssignee = parent.getAssigneeId() != null && parent.getAssigneeId().equals(actingUserId);

        // Use projectService to check manager role
        if (projectService != null) {
            try {
                isManager = projectService.isManagerOfProject(parent.getProjectId(), actingUserId);
            } catch (SQLException e) {
                System.err.println("Warning: Could not check manager status: " + e.getMessage());
            }
        }

        if (!isAdmin && !isManager && !isParentAssignee) {
            throw new SQLException("Only Admin/Manager or the parent task's assignee can create subtasks.");
        }

        // Check assignee membership - use ProjectMemberDAO directly if needed, or check through project members list
        if (assigneeId != null && projectMembers != null) {
            boolean isMember = false;
            for (User member : projectMembers) {
                if (member.getUserId() == assigneeId) {
                    isMember = true;
                    break;
                }
            }
            if (!isMember) {
                throw new SQLException("Assignee must be a member of the project.");
            }
        }

        // Determine subtask status based on project status AND dates
        String subtaskStatus = "TODO"; // Default
        if (projectService != null) {
            try {
                subtaskStatus = projectService.getNewTaskStatusForProject(
                        parent.getProjectId(), startDate, dueDate);
            } catch (SQLException e) {
                System.err.println("Warning: Could not determine status from project: " + e.getMessage());
            }
        }

        // Create the task object
        Task sub = new Task();
        sub.setProjectId(parent.getProjectId());
        sub.setParentTaskId(parentTaskId);
        sub.setTitle(title);
        sub.setDescription(description);
        sub.setAssigneeId(assigneeId);
        sub.setStartDate(startDate);
        sub.setDueDate(dueDate);
        sub.setStatus(subtaskStatus);
        sub.setPriority(priority); // Set the selected priority
        sub.setProgressPct(0.0);

        // Use taskService to create the subtask
        long newId = taskService.create(sub);

        // If parent was DONE and now has a new child, update parent status using taskDAO directly
        if ("DONE".equalsIgnoreCase(parent.getStatus())) {
            try {
                // Use taskDAO to update status since taskService.updateStatus might not exist
                TaskDAOImpl taskDAO = new TaskDAOImpl();
                taskDAO.updateStatus(parentTaskId, "IN_PROGRESS");
            } catch (SQLException e) {
                System.err.println("Warning: Could not update parent task status: " + e.getMessage());
            }
        }

        return newId;
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

    // Helper Methods for User-Friendly Dialogs
    private void showValidationError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(450);
        alert.getDialogPane().setPrefWidth(450);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void showSuccessMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(350);
        alert.showAndWait();
    }

    // Update status info when priority changes
    @FXML
    private void onPriorityChanged() {
        updateStatusInfo();
    }

    @FXML
    private void onStartDateChanged() {
        updateStatusInfo();
    }

    @FXML
    private void onDueDateChanged() {
        updateStatusInfo();
    }
}
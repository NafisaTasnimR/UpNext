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

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class SubtaskFormController {
    @FXML private Label parentLabel, statusLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startPicker;        // Add these to your FXML if not present
    @FXML private DatePicker duePicker;          // Add these to your FXML if not present
    @FXML private ComboBox<User> assigneeBox;    // Add these to your FXML if not present
    @FXML private ComboBox<String> priorityBox;  // Add these to your FXML if not present

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

        // Initialize priority box if it exists
        if (priorityBox != null) {
            List<String> priorities = Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL");
            priorityBox.getItems().setAll(priorities);
            priorityBox.getSelectionModel().select("MEDIUM");
        }

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
        LocalDate startDate = startPicker != null ? startPicker.getValue() : null;
        LocalDate dueDate = duePicker != null ? duePicker.getValue() : null;
        User selectedAssignee = assigneeBox != null ? assigneeBox.getValue() : null;
        Long assigneeId = selectedAssignee != null ? selectedAssignee.getUserId() : null;

        try {
            // Use the enhanced method with full validation and date support
            long subtaskId = taskService.createSubtaskWithValidation(
                    parent.getTaskId(),
                    title,
                    desc,
                    assigneeId,
                    startDate,
                    dueDate,
                    actingUser.getUserId(),
                    actingUser.getGlobalRole()
            );

            showSuccessMessage("Subtask Created",
                    "Subtask '" + title + "' created successfully with ID: " + subtaskId);
            close();

        } catch (SQLException e) {
            // Show detailed warning dialog for validation errors
            showValidationError("Cannot Create Subtask", e.getMessage());
        } catch (Exception e) {
            showValidationError("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

    // ==============================================================================
    // Helper Methods for User-Friendly Dialogs
    // ==============================================================================

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


    @FXML
    private void onStartDateChanged() {
        updateStatusInfo();
    }

    @FXML
    private void onDueDateChanged() {
        updateStatusInfo();
    }
}
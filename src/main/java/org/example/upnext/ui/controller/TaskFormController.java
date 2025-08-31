package org.example.upnext.ui.controller;

import org.example.upnext.dao.impl.TaskDAOImpl;
import org.example.upnext.dao.impl.ProjectDAOImpl;
import org.example.upnext.model.Task;
import org.example.upnext.service.TaskService;
import org.example.upnext.service.ProjectService;
import org.example.upnext.model.Project;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongConsumer;

public class TaskFormController {
    @FXML private TextField titleField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startPicker;
    @FXML private DatePicker duePicker;
    @FXML private ComboBox<String> priorityBox;
    @FXML private ComboBox<String> statusBox;
    @FXML private TextField parentIdField;
    @FXML private Label statusInfoLabel; // Add this to your FXML

    private TaskService taskService;
    private ProjectService projectService;
    private Long projectId;
    private LongConsumer onSaved;

    @FXML
    public void initialize() {
        // Initialize services with proper wiring
        taskService = new TaskService(new TaskDAOImpl(), new org.example.upnext.dao.impl.TaskDependencyDAOImpl());
        projectService = new ProjectService(new ProjectDAOImpl(), new TaskDAOImpl());
        taskService.setProjectService(projectService);
        taskService.setProjectDAO(new ProjectDAOImpl());

        List<String> priorities = Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL");
        priorityBox.getItems().setAll(priorities);
        priorityBox.getSelectionModel().select("MEDIUM");

        List<String> statuses = Arrays.asList("TODO", "IN_PROGRESS", "BLOCKED", "ON_HOLD", "DONE", "CANCELLED");
        statusBox.getItems().setAll(statuses);
    }

    public void initForCreate(Long projectId, LongConsumer onSaved) {
        this.projectId = projectId;
        this.onSaved = onSaved;

        startPicker.setOnAction(e -> updateStatusInfo());
        duePicker.setOnAction(e -> updateStatusInfo());

        try {
            // Get project info to show status context
            Project project = projectService.byId(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Show what status the task will have
            String taskStatus = projectService.getNewTaskStatusForProject(projectId);
            statusBox.getSelectionModel().select(taskStatus);
            statusBox.setDisable(true); // Status determined by project

            if (statusInfoLabel != null) {
                statusInfoLabel.setText("Task status will be: " + taskStatus +
                        " (based on project status: " + project.getStatus() + ")");
            }

        } catch (Exception e) {
            if (statusInfoLabel != null) {
                statusInfoLabel.setText("Warning: Could not determine task status - " + e.getMessage());
            }
        }
        updateStatusInfo();
    }

    @FXML
    public void onSave() {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                showError("Task title is required");
                return;
            }

            String description = descArea.getText();
            LocalDate startDate = startPicker.getValue();  // Get the dates from form
            LocalDate dueDate = duePicker.getValue();
            Long assigneeId = null; // TODO: Add assignee selection to the form

            // Use the enhanced method that considers dates
            long id = taskService.createTaskWithProjectStatus(
                    projectId, title, description, assigneeId, startDate, dueDate, "ADMIN"
            );

            if (onSaved != null) onSaved.accept(id);
            close();

        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }


    private void updateStatusInfo() {
        try {
            Project project = projectService.byId(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            LocalDate startDate = startPicker.getValue();
            LocalDate dueDate = duePicker.getValue();

            // Get what status the task will have based on dates
            String taskStatus = projectService.getNewTaskStatusForProject(projectId, startDate, dueDate);
            statusBox.getSelectionModel().select(taskStatus);
            statusBox.setDisable(true); // Status determined automatically

            // Update info label with explanation
            if (statusInfoLabel != null) {
                String explanation = getStatusExplanation(project.getStatus(), taskStatus, startDate, dueDate);
                statusInfoLabel.setText(explanation);
            }

        } catch (Exception e) {
            if (statusInfoLabel != null) {
                statusInfoLabel.setText("Warning: Could not determine task status - " + e.getMessage());
            }
        }
    }

    private String getStatusExplanation(String projectStatus, String taskStatus, LocalDate startDate, LocalDate dueDate) {
        LocalDate today = LocalDate.now();

        if ("ACTIVE".equals(projectStatus)) {
            if ("IN_PROGRESS".equals(taskStatus)) {
                if (startDate != null && !today.isBefore(startDate)) {
                    return "Status: IN_PROGRESS (start date has arrived in active project)";
                } else {
                    return "Status: IN_PROGRESS (ready to start in active project)";
                }
            } else if ("TODO".equals(taskStatus)) {
                if (startDate != null && today.isBefore(startDate)) {
                    return "Status: TODO (start date is " + startDate + " - in the future)";
                } else {
                    return "Status: TODO (no start date specified in active project)";
                }
            }
        }

        return "Status: " + taskStatus + " (based on project status: " + projectStatus + ")";
    }


    @FXML
    public void onCancel() {
        close();
    }

    private void close() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}

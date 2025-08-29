package org.example.upnext.ui.controller;

import org.example.upnext.dao.impl.TaskDAOImpl;
import org.example.upnext.model.Task;
import org.example.upnext.service.TaskService;
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

    private final TaskService taskService = new TaskService(new TaskDAOImpl(), new org.example.upnext.dao.impl.TaskDependencyDAOImpl());
    private Long projectId;
    private LongConsumer onSaved;

    @FXML
    public void initialize() {
        List<String> priorities = Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL");
        priorityBox.getItems().setAll(priorities);
        priorityBox.getSelectionModel().select("MEDIUM");

        List<String> statuses = Arrays.asList("TODO", "IN_PROGRESS", "BLOCKED", "ON_HOLD", "DONE", "CANCELLED");
        statusBox.getItems().setAll(statuses);
        statusBox.getSelectionModel().select("TODO");
    }

    public void initForCreate(Long projectId, LongConsumer onSaved) {
        this.projectId = projectId;
        this.onSaved = onSaved;
    }

    @FXML
    public void onSave() {
        try {
            Task t = new Task(projectId, titleField.getText().trim());
            t.setDescription(descArea.getText());
            t.setPriority(priorityBox.getValue());
            t.setStatus(statusBox.getValue());
            t.setStartDate(startPicker.getValue());
            t.setDueDate(duePicker.getValue());
            String parent = parentIdField.getText().trim();
            if (!parent.isEmpty()) t.setParentTaskId(Long.parseLong(parent));
            long id = taskService.create(t);
            if (onSaved != null) onSaved.accept(id);
            close();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML public void onCancel() { close(); }
    private void close() { ((Stage) titleField.getScene().getWindow()).close(); }
}

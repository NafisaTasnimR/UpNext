package org.example.upnext.ui.controller;

import org.example.upnext.dao.impl.ProjectDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.User;
import org.example.upnext.service.ProjectService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ProjectFormController {
    @FXML private TextField nameField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startPicker;
    @FXML private DatePicker endPicker;
    @FXML private ComboBox<String> statusBox;

    private final ProjectService projectService = new ProjectService(new ProjectDAOImpl(), new org.example.upnext.dao.impl.TaskDAOImpl());
    private User currentUser;
    private Project editing;
    private Consumer<Project> onSaved;

    @FXML
    public void initialize() {
        List<String> statuses = Arrays.asList("PLANNING", "ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED");
        statusBox.getItems().setAll(statuses);
        statusBox.getSelectionModel().select("PLANNING");
    }

    public void initForCreate(User user, Consumer<Project> onSaved) {
        this.currentUser = user;
        this.onSaved = onSaved;
    }

    public void initForEdit(User user, Project p, Consumer<Project> onSaved) {
        this.currentUser = user;
        this.onSaved = onSaved;
        this.editing = p;
        nameField.setText(p.getName());
        descArea.setText(p.getDescription());
        startPicker.setValue(p.getStartDate());
        endPicker.setValue(p.getEndDate());
        statusBox.getSelectionModel().select(p.getStatus());
    }

    @FXML
    public void onSave() {
        try {
            if (editing == null) {
                Project p = new Project();
                p.setName(nameField.getText().trim());
                p.setDescription(descArea.getText());
                p.setOwnerId(currentUser.getUserId());
                p.setStartDate(startPicker.getValue());
                p.setEndDate(endPicker.getValue());
                p.setStatus(statusBox.getValue());
                long id = projectService.create(p);
                p.setProjectId(id);
                if (onSaved != null) onSaved.accept(p);
            } else {
                editing.setName(nameField.getText().trim());
                editing.setDescription(descArea.getText());
                editing.setStartDate(startPicker.getValue());
                editing.setEndDate(endPicker.getValue());
                editing.setStatus(statusBox.getValue());
                projectService.update(editing);
                if (onSaved != null) onSaved.accept(editing);
            }
            close();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    public void onCancel() { close(); }

    private void close() { ((Stage) nameField.getScene().getWindow()).close(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}

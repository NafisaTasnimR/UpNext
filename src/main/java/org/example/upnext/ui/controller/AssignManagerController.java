package org.example.upnext.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.upnext.dao.impl.UserDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.User;
import org.example.upnext.service.ProjectService;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

public class AssignManagerController {
    @FXML private Label projectLabel;
    @FXML private ComboBox<User> managerBox;
    @FXML private Label statusLabel;

    private final UserDAOImpl userDAO = new UserDAOImpl();
    private final ProjectService projectService =
            new ProjectService(new org.example.upnext.dao.impl.ProjectDAOImpl(),
                    new org.example.upnext.dao.impl.TaskDAOImpl());

    private Project project;
    private BiConsumer<Project, User> onAssigned;

    @FXML
    public void initialize() {
        managerBox.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getUsername() + " (" + u.getEmail() + ")");
            }
        });
        managerBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getUsername());
            }
        });
    }

    public void init(Project p, BiConsumer<Project, User> onAssigned) {
        this.project = p;
        this.onAssigned = onAssigned;
        projectLabel.setText("Project: " + p.getName());
        try {
            List<User> managers = userDAO.findManagers();
            managerBox.getItems().setAll(managers);
        } catch (SQLException e) {
            statusLabel.setText("Load managers failed: " + e.getMessage());
        }
    }

    @FXML
    public void onAssign() {
        User selected = managerBox.getValue();
        if (selected == null) { statusLabel.setText("Choose a manager"); return; }
        try {
            projectService.assignManager(project.getProjectId(), selected.getUserId());
            if (onAssigned != null) onAssigned.accept(project, selected);
            close();
        } catch (SQLException e) {
            statusLabel.setText("Assign failed: " + e.getMessage());
        }
    }

    @FXML
    public void onCancel() { close(); }

    private void close() {
        ((Stage) managerBox.getScene().getWindow()).close();
    }
}


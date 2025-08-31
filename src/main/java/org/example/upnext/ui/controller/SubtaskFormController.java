package org.example.upnext.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;
import org.example.upnext.service.TaskService;

import java.sql.SQLException;
import java.util.List;

public class SubtaskFormController {
    @FXML private Label parentLabel, statusLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descArea;

    private final TaskService taskService =
            new TaskService(new org.example.upnext.dao.impl.TaskDAOImpl(), null);

    private Task parent;
    private User actingUser;


    public void init(Task parentTask, User actingUser, List<User> ignoredMembers) {
        this.parent = parentTask;
        this.actingUser = actingUser;
        parentLabel.setText("Parent: " + parentTask.getTitle());
    }

    @FXML
    private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) { statusLabel.setText("Title is required."); return; }
        String desc = descArea.getText();

        try {
            taskService.createSubtask(
                    parent.getTaskId(),
                    title,
                    desc,
                    null, // no assignee
                    actingUser.getUserId(),
                    actingUser.getGlobalRole()
            );
            close();
        } catch (SQLException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    @FXML private void onCancel() { close(); }
    private void close() { ((Stage) titleField.getScene().getWindow()).close(); }
}

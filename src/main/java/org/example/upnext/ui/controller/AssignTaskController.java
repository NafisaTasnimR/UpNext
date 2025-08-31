package org.example.upnext.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.upnext.dao.impl.UserDAOImpl;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;
import org.example.upnext.service.TaskService;

import java.util.List;
import java.util.function.Consumer;

public class AssignTaskController {
    @FXML private Label taskLabel, statusLabel;
    @FXML private ComboBox<User> memberBox;

    private final TaskService taskService =
            new TaskService(new org.example.upnext.dao.impl.TaskDAOImpl(),
                    new org.example.upnext.dao.impl.TaskDependencyDAOImpl());

    private Task task;
    private Long projectId;
    private User actingUser;
    private Consumer<User> onAssigned;

    public void init(Task t, Long projectId, User actingUser, List<User> projectMembers, Consumer<User> onAssigned) {
        this.task = t; this.projectId = projectId; this.actingUser = actingUser; this.onAssigned = onAssigned;
        taskLabel.setText("Task: " + t.getTitle());
        memberBox.getItems().setAll(projectMembers);
        memberBox.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty); setText(empty || u == null ? null : u.getUsername() + " (" + u.getEmail() + ")");
            }
        });
        memberBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty); setText(empty || u == null ? null : u.getUsername());
            }
        });
    }

    @FXML
    public void onAssign() {
        User target = memberBox.getValue();
        if (target == null) { statusLabel.setText("Choose a member"); return; }
        try {
            taskService.assignTask(task.getTaskId(), target.getUserId(), actingUser.getUserId(), actingUser.getGlobalRole());
            if (onAssigned != null) onAssigned.accept(target);
            close();
        } catch (Exception e) { statusLabel.setText(e.getMessage()); }
    }

    @FXML public void onCancel() { close(); }
    private void close() { ((Stage) memberBox.getScene().getWindow()).close(); }
}

package org.example.upnext.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.upnext.dao.impl.UserDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.User;
import org.example.upnext.service.ProjectService;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssignProjectMembersController {
    @FXML private Label projectLabel, statusLabel;
    @FXML private ListView<User> memberList;

    private final UserDAOImpl userDAO = new UserDAOImpl();
    private final ProjectService projectService =
            new ProjectService(new org.example.upnext.dao.impl.ProjectDAOImpl(),
                    new org.example.upnext.dao.impl.TaskDAOImpl());

    private Project project;

    @FXML
    public void initialize() {
        memberList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        memberList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getUsername() + " (" + u.getEmail() + ")");
            }
        });
    }

    public void init(Project p) {
        this.project = p;
        projectLabel.setText("Project: " + p.getName());
        try {
            List<User> allMembers = userDAO.findMembers();
            memberList.getItems().setAll(allMembers);

            Set<Long> existing = new HashSet<>(projectService.memberIds(p.getProjectId()));
            for (int i = 0; i < allMembers.size(); i++) {
                if (existing.contains(allMembers.get(i).getUserId())) {
                    memberList.getSelectionModel().select(i);
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Load members failed: " + e.getMessage());
        }
    }

    @FXML
    public void onSave() {
        try {
            Set<Long> current = new HashSet<>(projectService.memberIds(project.getProjectId()));
            Set<Long> selected = new HashSet<>();
            for (User u : memberList.getSelectionModel().getSelectedItems()) selected.add(u.getUserId());

            for (Long uid : current) if (!selected.contains(uid)) projectService.removeMember(project.getProjectId(), uid);
            for (Long uid : selected) if (!current.contains(uid)) projectService.addMember(project.getProjectId(), uid);

            close();
        } catch (SQLException e) {
            statusLabel.setText("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() { close(); }
    private void close() { ((Stage) memberList.getScene().getWindow()).close(); }
}

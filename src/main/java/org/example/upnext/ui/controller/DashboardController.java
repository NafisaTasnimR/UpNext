package org.example.upnext.ui.controller;

import org.example.upnext.dao.impl.*;
import org.example.upnext.model.Project;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;
import org.example.upnext.service.ProjectService;
import org.example.upnext.service.TaskService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {
    @FXML private TableView<Project> projectTable;
    @FXML private TreeTableView<Task> taskTree;
    @FXML private Label statusLabel;

    private final ProjectService projectService = new ProjectService(new ProjectDAOImpl(), new TaskDAOImpl());
    private final TaskService taskService = new TaskService(new TaskDAOImpl(), new TaskDependencyDAOImpl());
    private User currentUser;

    public void setCurrentUser(User u) {
        this.currentUser = u;
        statusLabel.setText("Logged in as: " + u.getUsername());
        loadProjects();
    }

    @FXML
    public void initialize() {
        // Projects table columns
        TableColumn<Project, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));

        TableColumn<Project, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));

        projectTable.getColumns().setAll(nameCol, statusCol);

        // Task tree columns
        TreeTableColumn<Task, String> titleCol = new TreeTableColumn<>("Title");
        titleCol.setPrefWidth(240);
        titleCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("title"));

        TreeTableColumn<Task, String> stCol = new TreeTableColumn<>("Status");
        stCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue().getValue().getStatus()));

        TreeTableColumn<Task, String> prCol = new TreeTableColumn<>("Priority");
        prCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue().getValue().getPriority()));

        TreeTableColumn<Task, Number> prgCol = new TreeTableColumn<>("Progress");
        prgCol.setCellValueFactory(param -> new javafx.beans.property.SimpleDoubleProperty(
                param.getValue().getValue().getProgressPct()));

        taskTree.getColumns().setAll(titleCol, stCol, prCol, prgCol);
    }

    private void loadProjects() {
        try {
            long ownerId = (currentUser != null) ? currentUser.getUserId() : 1L;
            projectTable.getItems().setAll(projectService.byOwner(ownerId));
        } catch (SQLException e) {
            statusLabel.setText("Failed to load projects: " + e.getMessage());
        }
    }

    @FXML
    public void onOpenProject() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project"); return; }
        loadTasksForProject(p.getProjectId());
    }

    private void loadTasksForProject(long projectId) {
        try {
            List<Task> tasks = taskService.projectTasks(projectId);
            Map<Long, TreeItem<Task>> byId = new HashMap<>();
            TreeItem<Task> root = new TreeItem<>(fakeRoot(projectId));
            root.setExpanded(true);

            // Index by id
            tasks.forEach(t -> byId.put(t.getTaskId(), new TreeItem<>(t)));

            // Build hierarchy
            for (Task t : tasks) {
                TreeItem<Task> item = byId.get(t.getTaskId());
                if (t.getParentTaskId() != null && byId.containsKey(t.getParentTaskId())) {
                    byId.get(t.getParentTaskId()).getChildren().add(item);
                } else {
                    root.getChildren().add(item);
                }
            }
            taskTree.setRoot(root);
            taskTree.setShowRoot(false);
            statusLabel.setText("Loaded " + tasks.size() + " tasks.");
        } catch (SQLException e) {
            statusLabel.setText("Failed to load tasks: " + e.getMessage());
        }
    }

    private Task fakeRoot(long projectId) {
        Task t = new Task();
        t.setProjectId(projectId);
        t.setTitle("ROOT");
        t.setStatus("TODO");
        t.setPriority("LOW");
        return t;
    }

    @FXML
    public void onNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectForm.fxml"));
            Scene scene = new Scene(loader.load());
            ProjectFormController ctrl = loader.getController();
            ctrl.initForCreate(currentUser, saved -> {
                loadProjects();
                statusLabel.setText("Project created: " + saved.getName());
            });
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("New Project");
            dlg.setScene(scene);
            dlg.showAndWait();
        } catch (Exception ex) {
            statusLabel.setText("Open form failed: " + ex.getMessage());
        }
    }

    @FXML
    public void onEditProject() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project to edit"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectForm.fxml"));
            Scene scene = new Scene(loader.load());
            ProjectFormController ctrl = loader.getController();
            ctrl.initForEdit(currentUser, p, updated -> {
                loadProjects();
                statusLabel.setText("Project updated: " + updated.getName());
            });
            Stage dlg = new Stage(); dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Edit Project"); dlg.setScene(scene); dlg.showAndWait();
        } catch (Exception ex) { statusLabel.setText("Edit failed: " + ex.getMessage()); }
    }

    @FXML
    public void onDeleteProject() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project to delete"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete project \"" + p.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { projectService.delete(p.getProjectId()); loadProjects(); taskTree.setRoot(null); }
                catch (SQLException e) { statusLabel.setText("Delete failed: " + e.getMessage()); }
            }
        });
    }

    @FXML
    public void onNewTask() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Open a project first"); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskForm.fxml"));
            Scene scene = new Scene(loader.load());
            TaskFormController ctrl = loader.getController();
            ctrl.initForCreate(p.getProjectId(), savedId -> {
                loadTasksForProject(p.getProjectId());
                statusLabel.setText("Task created: " + savedId);
            });
            Stage dlg = new Stage(); dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("New Task"); dlg.setScene(scene); dlg.showAndWait();
        } catch (Exception ex) { statusLabel.setText("Open task form failed: " + ex.getMessage()); }
    }

    @FXML
    public void onStartTask() {
        Task selected = getSelectedTask();
        if (selected == null) { statusLabel.setText("Select a task"); return; }
        try {
            taskService.start(selected.getTaskId());
            reloadSelectedProjectTasks();
            statusLabel.setText("Task started");
        } catch (SQLException e) { statusLabel.setText(e.getMessage()); }
    }

    @FXML
    public void onCompleteTask() {
        Task selected = getSelectedTask();
        if (selected == null) { statusLabel.setText("Select a task"); return; }
        try {
            taskService.complete(selected.getTaskId());
            reloadSelectedProjectTasks();
            statusLabel.setText("Task completed");
        } catch (SQLException e) { statusLabel.setText(e.getMessage()); }
    }

    @FXML
    public void onRefreshTasks() {
        reloadSelectedProjectTasks();
    }

    @FXML
    public void onLogout() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        try {
            Stage s = (Stage) statusLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            s.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            statusLabel.setText("Logout failed: " + e.getMessage());
        }
    }

    private Task getSelectedTask() {
        TreeItem<Task> item = taskTree.getSelectionModel().getSelectedItem();
        return (item == null || item.getValue() == null || "ROOT".equals(item.getValue().getTitle())) ? null : item.getValue();
    }

    private void reloadSelectedProjectTasks() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p != null) loadTasksForProject(p.getProjectId());
    }
}

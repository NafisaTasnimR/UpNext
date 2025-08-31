package org.example.upnext.ui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TreeItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.upnext.auth.AuthContext;
import org.example.upnext.dao.impl.*;
import org.example.upnext.dao.impl.ProjectDAOImpl;
import org.example.upnext.model.Project;
import org.example.upnext.model.Task;
import org.example.upnext.model.User;
import org.example.upnext.service.NotificationService;
import org.example.upnext.service.ProjectService;
import org.example.upnext.service.TaskService;
import org.example.upnext.service.NotificationService.Role;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.upnext.service.NotificationScheduler;

public class DashboardController {

    // FXML refs (ensure they exist in FXML)
    @FXML private TableView<Project> projectTable;
    @FXML private TreeTableView<Task> taskTree;
    @FXML private Label statusLabel;
    @FXML private Button assignManagerBtn;  // Admin-only
    @FXML private Button assignMembersBtn;  // Manager-only
    @FXML private Button assignTaskBtn;     // Manager-only
    @FXML private Button newSubtaskBtn;
    @FXML private Button notificationBtn;
    // Add this method to your DashboardController class
    @FXML
    private void onShowNotifications() {
        if (currentUser == null) {
            statusLabel.setText("Not logged in");
            return;
        }

        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) {
            statusLabel.setText("Select a project first");
            return;
        }

        try {
            // Determine user role for notifications
            NotificationService.Role notificationRole;
            String globalRole = currentUser.getGlobalRole();
            if ("ADMIN".equalsIgnoreCase(globalRole)) {
                notificationRole = NotificationService.Role.ADMIN;
            } else if ("MANAGER".equalsIgnoreCase(globalRole)) {
                notificationRole = NotificationService.Role.MANAGER;
            } else {
                notificationRole = NotificationService.Role.MEMBER;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Notification.fxml"));
            Scene scene = new Scene(loader.load());
            NotificationController ctrl = loader.getController();
            ctrl.setContext(p.getProjectId(), currentUser.getUserId(), notificationRole);

            Stage stage = new Stage();
            stage.setTitle("Notifications - " + p.getName());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Open notifications failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Timeline poller;

    private final ProjectService projectService =
            new ProjectService(new ProjectDAOImpl(), new TaskDAOImpl());
    private final TaskService taskService =
            new TaskService(new TaskDAOImpl(), new TaskDependencyDAOImpl()); // supply dependencyDAO if you use it

    private User currentUser;

    {
        // Inject ActivityLogDAO so TaskService can check creators
        taskService.setActivityLogDAO(new ActivityLogDAOImpl());
    }


    public void setCurrentUser(User u) {
        AuthContext.setUsername(u.getUsername());
        this.currentUser = u;
        statusLabel.setText("Logged in as: " + u.getUsername());
        applyRoleUI();
        loadProjects();
        NotificationScheduler.startScheduler();
    }

    @FXML
    private void onNewSubtask() {
        if (currentUser == null) {
            statusLabel.setText("Not logged in");
            return;
        }

        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) {
            statusLabel.setText("Select a project");
            return;
        }

        Task parent = getSelectedTask();
        if (parent == null) {
            statusLabel.setText("Select a parent task first");
            return;
        }

        try {
            // Validate subtask creation first - will throw detailed exception if blocked
            taskService.validateSubtaskCreation(parent.getTaskId());

            // Check user permissions
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getGlobalRole());
            boolean isManager = projectService.isManagerOfProject(p.getProjectId(), currentUser.getUserId());
            boolean isParentAssignee = parent.getAssigneeId() != null &&
                    parent.getAssigneeId().equals(currentUser.getUserId());

            if (!isAdmin && !isManager && !isParentAssignee) {
                showTaskCreationWarning("Permission Denied",
                        "You can only create subtasks under tasks assigned to you.\n\n" +
                                "Parent Task: " + parent.getTitle() + "\n" +
                                "Assigned to: " + (parent.getAssigneeName() != null ? parent.getAssigneeName() : "Unassigned") + "\n" +
                                "Your Role: " + currentUser.getGlobalRole());
                return;
            }

            // If validation passes, proceed with subtask creation
            var members = projectService.projectMembers(p.getProjectId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SubtaskForm.fxml"));
            Scene scene = new Scene(loader.load());
            SubtaskFormController ctrl = loader.getController();
            ctrl.init(parent, currentUser, members);

            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("New Subtask under: " + parent.getTitle());
            dlg.setScene(scene);
            dlg.showAndWait();

            loadTasksForProject(p.getProjectId());

        } catch (SQLException ex) {
            // Show detailed warning dialog for validation errors
            showTaskCreationWarning("Cannot Create Subtask", ex.getMessage());
            statusLabel.setText("Subtask creation blocked");
        } catch (Exception e) {
            statusLabel.setText("Subtask creation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void wireNewSubtaskButton() {
        if (newSubtaskBtn == null) return;
        newSubtaskBtn.setDisable(true);
        taskTree.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> {
            boolean hasTask = b != null && b.getValue() != null && b.getValue().getTaskId() != null;
            newSubtaskBtn.setDisable(!hasTask);
        });
    }

    @FXML
    public void initialize() {
        wireEditProjectButton();
        // Projects table columns
        TableColumn<Project, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Project, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        projectTable.getColumns().setAll(nameCol, statusCol);

        // Tasks tree columns (title, status, priority, progress, assignee)
        TreeTableColumn<Task, String> tTitle = new TreeTableColumn<>("Title");
        tTitle.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getValue().getTitle()));
        tTitle.setPrefWidth(400);
        taskTree.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        TreeTableColumn<Task, String> tStatus = new TreeTableColumn<>("Status");
        tStatus.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getValue().getStatus()));
        TreeTableColumn<Task, String> tPrio = new TreeTableColumn<>("Priority");
        tPrio.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getValue().getPriority()));
        TreeTableColumn<Task, Number> tProg = new TreeTableColumn<>("Progress");
        tProg.setCellValueFactory(p ->
                new javafx.beans.property.SimpleDoubleProperty(
                        p.getValue().getValue().getProgressPct() / 100.0
                )
        );
        //tProg.setCellFactory(javafx.scene.control.cell.ProgressBarTreeTableCell.forTreeTableColumn());
        tProg.setPrefWidth(140);

        TreeTableColumn<Task, String> tPct = new TreeTableColumn<>("%");
        tPct.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f%%", p.getValue().getValue().getProgressPct())
                )
        );
        tPct.setPrefWidth(70);

        TreeTableColumn<Task, String> tAssignee = new TreeTableColumn<>("Assigned to");
        tAssignee.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue().getValue().getAssigneeName() == null ? "" : p.getValue().getValue().getAssigneeName()));
      
        taskService.setActivityLogDAO(new ActivityLogDAOImpl());
        taskService.setProjectDAO(new ProjectDAOImpl());
        taskService.setProjectService(projectService);

        taskTree.getColumns().setAll(tTitle, tStatus, tPrio, tProg, tAssignee , tPct );

        if (taskTree != null) wireAssignButton();

        // When project selection changes, refresh tasks
        projectTable.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> {
            if (b != null) {
                try { loadTasksForProject(b.getProjectId()); }
                catch (Exception e) { statusLabel.setText("Load tasks failed: " + e.getMessage()); }
            } else {
                taskTree.setRoot(null);
            }
        });

        wireAssignButton();
        wireNewSubtaskButton();

    }

    private void applyRoleUI() {
        if (currentUser == null) return;
        String role = currentUser.getGlobalRole();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role);
        if (assignManagerBtn != null) assignManagerBtn.setDisable(!isAdmin);
        if (assignMembersBtn != null) assignMembersBtn.setDisable(!isManager && !isAdmin);
        if (assignTaskBtn != null) assignTaskBtn.setDisable(!isManager && !isAdmin);
    }

    private void loadProjects() {
        try {
            if (currentUser == null) { projectTable.getItems().clear(); return; }
            String role = currentUser.getGlobalRole();
            if ("ADMIN".equalsIgnoreCase(role)) {
                projectTable.getItems().setAll(projectService.all());
            } else if ("MANAGER".equalsIgnoreCase(role)) {
                projectTable.getItems().setAll(projectService.byManager(currentUser.getUserId()));
            } else {
                projectTable.getItems().setAll(projectService.byMember(currentUser.getUserId()));
            }
        } catch (SQLException e) {
            statusLabel.setText("Failed to load projects: " + e.getMessage());
        }
    }

    private void loadTasksForProject(long projectId) throws SQLException {
        List<Task> tasks = taskService.findByProject(projectId); // or projectTasks(projectId)

        // Build parent->children map
        Map<Long, TreeItem<Task>> byId = new HashMap<>();
        TreeItem<Task> root = new TreeItem<>(new Task(projectId, "ROOT")); // dummy invisible root
        root.setExpanded(true);

        // First pass: create an item for each task
        for (Task t : tasks) {
            TreeItem<Task> item = new TreeItem<>(t);
            byId.put(t.getTaskId(), item);
        }

        // Second pass: attach to parent or root
        for (Task t : tasks) {
            TreeItem<Task> item = byId.get(t.getTaskId());
            Long parentId = t.getParentTaskId();
            if (parentId != null && byId.containsKey(parentId)) {
                byId.get(parentId).getChildren().add(item);
            } else {
                root.getChildren().add(item);
            }
        }

        taskTree.setRoot(root);
        taskTree.setShowRoot(false);
    }



    private Task getSelectedTask() {
        TreeItem<Task> item = taskTree.getSelectionModel().getSelectedItem();
        return item != null ? item.getValue() : null;
    }

    // =========== Handlers ===========


    @FXML
    private void onOpenProject() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project first."); return; }
        try { loadTasksForProject(p.getProjectId()); }
        catch (Exception e) { statusLabel.setText("Open project failed: " + e.getMessage()); }
    }

    // ADMIN: assign a Manager to the selected project (PROJECT_MEMBERS role='MANAGER')
    @FXML
    private void onAssignManager() {
        if (currentUser == null) { statusLabel.setText("Not logged in"); return; }
        if (!"ADMIN".equalsIgnoreCase(currentUser.getGlobalRole())) {
            statusLabel.setText("Only Admin can assign a manager.");
            return;
        }
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project"); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AssignManagerDialog.fxml"));
            Scene scene = new Scene(loader.load());
            AssignManagerController ctrl = loader.getController();
            ctrl.init(p, (proj, manager) -> {
                statusLabel.setText("Assigned manager " + manager.getUsername() + " to " + proj.getName());
            });
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Assign Manager");
            dlg.setScene(scene);
            dlg.showAndWait();
        } catch (Exception e) {
            statusLabel.setText("Open Assign Manager dialog failed: " + e.getMessage());
        }
    }

    // MANAGER/ADMIN: assign members to selected project (role='MEMBER')
    @FXML
    private void onAssignMembers() {
        if (currentUser == null) { statusLabel.setText("Not logged in"); return; }
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project"); return; }

        try {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getGlobalRole());
            boolean isGlobalManager = "MANAGER".equalsIgnoreCase(currentUser.getGlobalRole());
            boolean isProjectManager = projectService.isManagerOfProject(p.getProjectId(), currentUser.getUserId());

            if (!isAdmin && !isGlobalManager && !isProjectManager) {
                statusLabel.setText("You cannot assign members for this project");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AssignProjectMembersDialog.fxml"));
            Scene scene = new Scene(loader.load());
            AssignProjectMembersController ctrl = loader.getController();
            ctrl.init(p);
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Assign Project Members");
            dlg.setScene(scene);
            dlg.showAndWait();

        } catch (Exception e) {
            statusLabel.setText("Open dialog failed: " + e.getMessage());
        }
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
        if (p == null) {
            statusLabel.setText("Select a project to edit");
            return;
        }
        if (currentUser == null || currentUser.getUserId() != p.getOwnerId()) {
            statusLabel.setText("Only the project owner can edit this project");
            new Alert(Alert.AlertType.WARNING,
                    "Only the project owner can edit project details.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectForm.fxml"));
            Scene scene = new Scene(loader.load());
            ProjectFormController ctrl = loader.getController();
            ctrl.initForEdit(currentUser, p, updated -> {
                loadProjects();
                // Refresh tasks if the project status changed
                try {
                    if (!p.getStatus().equals(updated.getStatus())) {
                        loadTasksForProject(updated.getProjectId());
                        statusLabel.setText("Project updated: " + updated.getName() +
                                " (Status: " + updated.getStatus() + ")");
                    } else {
                        statusLabel.setText("Project updated: " + updated.getName());
                    }
                } catch (SQLException e) {
                    statusLabel.setText("Project updated but task refresh failed: " + e.getMessage());
                }
            });
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Edit Project");
            dlg.setScene(scene);
            dlg.showAndWait();
        } catch (Exception ex) {
            statusLabel.setText("Edit failed: " + ex.getMessage());
        }
    }

    private void wireEditProjectButton() {
        projectTable.getSelectionModel().selectedItemProperty().addListener((obs, oldProject, newProject) -> {
            boolean canEdit = false;
            if (newProject != null && currentUser != null) {
                // Only enable if current user is the project owner
                canEdit = (currentUser.getUserId() == newProject.getOwnerId());
            }

            // Find the Edit button in the HBox (you might need to add an fx:id to the button)
            HBox projectButtons = (HBox) projectTable.lookup("HBox");
            if (projectButtons != null) {
                for (javafx.scene.Node node : projectButtons.getChildren()) {
                    if (node instanceof Button) {
                        Button button = (Button) node;
                        if ("Edit".equals(button.getText())) {
                            button.setDisable(!canEdit);
                            break;
                        }
                    }
                }
            }
        });
    }

    @FXML
    public void onDeleteProject() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project to delete"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete project \"" + p.getName() + "\"?\nAll tasks, subtasks and logs will be removed.",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    projectService.deleteProjectWithAuth(p.getProjectId(), currentUser);
                    loadProjects();
                    taskTree.setRoot(null);
                    statusLabel.setText("Project deleted.");
                } catch (SecurityException se) {
                    new Alert(Alert.AlertType.WARNING, se.getMessage()).showAndWait();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, "Delete failed: " + e.getMessage()).showAndWait();
                }
            }
        });
    }


    @FXML
    public void onNewTask() {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) {
            statusLabel.setText("Open a project first");
            return;
        }

        try {
            // Validate before opening form - will throw detailed exception if blocked
            taskService.validateTaskCreation(p.getProjectId());

            // If validation passes, open the form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskForm.fxml"));
            Scene scene = new Scene(loader.load());
            TaskFormController ctrl = loader.getController();
            ctrl.initForCreate(p.getProjectId(), savedId -> {
                try {
                    loadTasksForProject(p.getProjectId());
                    statusLabel.setText("Task created with ID: " + savedId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("New Task");
            dlg.setScene(scene);
            dlg.showAndWait();

        } catch (SQLException ex) {
            // Show detailed warning dialog instead of just status message
            showTaskCreationWarning("Cannot Create Task", ex.getMessage());
            statusLabel.setText("Task creation blocked");
        } catch (Exception ex) {
            statusLabel.setText("Task creation failed: " + ex.getMessage());
        }
    }

    private void showTaskCreationWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(450);
        //alert.getDialogPane().setGraphic(true);
        alert.setResizable(true);
        alert.showAndWait();
    }

    @FXML
    public void onStartTask() {
        Task selected = getSelectedTask();
        if (selected == null) {
            statusLabel.setText("Select a task");
            return;
        }
        try {
            taskService.start(selected.getTaskId());
            reloadSelectedProjectTasks();
            statusLabel.setText("Task started");
        } catch (SQLException e) {
            // Show warning dialog instead of status label
            showWarningDialog("Cannot Start Task", e.getMessage());
            statusLabel.setText("Task start blocked by priority rules");
        }
    }

    // Add this helper method to DashboardController
    private void showWarningDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Priority Rule Violation");
        alert.setContentText(message);

        // Make dialog larger to fit the task list
        alert.getDialogPane().setMinWidth(500);
        alert.getDialogPane().setMinHeight(200);

        // Add OK button
        alert.getButtonTypes().setAll(ButtonType.OK);

        // Show and wait for user to click OK
        alert.showAndWait();
    }

    @FXML
    public void onCompleteTask() {
        Task selected = getSelectedTask();
        if (selected == null) {
            statusLabel.setText("Select a task");
            return;
        }
        try {
            taskService.complete(selected.getTaskId());
            reloadSelectedProjectTasks();
            loadProjects();
            statusLabel.setText("Task completed");
        } catch (SQLException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void onRefreshTasks() throws SQLException {
        reloadSelectedProjectTasks();
    }


    private void reloadSelectedProjectTasks() throws SQLException {
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { taskTree.setRoot(null); return; }
        try { loadTasksForProject(p.getProjectId()); }
        catch (Exception e) { statusLabel.setText("Refresh failed: " + e.getMessage()); }
    }


    @FXML
    private void onAssignTaskTo() {
        if (currentUser == null) { statusLabel.setText("Not logged in"); return; }
        Project p = projectTable.getSelectionModel().getSelectedItem();
        if (p == null) { statusLabel.setText("Select a project first"); return; }

        Task task = getSelectedTask();
        if (task == null) { statusLabel.setText("Select a task to assign"); return; }
        try {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getGlobalRole());
            boolean isGlobalManager = "MANAGER".equalsIgnoreCase(currentUser.getGlobalRole());
            boolean isProjectManager = projectService.isManagerOfProject(p.getProjectId(), currentUser.getUserId());

            if (!isAdmin && !isGlobalManager && !isProjectManager) {
                statusLabel.setText("You cannot assign tasks in this project");
                return;
            }

            var members = projectService.projectMembers(p.getProjectId());
            if (members == null || members.isEmpty()) {
                statusLabel.setText("No members in this project. Use 'Assign Members' first.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AssignTaskDialog.fxml"));
            Scene scene = new Scene(loader.load());
            AssignTaskController ctrl = loader.getController();
            ctrl.init(task, p.getProjectId(), currentUser, members, who -> {
                statusLabel.setText("Assigned to " + who.getUsername());
                try { loadTasksForProject(p.getProjectId()); } catch (Exception ignored) {}
            });
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Assign Task To Member");
            dlg.setScene(scene);
            dlg.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Open assign dialog failed: " + e.getMessage());
        }
    }
    // Enable/disable the button based on selection
    private void wireAssignButton() {
        if (assignTaskBtn == null) return;
        assignTaskBtn.setDisable(true);
        taskTree.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> {
            boolean hasTask = b != null && b.getValue() != null && b.getValue().getTaskId() != null;
            String role = currentUser != null ? currentUser.getGlobalRole() : "";
            boolean canTry = "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
            assignTaskBtn.setDisable(!(hasTask && canTry));
        });
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
    private void onLogout() {
        try {
            this.currentUser = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            AuthContext.clear();
            Scene scene = new Scene(loader.load(), 920, 600);
            Stage stage = (Stage) projectTable.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Logout failed: " + e.getMessage());
        }
    }
    @FXML
    public void onDeleteTask() {
        var sel = taskTree.getSelectionModel().getSelectedItem();
        Task t = (sel == null ? null : sel.getValue());
        if (t == null) { statusLabel.setText("Select a task to delete"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + t.getTitle() + "\" and all its subtasks?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    taskService.deleteTaskWithAuth(t.getTaskId(), currentUser);
                    // refresh current project's tree
                    Project p = projectTable.getSelectionModel().getSelectedItem();
                    if (p != null) loadTasksForProject(p.getProjectId());
                    statusLabel.setText("Task deleted.");
                } catch (SecurityException se) {
                    new Alert(Alert.AlertType.WARNING, se.getMessage()).showAndWait();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, "Delete failed: " + e.getMessage()).showAndWait();
                }
            }
        });
    }

    // ... existing initialize code ...


}


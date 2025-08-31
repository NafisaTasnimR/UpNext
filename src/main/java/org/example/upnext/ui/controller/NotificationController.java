package org.example.upnext.ui.controller;
import org.example.upnext.service.NotificationService.Role;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.upnext.dao.impl.NotificationDAOImpl;
import org.example.upnext.model.Notification;
import org.example.upnext.model.Task;
import org.example.upnext.service.NotificationService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationController {

    // ===== FXML refs =====
    @FXML private TableView<Notification> notificationTable;
    @FXML private TableColumn<Notification, String> colType, colTaskTitle, colMessage;
    @FXML private TableColumn<Notification, java.time.LocalDate> colDueDate;
    @FXML private TableColumn<Notification, java.time.LocalDateTime> colCreatedAt;

    // ===== Dependencies / context =====
    private NotificationService notificationService;
    private long currentProjectId;
    private long currentUserId;
    private Role currentRole;

    @FXML
    public void initialize() {
        this.notificationService = new NotificationService(new NotificationDAOImpl());
        setupTable();
    }

    /** Parent controller must call once project/user/role are known */
    public void setContext(long projectId, long userId, Role role) {
        this.currentProjectId = projectId;
        this.currentUserId = userId;
        this.currentRole = role;
        updateRoleUI();
        refreshNotifications();
    }

    // ===== Actions =====
    @FXML public void onRefreshNotifications() { refreshNotifications(); }

    @FXML public void onShowInbox() {
        var data = notificationService.getInbox(currentProjectId, currentUserId, currentRole);
        notificationTable.getItems().setAll(data);
    }

    @FXML public void onShowOverdue() {
        List<Task> tasks = notificationService.getOverdue(currentProjectId, currentUserId, currentRole);
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Overdue tasks");
        a.setContentText(tasks.isEmpty() ? "None" :
                String.join("\n", tasks.stream().map(t ->
                        (t.getDueDate() == null ? "" : t.getDueDate()+" - ") + t.getTitle()
                ).toList()));
        a.showAndWait();
    }

    // ===== Helpers =====
    private void refreshNotifications() {
        if (currentRole == null || currentProjectId == 0 || currentUserId == 0) return;
        var data = notificationService.getInbox(currentProjectId, currentUserId, currentRole);
        notificationTable.getItems().setAll(data);
    }

    private void setupTable() {
        colType.setCellValueFactory(cd -> new SimpleStringProperty(n2e(cd.getValue().getType())));
        colTaskTitle.setCellValueFactory(cd -> new SimpleStringProperty(n2e(cd.getValue().getTaskTitle())));
        colMessage.setCellValueFactory(cd -> new SimpleStringProperty(n2e(cd.getValue().getMessage())));
        colDueDate.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getDueDate()));
        colCreatedAt.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getCreatedAt()));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(java.time.LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : dtf.format(item));
            }
        });
    }

    private void updateRoleUI() {
        // No role-specific UI changes needed now
    }

    private static String n2e(String s) { return s == null ? "" : s; }
}
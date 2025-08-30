package org.example.upnext.ui.controller;

import org.example.upnext.dao.impl.UserDAOImpl;
import org.example.upnext.model.User;
import org.example.upnext.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Arrays;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label errorLabel;

    private final AuthService auth = new AuthService(new UserDAOImpl());

    @FXML
    public void initialize() {
        roleBox.getItems().setAll(Arrays.asList("MEMBER", "MANAGER", "ADMIN"));
        roleBox.getSelectionModel().select("MEMBER");
    }

    @FXML
    public void onRegister() {
        errorLabel.setText("");
        try {
            String u = safe(usernameField.getText());
            String e = safe(emailField.getText());
            String p = safe(passwordField.getText());
            String c = safe(confirmField.getText());
            String role = roleBox.getValue();

            if (u.isEmpty() || e.isEmpty() || p.isEmpty() || c.isEmpty()) {
                errorLabel.setText("All fields are required");
                return;
            }
            if (!p.equals(c)) {
                errorLabel.setText("Passwords do not match");
                return;
            }

            User user = new User();
            user.setUsername(u);
            user.setEmail(e);
            user.setGlobalRole(role);
            user.setStatus("ACTIVE");

            auth.register(user, p);

            new Alert(Alert.AlertType.INFORMATION, "Registration successful! You can now log in.", ButtonType.OK).showAndWait();
            go("/fxml/LoginView.fxml");
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void onBack() {
        go("/fxml/LoginView.fxml"); // or back to landing if you prefer
    }

    private void go(String fxml) {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(new FXMLLoader(getClass().getResource(fxml)).load(), 920, 600));
            stage.centerOnScreen();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
    }


    private static String safe(String s) { return s == null ? "" : s.trim(); }
}

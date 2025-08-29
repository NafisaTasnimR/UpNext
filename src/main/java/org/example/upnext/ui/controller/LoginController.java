package org.example.upnext.ui.controller;

import org.example.upnext.dao.impl.UserDAOImpl;
import org.example.upnext.model.User;
import org.example.upnext.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService auth = new AuthService(new UserDAOImpl());

    @FXML
    public void onLogin(ActionEvent e) {
        try {
            Optional<User> u = auth.login(usernameField.getText().trim(), passwordField.getText());
            if (u.isPresent()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardView.fxml"));
                Scene scene = new Scene(loader.load());
                DashboardController ctrl = loader.getController();
                ctrl.setCurrentUser(u.get());
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(scene);
                stage.centerOnScreen();
            } else {
                errorLabel.setText("Invalid credentials or inactive user");
            }
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}

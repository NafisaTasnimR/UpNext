package org.example.upnext.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LandingController {
    @FXML private BorderPane root; // injected from fx:id="root"

    @FXML
    public void onLogin() { switchScene("/fxml/LoginView.fxml"); }

    @FXML
    public void onRegister() { switchScene("/fxml/RegisterView.fxml"); }

    private void switchScene(String fxmlPath) {
        try {
            Stage stage = (Stage) root.getScene().getWindow();
            Scene scene = new Scene(new FXMLLoader(getClass().getResource(fxmlPath)).load(), 920, 600);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

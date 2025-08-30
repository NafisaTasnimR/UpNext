package org.example.upnext;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Properties;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        String title = "UpNext Task Manager";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties p = new Properties(); p.load(in);
                title = p.getProperty("app.title", title);
            }
        } catch (Exception ignored) {}

        /*FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        Scene scene = new Scene(loader.load(), 920, 600);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();*/
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LandingView.fxml"));
        Scene scene = new Scene(loader.load(), 920, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}

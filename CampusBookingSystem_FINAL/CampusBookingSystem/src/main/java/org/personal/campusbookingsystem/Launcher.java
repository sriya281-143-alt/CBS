package org.personal.campusbookingsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/personal/campusbookingsystem/login/login.fxml")
        );

        // login.fxml declares stylesheets="@../css/style.css" on its own root
        // element, so the Scene picks up the styling without adding it here in code.
        Scene scene = new Scene(loader.load());

        stage.setTitle("Campus Booking System");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package org;

import javafx.application.Application;
import javafx.stage.Stage;
import org.controller.MainController;

public class SmartFaceApplication extends Application {

    private MainController controller;

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Face Commander Pro");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.centerOnScreen();

        controller = new MainController();
        controller.start(stage);
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
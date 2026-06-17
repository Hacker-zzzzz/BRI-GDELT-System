package edu.course.brigdelt;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.service.StartupService;
import edu.course.brigdelt.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 */
public class BriGdeltApplication extends Application {

    private final StartupService startupService = new StartupService();

    @Override
    public void start(Stage stage) {
        AppPaths paths = startupService.initialize();

        MainView mainView = new MainView(paths);
        Scene scene = new Scene(mainView.createContent(), 1120, 720);
        scene.getStylesheets().add(BriGdeltApplication.class
                .getResource("/styles/app.css")
                .toExternalForm());

        stage.setTitle("一带一路沿线国家合作态势分析系统");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

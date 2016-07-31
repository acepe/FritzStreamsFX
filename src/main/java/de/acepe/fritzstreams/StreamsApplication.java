package de.acepe.fritzstreams;

import de.acepe.fritzstreams.ui.StreamsController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class StreamsApplication extends Application {

    private ScreenManager screenManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        screenManager = new ScreenManager(this);
        screenManager.loadScreen(ScreenId.SETTINGS);
        screenManager.loadScreen(ScreenId.STREAMS);

        screenManager.setScreen(ScreenId.STREAMS);

        BorderPane root = new BorderPane();
        root.setCenter(screenManager);

        Scene scene = new Scene(root, 630, 450);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Fritz Streams");
        primaryStage.setX(600);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        ((StreamsController) screenManager.getController(ScreenId.STREAMS)).stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

}

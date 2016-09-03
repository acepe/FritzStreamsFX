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
        screenManager.loadScreen(Screens.SETTINGS);
        screenManager.loadScreen(Screens.STREAMS);

        BorderPane root = new BorderPane();
        root.setCenter(screenManager);

        Screens startScreen = Screens.STREAMS;
        Scene scene = new Scene(root, startScreen.getWidth(), startScreen.getHeight());
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setX(600);
        primaryStage.show();
        primaryStage.setOnCloseRequest((event -> {
            // event.consume();
            screenManager.closeStages();
        }));

        screenManager.setScreen(startScreen);
    }

    @Override
    public void stop() throws Exception {
        ((StreamsController) screenManager.getController(Screens.STREAMS)).stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package de.acepe.fritzstreams;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StreamsApplication extends Application {

    @SuppressWarnings("StaticNonFinalField")
    private static Application application;

    @Override
    public void start(Stage primaryStage) throws Exception {
        application = this;
        Parent root = FXMLLoader.load(getClass().getResource("streams.fxml"));
        primaryStage.setTitle("Fritz Streams");
        primaryStage.setX(600);
        Scene scene = new Scene(root, 630, 450);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    static Application getApplication() {
        return application;
    }
}

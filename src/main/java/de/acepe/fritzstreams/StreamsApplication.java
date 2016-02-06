package de.acepe.fritzstreams;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StreamsApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("streams.fxml"));
        primaryStage.setTitle("Fritz Streams");
        Scene value = new Scene(root, 500, 450);
        value.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(value);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

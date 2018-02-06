package de.acepe.fritzstreams;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

@SuppressWarnings("Duplicates")
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws MalformedURLException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                downloadTemp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        // Add a scene
        Group root = new Group();
        Scene scene = new Scene(root, 500, 200);

        String s = new File("/tmp/file.mp3").toURI().toURL().toExternalForm();
        Media pick = new Media(s);
        pick.setOnError(() -> System.out.println(pick.getError()));

        MediaPlayer player = new MediaPlayer(pick);
        player.setOnError(() -> System.out.println(player.getError()));
        player.play();

        // Add a mediaView, to display the media. Its necessary !
        // This mediaView is added to a Pane
        MediaView mediaView = new MediaView(player);
        ((Group) scene.getRoot()).getChildren().add(mediaView);

        // show the stage
        primaryStage.setTitle("Media Player");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void downloadTemp() throws IOException {
        URLConnection conn = new URL("https://rbb-fritz-live.sslcast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3").openConnection();
        InputStream is = conn.getInputStream();

        OutputStream outstream = new FileOutputStream(new File("/tmp/file.mp3"));
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            outstream.write(buffer, 0, len);
        }
        outstream.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

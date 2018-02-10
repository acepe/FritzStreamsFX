package de.acepe.fritzstreams;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("Duplicates")
public class HTTPMediaPlayer extends Application {

    private static final String TMP_FILE = "/tmp/file.mp3";
    private static final int MIN_SIZE_IN_KB = 40 * 1024;

    @Override
    public void start(Stage primaryStage) throws MalformedURLException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                downloadTemp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        File file = new File(TMP_FILE);
        for (int i = 0; i < 10; i++) {
            if (file.exists() && file.length() > MIN_SIZE_IN_KB) {
                break;
            }
            Thread.sleep(1000);
        }
        // Add a scene
        Group root = new Group();
        Scene scene = new Scene(root, 500, 200);

        String s = file.toURI().toURL().toExternalForm();
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
        InputStream stream = conn.getInputStream();

//        int maxConnections = 10;
//        int keepAliveDuration = 15;
//        ConnectionPool pool = new ConnectionPool(maxConnections, keepAliveDuration, TimeUnit.SECONDS);
//
//        OkHttpClient httpClient = new OkHttpClient().newBuilder()
//                                                    .connectionPool(pool)
//                                                    .connectTimeout(5, SECONDS)
//                                                    .readTimeout(10, SECONDS)
//                                                    .build();
//
//        Request request = new Request.Builder().url("https://rbb-fritz-live.sslcast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3")
//                                               .build();
//        Response response = httpClient.newCall(request).execute();
//        if (!response.isSuccessful()) {
//            // TODO: error handling
//            return;
//        }
//        InputStream stream = response.body().byteStream();

        OutputStream outstream = new FileOutputStream(new File("/tmp/file.mp3"));
        byte[] buffer = new byte[4096];
        int len;
        while ((len = stream.read(buffer)) > 0) {
            outstream.write(buffer, 0, len);
        }
        outstream.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

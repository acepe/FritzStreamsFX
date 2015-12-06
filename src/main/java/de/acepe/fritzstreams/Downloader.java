package de.acepe.fritzstreams;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;

public class Downloader {

    private final StreamInfo streamInfo;

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();

    public Downloader(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public void download() {
        String userHome = System.getProperty("user.home");

        String pathname = userHome
                          + File.separator
                          + streamInfo.getTitle()
                          + "_"
                          + streamInfo.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                          + ".mp3";
        File file = new File(pathname);

        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                URLConnection connection = new URL(streamInfo.getStreamURL()).openConnection();
                try (InputStream is = connection.getInputStream();
                        OutputStream outstream = new FileOutputStream(file)) {

                    final int size = connection.getContentLength();
                    updateProgress(0, size);

                    byte[] buffer = new byte[4096];
                    int downloadedSum = 0;
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        if (isCancelled()) {
                            break;
                        }
                        downloadedSum += len;
                        updateProgress(downloadedSum, size);
                        outstream.write(buffer, 0, len);
                    }
                    outstream.close();
                } catch (IOException e) {
                    throw new Exception(e);
                }
                return null;
            }
        };
        progress.bind(downloadTask.progressProperty());
        runningProperty().bind(downloadTask.runningProperty());
        new Thread(downloadTask).start();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty runningProperty() {
        return running;
    }
}

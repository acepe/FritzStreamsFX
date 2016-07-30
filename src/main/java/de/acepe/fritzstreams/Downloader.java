package de.acepe.fritzstreams;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;

public class Downloader {
    private static final Logger LOG = LoggerFactory.getLogger(Downloader.class);

    private final StreamInfo streamInfo;

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private Task<Void> downloadTask;
    private File targetFile;

    public Downloader(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public void download() {
        targetFile = new File(streamInfo.getDownloadFileName());

        downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                URLConnection connection = new URL(streamInfo.getStreamURL()).openConnection();
                try (InputStream is = connection.getInputStream();
                        OutputStream outstream = new FileOutputStream(targetFile)) {

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
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                streamInfo.setDownloadedFile(targetFile);
            }

            @Override
            protected void failed() {
                LOG.error("Failed to download stream {}", streamInfo, getException());
            }

            @Override
            protected void cancelled() {
                LOG.info("Download was cancelled, deleting partial download.");
                if (targetFile.exists()) {
                    targetFile.delete();
                }
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

    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }
}

package de.acepe.fritzstreams.backend.stream;

import de.acepe.fritzstreams.backend.download.DownloadTask;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import okhttp3.OkHttpClient;

public class StreamDownloader {

    private final OkHttpClient httpClient;
    private final StreamInfo streamInfo;

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private Task<Void> downloadTask;

    public StreamDownloader(OkHttpClient httpClient, StreamInfo streamInfo) {
        this.httpClient = httpClient;
        this.streamInfo = streamInfo;
    }

    public void download() {
        downloadTask = new DownloadTask<>(httpClient,
                                          streamInfo,
                                          streamInfo::setDownloadedFile,
                                          streamInfo.getPlaylist());

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

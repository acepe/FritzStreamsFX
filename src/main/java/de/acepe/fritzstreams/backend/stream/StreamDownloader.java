package de.acepe.fritzstreams.backend.stream;

import de.acepe.fritzstreams.backend.download.DownloadTask;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;

public class StreamDownloader {

    private final StreamInfo streamInfo;

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private Task<Void> downloadTask;

    public StreamDownloader(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public void download() {
        downloadTask = new DownloadTask<>(streamInfo, streamInfo::setDownloadedFile);

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

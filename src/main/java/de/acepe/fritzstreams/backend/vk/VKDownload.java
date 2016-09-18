package de.acepe.fritzstreams.backend.vk;

import java.io.File;

import com.google.common.base.MoreObjects;

import de.acepe.fritzstreams.FileUtil;
import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.DownloadTask;
import de.acepe.fritzstreams.backend.download.Downloadable;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import javafx.beans.property.*;
import javafx.concurrent.WorkerStateEvent;

public class VKDownload implements Downloadable {

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private final IntegerProperty size = new SimpleIntegerProperty();
    private final IntegerProperty downloadedSize = new SimpleIntegerProperty();
    private final String targetFileName;

    private AudioItem audioItem;
    private DownloadTask<VKDownload> task;

    public VKDownload(AudioItem audioItem) {
        this.audioItem = audioItem;
        targetFileName = createTargetFileName();
    }

    private String createTargetFileName() {
        String targetpath = Settings.getInstance().getTargetpath();
        String filename = audioItem.getArtist() + "-" + audioItem.getTitle();
        filename = FileUtil.escapeStringAsFilename(filename);
        return targetpath + File.separator + filename + ".mp3";
    }

    public void setTask(DownloadTask<VKDownload> task) {
        this.task = task;
        running.bind(task.runningProperty());
        progressProperty().bind(task.progressProperty());
        task.addEventHandler(WorkerStateEvent.ANY, this::onTaskFinished);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    private void onTaskFinished(WorkerStateEvent evt) {
        if (evt.getEventType() == WorkerStateEvent.WORKER_STATE_SUCCEEDED
            || evt.getEventType() == WorkerStateEvent.WORKER_STATE_CANCELLED
            || evt.getEventType() == WorkerStateEvent.WORKER_STATE_FAILED) {

            progressProperty().unbind();
            runningProperty().unbind();
            progressProperty().setValue(1);
            runningProperty().setValue(false);
        }
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public IntegerProperty downloadedSizeProperty() {
        return downloadedSize;
    }

    public AudioItem getAudioItem() {
        return audioItem;
    }

    public void setAudioItem(AudioItem audioItem) {
        this.audioItem = audioItem;
    }

    @Override
    public String getDownloadURL() {
        return audioItem.getUrl();
    }

    @Override
    public String getTargetFileName() {
        return targetFileName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("targetFileName", targetFileName).toString();
    }

    public DownloadTask<VKDownload> getTask() {
        return task;
    }
}

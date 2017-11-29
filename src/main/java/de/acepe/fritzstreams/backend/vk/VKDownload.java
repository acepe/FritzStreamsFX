package de.acepe.fritzstreams.backend.vk;

import java.io.File;

import com.google.common.base.MoreObjects;

import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.DownloadTask;
import de.acepe.fritzstreams.backend.download.Downloadable;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import de.acepe.fritzstreams.util.FileUtil;
import javafx.beans.property.*;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventType;

public class VKDownload implements Downloadable {

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private final ObjectProperty<Integer> sizeInBytes = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> downloadedSizeInBytes = new SimpleObjectProperty<>();
    private final ObjectProperty<EventType> state = new SimpleObjectProperty<>();
    private final String targetFileName;

    private AudioItem audioItem;
    private final Settings settings;
    private DownloadTask<VKDownload> task;

    public VKDownload(AudioItem audioItem, Settings settings) {
        this.audioItem = audioItem;
        this.settings = settings;
        targetFileName = createTargetFileName();
    }

    private String createTargetFileName() {
        String targetpath = settings.getTargetpath();
        String filename = audioItem.getArtist() + "-" + audioItem.getTitle();
        filename = FileUtil.escapeStringAsFilename(filename);
        return targetpath + File.separator + filename + ".mp3";
    }

    public void setTask(DownloadTask<VKDownload> task) {
        this.task = task;
        running.bind(task.runningProperty());
        progress.bind(task.progressProperty());
        task.addEventHandler(WorkerStateEvent.ANY, this::onTaskFinished);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    public void reset() {
        state.setValue(null);
        progress.setValue(0);
        downloadedSizeInBytes.setValue(0);
    }

    private void onTaskFinished(WorkerStateEvent evt) {
        state.setValue(evt.getEventType());
        if (evt.getEventType() == WorkerStateEvent.WORKER_STATE_SUCCEEDED
            || evt.getEventType() == WorkerStateEvent.WORKER_STATE_CANCELLED
            || evt.getEventType() == WorkerStateEvent.WORKER_STATE_FAILED) {

            progress.unbind();
            progress.setValue(1);
            running.unbind();
            running.setValue(false);
        }
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    @Override
    public Integer getTotalSizeInBytes() {
        return sizeInBytes.get();
    }

    public ObjectProperty<Integer> totalSizeInBytesProperty() {
        return sizeInBytes;
    }

    public void setTotalSizeInBytes(Integer sizeInBytes) {
        this.sizeInBytes.set(sizeInBytes);
    }

    @Override
    public Integer getDownloadedSizeInBytes() {
        return downloadedSizeInBytes.get();
    }

    public ObjectProperty<Integer> downloadedSizeInBytesProperty() {
        return downloadedSizeInBytes;
    }

    public void setDownloadedSizeInBytes(Integer downloadedSizeInBytes) {
        this.downloadedSizeInBytes.set(downloadedSizeInBytes);
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

    public EventType getState() {
        return state.get();
    }

    public ObjectProperty<EventType> stateProperty() {
        return state;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("targetFileName", targetFileName).toString();
    }

}
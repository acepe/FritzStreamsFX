package de.acepe.fritzstreams.backend.vk;

import java.io.File;

import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.Downloadable;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import javafx.beans.property.*;

public class VKDownload implements Downloadable {

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private final IntegerProperty size = new SimpleIntegerProperty();
    private final IntegerProperty downloadedSize = new SimpleIntegerProperty();

    private AudioItem audioItem;
    private String targetFileName;

    public VKDownload(AudioItem audioItem) {
        this.audioItem = audioItem;
        targetFileName = createTargetFileName();
    }

    private String createTargetFileName() {
        String targetpath = Settings.getInstance().getTargetpath();
        return targetpath + File.separator + audioItem.getArtist() + "-" + audioItem.getTitle() + ".mp3";
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

}

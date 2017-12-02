package de.acepe.fritzstreams.ui;

import javax.inject.Inject;

import com.sun.javafx.application.HostServicesDelegate;

import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.backend.vk.VKDownload;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AudioItemController {

    private final ObjectProperty<AudioItem> audioItem = new SimpleObjectProperty<>();
    private final Settings settings;
    private final DownloadManager downloadManager;
    private final HostServicesDelegate hostServices;

    @FXML
    private VBox root;
    @FXML
    private Button playButton;
    @FXML
    private Button downloadButton;
    @FXML
    private Label artistLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label durationLabel;

    @Inject
    public AudioItemController(Settings settings, DownloadManager downloadManager, HostServicesDelegate hostServices) {
        this.settings = settings;
        this.downloadManager = downloadManager;
        this.hostServices = hostServices;
    }

    @FXML
    private void initialize() {
    }

    @FXML
    void onDownloadPerformed() {
        downloadManager.addDownload(new VKDownload(audioItem.get(), settings));
    }

    @FXML
    void onPlayPerformed() {
        hostServices.showDocument(audioItem.get().getUrl());
    }

    private void bind() {
        AudioItem audioItem = this.audioItem.get();
        artistLabel.textProperty().setValue(audioItem.getArtist());
        titleLabel.textProperty().setValue(audioItem.getTitle());
        int duration = audioItem.getDuration();
        durationLabel.textProperty().setValue(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
    }

    public void setAudioItem(AudioItem audioItem) {
        this.audioItem.set(audioItem);
        bind();
    }

    public VBox getContent() {
        return root;
    }
}

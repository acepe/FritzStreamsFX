package de.acepe.fritzstreams.ui;

import java.io.IOException;

import javax.inject.Inject;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;

import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.backend.vk.VKDownload;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AudioItemController extends VBox {

    private final ObjectProperty<AudioItem> audioItem = new SimpleObjectProperty<>();
    private final Settings settings;
    private final DownloadManager downloadManager;

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

    private Application application;

    @Inject
    public AudioItemController(Settings settings, DownloadManager downloadManager) {
        this.settings = settings;
        this.downloadManager = downloadManager;
        //FIXME: fragment
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("audio_item.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
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
        HostServicesFactory.getInstance(application).showDocument(audioItem.get().getUrl());
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

    public void setApplication(Application application) {
        this.application = application;
    }
}

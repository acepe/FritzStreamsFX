package de.acepe.fritzstreams.ui;

import java.io.IOException;

import javax.inject.Inject;

import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.backend.vk.VKDownload;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import de.acepe.fritzstreams.util.FileUtil;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class DownloadItemController extends VBox {

    private final ObjectProperty<VKDownload> download = new SimpleObjectProperty<>();
    private final DownloadManager downloadManager;

    @FXML
    private Button cancelButton;
    @FXML
    private Label artistLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label downloadedSizeLabel;
    @FXML
    private Label totalSizeLabel;
    @FXML
    private ProgressBar downloadProgress;


    @Inject
    public DownloadItemController(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;

        // FIXME: Fragment
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("download_item.fxml"));
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
        updateCancelButton();
    }

    @FXML
    void onCancelPerformed() {
        VKDownload vkDownload = download.get();
        if (vkDownload.runningProperty().get()) {
            downloadManager.cancel(vkDownload);
        } else if (vkDownload.getState() == WorkerStateEvent.WORKER_STATE_CANCELLED
                   || vkDownload.getState() == WorkerStateEvent.WORKER_STATE_FAILED) {
            downloadManager.restart(vkDownload);
        }
    }

    public void setDownload(VKDownload download) {
        this.download.set(download);
        bind();
    }

    private void bind() {
        VKDownload vkDownload = this.download.get();
        downloadProgress.progressProperty().bind(vkDownload.progressProperty());
        downloadProgress.visibleProperty().bind(vkDownload.runningProperty());

        vkDownload.stateProperty().addListener((observable, oldValue, newValue) -> updateCancelButton());
        vkDownload.downloadedSizeInBytesProperty()
                  .addListener((observable, oldValue, newValue) -> updateSizeLabel(newValue, downloadedSizeLabel));
        vkDownload.totalSizeInBytesProperty()
                  .addListener((observable, oldValue, newValue) -> updateSizeLabel(newValue, totalSizeLabel));

        AudioItem audioItem = vkDownload.getAudioItem();
        artistLabel.textProperty().setValue(audioItem.getArtist());
        titleLabel.textProperty().setValue(audioItem.getTitle());
    }

    private void updateSizeLabel(Long newValue, Label sizeLabel) {
        sizeLabel.setText(newValue == null ? "" : FileUtil.humanReadableBytes(newValue, true));
    }

    private void updateCancelButton() {
        VKDownload vkDownload = download.get();
        if (vkDownload == null) {
            return;
        }
        if (vkDownload.getState() == WorkerStateEvent.WORKER_STATE_SUCCEEDED) {
            GlyphsDude.setIcon(cancelButton, MaterialDesignIcon.CHECK, "1.5em");
            cancelButton.setDisable(true);
        } else if (vkDownload.getState() == WorkerStateEvent.WORKER_STATE_CANCELLED
                   || vkDownload.getState() == WorkerStateEvent.WORKER_STATE_FAILED) {
            GlyphsDude.setIcon(cancelButton, MaterialDesignIcon.RELOAD, "1.5em");
        } else {
            GlyphsDude.setIcon(cancelButton, FontAwesomeIcon.TIMES_CIRCLE, "1.5em");
        }
    }
}

package de.acepe.fritzstreams.ui;

import java.io.IOException;

import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.backend.vk.VKDownload;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class DownloadItemController extends VBox {

    private final ObjectProperty<VKDownload> download = new SimpleObjectProperty<>();

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

    private DownloadManager downloadManager;

    public DownloadItemController() {
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
        downloadManager = DownloadManager.getInstance();
        updateCancelButton();
    }

    private void updateCancelButton() {
        VKDownload vkDownload = download.get();
        if (vkDownload != null && vkDownload.progressProperty().get() == 1) {
            GlyphsDude.setIcon(cancelButton, MaterialDesignIcon.CHECK, "1.5em");
        } else {
            GlyphsDude.setIcon(cancelButton, FontAwesomeIcon.TIMES_CIRCLE, "1.5em");
        }
    }

    @FXML
    void onCancelPerformed() {
        downloadManager.cancel(download.get());
    }


    public void setDownload(VKDownload download) {
        this.download.set(download);
        bind();
    }

    private void bind() {
        downloadProgress.progressProperty().bind(download.get().progressProperty());
        cancelButton.disableProperty().bind(downloadProgress.progressProperty().isEqualTo(1));
        downloadProgress.progressProperty().addListener((observable, oldValue, newValue) -> updateCancelButton());
        // TODO: bind sizes

        AudioItem audioItem = this.download.get().getAudioItem();
        artistLabel.textProperty().setValue(audioItem.getArtist());
        titleLabel.textProperty().setValue(audioItem.getTitle());
        // durationLabel.textProperty().setValue(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
    }

}

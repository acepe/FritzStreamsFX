package de.acepe.fritzstreams.ui;

import java.io.IOException;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;

import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.Downloader;
import de.acepe.fritzstreams.backend.StreamInfo;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class StreamController extends HBox {

    private final ObjectProperty<StreamInfo> stream = new SimpleObjectProperty<>();
    private final InvalidationListener changeIconListener = this::updateDownloadButton;

    @FXML
    private ImageView imageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subTitleLabel;
    @FXML
    private StackPane downloadStackPane;
    @FXML
    private Button downloadButton;
    @FXML
    private ProgressBar downloadProgress;
    private ScreenManager screenManager;

    public StreamController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("stream_view.fxml"));
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
        streamProperty().addListener((observable, oldValue, stream) -> {
            unbindDownloader();
            unbindStreamInfo();

            bindStreamInfo();
            bindDownloader();
        });
    }

    private void bindStreamInfo() {
        StreamInfo streamInfo = stream.get();

        titleProperty().bind(streamInfo.titleProperty());
        subTitleProperty().bind(streamInfo.subtitleProperty());
        imageProperty().bind(streamInfo.imageProperty());
        downloadButton.disableProperty().bind(streamInfo.initialisedProperty().not());

        streamInfo.downloadedFileProperty().addListener(changeIconListener);
        updateDownloadButton(null);
    }

    private String getButtonText() {
        StreamInfo streamInfo = stream.get();
        return streamInfo.isDownloadFinished() ? "Play" : "Download";
    }

    private void unbindStreamInfo() {
        titleProperty().unbind();
        subTitleProperty().unbind();
        imageProperty().unbind();
        downloadButton.disableProperty().unbind();

        StreamInfo streamInfo = stream.get();
        streamInfo.downloadedFileProperty().removeListener(changeIconListener);
    }

    private void updateDownloadButton(Observable observable) {
        if (stream.get().isDownloadFinished()) {
            GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.PLAY, "1.5em");
        } else {
            GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.DOWNLOAD, "1.5em");
        }
        downloadButton.setText(getButtonText());
    }

    private void bindDownloader() {
        StreamInfo streamInfo = stream.get();
        Downloader downloader = streamInfo.getDownloader();
        if (downloader == null) {
            downloadProgress.setVisible(false);
            return;
        }
        downloadProgress.progressProperty().bind(downloader.progressProperty());
        downloadProgress.visibleProperty().bind(downloader.runningProperty());
        downloadButton.disableProperty().unbind();
        downloadButton.disableProperty().bind(downloader.runningProperty());
    }

    private void unbindDownloader() {
        downloadProgress.progressProperty().unbind();
        downloadProgress.visibleProperty().unbind();
        downloadButton.disableProperty().unbind();
    }

    @FXML
    void onDownloadPerformed() {
        StreamInfo streamInfo = stream.get();
        if (streamInfo.isDownloadFinished()) {
            HostServicesFactory.getInstance(screenManager.getApplication())
                               .showDocument(stream.get().getDownloadedFile().getAbsolutePath());
            return;
        }
        streamInfo.download();
        bindDownloader();
    }

    public StringProperty titleProperty() {
        return titleLabel.textProperty();
    }

    public StringProperty subTitleProperty() {
        return subTitleLabel.textProperty();
    }

    public ObjectProperty<Image> imageProperty() {
        return imageView.imageProperty();
    }

    public ObjectProperty<StreamInfo> streamProperty() {
        return stream;
    }

    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}

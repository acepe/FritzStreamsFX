package de.acepe.fritzstreams.ui;

import static javafx.beans.binding.Bindings.createBooleanBinding;

import java.io.IOException;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.Screens;
import de.acepe.fritzstreams.backend.Downloader;
import de.acepe.fritzstreams.backend.Playlist;
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
import javafx.stage.Stage;

public class StreamController extends HBox {

    private final ObjectProperty<StreamInfo> stream = new SimpleObjectProperty<>();
    private final InvalidationListener changeIconListener = this::updateDownloadButton;
    private final InvalidationListener updatePlayListTextListener = this::updatePlayListButton;

    @FXML
    private ImageView imageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subTitleLabel;
    @FXML
    private StackPane downloadStackPane;
    @FXML
    private Button playListButton;
    @FXML
    private Button downloadButton;
    @FXML
    private ProgressBar downloadProgress;
    private ScreenManager screenManager;
    private Stage playListStage;

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
        playListButton.disableProperty().bind(streamInfo.initialisedProperty().and(createBooleanBinding(() -> {
            Playlist playlist = streamInfo.getPlaylist();
            return playlist != null && !playlist.getEntries().isEmpty();
        }, streamInfo.initialisedProperty())).not());

        streamInfo.initialisedProperty().addListener(updatePlayListTextListener);
        streamInfo.downloadedFileProperty().addListener(changeIconListener);
        updateDownloadButton(null);
        updatePlayListButton(null);
    }

    private void unbindStreamInfo() {
        titleProperty().unbind();
        subTitleProperty().unbind();
        imageProperty().unbind();
        downloadButton.disableProperty().unbind();
        playListButton.disableProperty().unbind();

        StreamInfo streamInfo = stream.get();
        streamInfo.downloadedFileProperty().removeListener(changeIconListener);
        streamInfo.initialisedProperty().removeListener(updatePlayListTextListener);
    }

    private void updateDownloadButton(Observable observable) {
        if (stream.get().isDownloadFinished()) {
            GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.PLAY, "1.5em");
        } else {
            GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.DOWNLOAD, "1.5em");
        }
        downloadButton.setText(getDownloadButtonText());
    }

    private void updatePlayListButton(Observable observable) {
        playListButton.setText(getPlayListButtonText());
    }

    private String getDownloadButtonText() {
        return stream.get().isDownloadFinished() ? "Play" : "Download";
    }

    private String getPlayListButtonText() {
        Playlist playlist = stream.get().getPlaylist();
        return playlist != null && playlist.getEntries().isEmpty() ? "keine Playlist" : "PlayList";
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
    void onPlayListPerformed() {
        if (playListStage != null) {
            playListStage.close();
            playListStage = null;
            return;
        }
        StreamInfo streamInfo = stream.get();
        playListStage = screenManager.showScreenInNewStage(Screens.PLAYLIST);
        ControlledScreen controller = screenManager.getController(Screens.PLAYLIST);
        ((PlaylistController) controller).setPlayList(streamInfo.getPlaylist());
        playListStage.setOnCloseRequest(event -> playListStage = null);
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

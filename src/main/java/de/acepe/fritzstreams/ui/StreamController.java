package de.acepe.fritzstreams.ui;

import static javafx.beans.binding.Bindings.createBooleanBinding;

import java.io.File;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.backend.StreamInfo;
import de.acepe.fritzstreams.util.FileUtil;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class StreamController {
    private static final Logger LOG = LoggerFactory.getLogger(StreamController.class);

    private final ObjectProperty<StreamInfo> stream = new SimpleObjectProperty<>();
    private final InvalidationListener updatePlayListTextListener = this::updatePlayListButton;
    private final Player player;
    private final ScreenManager screenManager;

    @FXML
    private HBox root;
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
    private SplitMenuButton playButton;
    @FXML
    private Button downloadButton;
    @FXML
    private ProgressBar downloadProgress;
    private Stage playListStage;

    @Inject
    public StreamController(Player player, ScreenManager screenManager) {
        this.player = player;
        this.screenManager = screenManager;
    }

    @FXML
    private void initialize() {
        streamProperty().addListener((observable, oldValue, stream) -> {
            unbindDownloader();
            unbindStreamInfo();

            bindStreamInfo();
            bindDownloader();
        });
        GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.DOWNLOAD, "1.5em");
        GlyphsDude.setIcon(playButton, MaterialDesignIcon.PLAY, "1.5em");

        configurePlayMenu();
    }

    private void configurePlayMenu() {
        MenuItem playExternalMenuItem = new MenuItem("Mit Standard-Player abspielen");
        playExternalMenuItem.setOnAction(event -> play(true));

        MenuItem playInternalMenuItem = new MenuItem("Abspielen");
        playInternalMenuItem.setOnAction(event -> play(false));

        MenuItem openFolderMenuItem = new MenuItem("Ordner öffnen");
        openFolderMenuItem.setOnAction(event -> open());

        MenuItem deleteDownloadMenuItem = new MenuItem("Download löschen");
        deleteDownloadMenuItem.setOnAction(event -> delete());

        playButton.getItems().addAll(playInternalMenuItem,
                                     playExternalMenuItem,
                                     new SeparatorMenuItem(),
                                     openFolderMenuItem,
                                     new SeparatorMenuItem(),
                                     deleteDownloadMenuItem);
    }

    private void bindStreamInfo() {
        StreamInfo streamInfo = stream.get();

        titleProperty().bind(streamInfo.titleProperty());
        subTitleProperty().bind(streamInfo.subtitleProperty());
        imageProperty().bind(streamInfo.imageProperty());

        downloadButton.disableProperty().bind(streamInfo.initialisedProperty().not());
        downloadButton.visibleProperty()
                      .bind(streamInfo.initialisedProperty().and(streamInfo.downloadedFileProperty().isNull()));
        playButton.visibleProperty()
                  .bind(streamInfo.initialisedProperty().and(streamInfo.downloadedFileProperty().isNotNull()));

        playListButton.visibleProperty().bind(streamInfo.initialisedProperty().and(createBooleanBinding(() -> {
            Playlist playlist = streamInfo.getPlaylist();
            return playlist != null && !playlist.getEntries().isEmpty();
        }, streamInfo.initialisedProperty())));

        streamInfo.initialisedProperty().addListener(updatePlayListTextListener);

        updatePlayListButton(null);
    }

    private void unbindStreamInfo() {
        titleProperty().unbind();
        subTitleProperty().unbind();
        imageProperty().unbind();
        downloadButton.disableProperty().unbind();
        playListButton.disableProperty().unbind();

        StreamInfo streamInfo = stream.get();
        streamInfo.initialisedProperty().removeListener(updatePlayListTextListener);
    }

    private void updatePlayListButton(Observable observable) {
        playListButton.setText(getPlayListButtonText());
    }

    private String getPlayListButtonText() {
        Playlist playlist = stream.get().getPlaylist();
        return playlist.getEntries().isEmpty() ? "keine Playlist" : "PlayList";
    }

    private void bindDownloader() {
        StreamInfo streamInfo = stream.get();

        downloadProgress.progressProperty().bind(streamInfo.progressProperty());
        downloadProgress.visibleProperty().bind(streamInfo.downloadingProperty());

        downloadButton.disableProperty().unbind();
        downloadButton.disableProperty().bind(streamInfo.downloadingProperty());
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
        LOG.info("Starting Download {}", streamInfo);
        streamInfo.download();
        bindDownloader();
    }

    @FXML
    void onPlayPerformed() {
        play(false);
    }

    private void play(boolean external) {
        StreamInfo streamInfo = stream.get();
        if (!streamInfo.isDownloadFinished()) {
            return;
        }
        LOG.info("Opening finished Download {}", streamInfo);
        File downloadedFile = stream.get().getDownloadedFile();
        if (external) {
            FileUtil.doOpen(downloadedFile);
        } else {
            player.currentFileProperty().setValue(downloadedFile.toPath());
            if (!player.isPlaying()) {
                player.playOrPause();
            }
        }
    }

    private void open() {
        StreamInfo streamInfo = stream.get();
        if (!streamInfo.isDownloadFinished()) {
            return;
        }
        LOG.info("Opening download folder {}", streamInfo);
        File downloadedFile = stream.get().getDownloadedFile();
        FileUtil.doOpenFolder(downloadedFile);
    }

    private void delete() {
        StreamInfo streamInfo = stream.get();
        if (!streamInfo.isDownloadFinished()) {
            return;
        }
        LOG.info("Deleting Download {}", streamInfo);
        File downloadedFile = stream.get().getDownloadedFile();

        player.removePlaylistEntry(downloadedFile.toPath());

        FileUtil.delete(downloadedFile);
        streamInfo.downloadedFileProperty().setValue(null);
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

    public Parent getContent() {
        return root;
    }

}

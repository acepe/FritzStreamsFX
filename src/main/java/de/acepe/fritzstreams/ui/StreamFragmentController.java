package de.acepe.fritzstreams.ui;

import static javafx.beans.binding.Bindings.createBooleanBinding;

import java.io.File;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.backend.OnDemandStream;
import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.Playlist;
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

public class StreamFragmentController {
    private static final Logger LOG = LoggerFactory.getLogger(StreamFragmentController.class);

    private final ObjectProperty<OnDemandStream> stream = new SimpleObjectProperty<>();
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
    public StreamFragmentController(Player player, ScreenManager screenManager) {
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
        OnDemandStream onDemandStream = stream.get();

        titleProperty().bind(onDemandStream.titleProperty());
        subTitleProperty().bind(onDemandStream.subtitleProperty());
        imageProperty().bind(onDemandStream.imageProperty());

        downloadButton.disableProperty().bind(onDemandStream.initialisedProperty().not());
        downloadButton.visibleProperty()
                      .bind(onDemandStream.initialisedProperty().and(onDemandStream.downloadedFileProperty().isNull()));
        playButton.visibleProperty()
                  .bind(onDemandStream.initialisedProperty().and(onDemandStream.downloadedFileProperty().isNotNull()));

        playListButton.visibleProperty().bind(onDemandStream.initialisedProperty().and(createBooleanBinding(() -> {
            Playlist playlist = onDemandStream.getPlaylist();
            return playlist != null && !playlist.getEntries().isEmpty();
        }, onDemandStream.initialisedProperty())));

        onDemandStream.initialisedProperty().addListener(updatePlayListTextListener);

        updatePlayListButton(null);
    }

    private void unbindStreamInfo() {
        titleProperty().unbind();
        subTitleProperty().unbind();
        imageProperty().unbind();
        downloadButton.disableProperty().unbind();
        playListButton.disableProperty().unbind();

        OnDemandStream onDemandStream = stream.get();
        onDemandStream.initialisedProperty().removeListener(updatePlayListTextListener);
    }

    private void updatePlayListButton(Observable observable) {
        playListButton.setText(getPlayListButtonText());
    }

    private String getPlayListButtonText() {
        Playlist playlist = stream.get().getPlaylist();
        return playlist.getEntries().isEmpty() ? "keine Playlist" : "PlayList";
    }

    private void bindDownloader() {
        OnDemandStream onDemandStream = stream.get();

        downloadProgress.progressProperty().bind(onDemandStream.progressProperty());
        downloadProgress.visibleProperty().bind(onDemandStream.downloadingProperty());

        downloadButton.disableProperty().unbind();
        downloadButton.disableProperty().bind(onDemandStream.downloadingProperty());
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
        OnDemandStream onDemandStream = stream.get();
        playListStage = screenManager.showScreenInNewStage(Screens.PLAYLIST);
        ControlledScreen controller = screenManager.getController(Screens.PLAYLIST);
        ((PlaylistController) controller).setPlayList(onDemandStream.getPlaylist());
        playListStage.setOnCloseRequest(event -> playListStage = null);
    }

    @FXML
    void onDownloadPerformed() {
        OnDemandStream onDemandStream = stream.get();
        LOG.info("Starting Download {}", onDemandStream);
        onDemandStream.download();
        bindDownloader();
    }

    @FXML
    void onPlayPerformed() {
        play(false);
    }

    private void play(boolean external) {
        OnDemandStream onDemandStream = stream.get();
        if (!onDemandStream.isDownloadFinished()) {
            return;
        }
        LOG.info("Opening finished Download {}", onDemandStream);
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
        OnDemandStream onDemandStream = stream.get();
        if (!onDemandStream.isDownloadFinished()) {
            return;
        }
        LOG.info("Opening download folder {}", onDemandStream);
        File downloadedFile = stream.get().getDownloadedFile();
        FileUtil.doOpenFolder(downloadedFile);
    }

    private void delete() {
        OnDemandStream onDemandStream = stream.get();
        if (!onDemandStream.isDownloadFinished()) {
            return;
        }
        LOG.info("Deleting Download {}", onDemandStream);
        File downloadedFile = stream.get().getDownloadedFile();

        player.removePlaylistEntry(downloadedFile.toPath());

        FileUtil.delete(downloadedFile);
        onDemandStream.downloadedFileProperty().setValue(null);
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

    public ObjectProperty<OnDemandStream> streamProperty() {
        return stream;
    }

    public Parent getContent() {
        return root;
    }

}

package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.backend.OnDemandStream;
import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.util.FileUtil;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;

import static javafx.beans.binding.Bindings.createBooleanBinding;

public class OnDemandFragmentController {
    private static final Logger LOG = LoggerFactory.getLogger(OnDemandFragmentController.class);
    private static final int FADE_DURATION = 250;

    private final InvalidationListener updatePlayListTextListener = this::updatePlayListButton;
    private final Player player;
    private final ScreenManager screenManager;
    private final OnDemandStreamAdapter adapter;

    @FXML
    private HBox root;
    @FXML
    private ImageView imageView;
    @FXML
    private ImageView fadeImageView;
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
    public OnDemandFragmentController(Player player, ScreenManager screenManager, OnDemandStreamAdapter adapter) {
        this.player = player;
        this.screenManager = screenManager;
        this.adapter = adapter;
    }

    @FXML
    private void initialize() {
        GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.DOWNLOAD, "1.5em");
        GlyphsDude.setIcon(playButton, MaterialDesignIcon.PLAY, "1.5em");

        configurePlayMenu();
    }

    public void setOnDemandStream(OnDemandStream onDemandStream) {
        unbindDownloader();
        unbindStreamInfo();

        adapter.setOnDemandStream(onDemandStream);

        bindStreamInfo();
        bindDownloader();
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
        titleProperty().bind(adapter.titleProperty());
        subTitleProperty().bind(adapter.subtitleProperty());

        downloadButton.disableProperty().bind(adapter.initialisedProperty().not());
        downloadButton.visibleProperty()
                      .bind(adapter.initialisedProperty().and(adapter.downloadedFileProperty().isNull()));
        playButton.visibleProperty()
                  .bind(adapter.initialisedProperty().and(adapter.downloadedFileProperty().isNotNull()));

        playListButton.visibleProperty().bind(adapter.initialisedProperty().and(createBooleanBinding(() -> {
            Playlist playlist = adapter.getPlaylist();
            return playlist != null && !playlist.getEntries().isEmpty();
        }, adapter.initialisedProperty())));

        adapter.initialisedProperty().addListener(updatePlayListTextListener);

        updatePlayListButton(null);

        FadeTransition ft1 = new FadeTransition(Duration.millis(FADE_DURATION), imageView);
        ft1.setFromValue(1.0);
        ft1.setToValue(0);

        fadeImageView.setImage(adapter.imageProperty().getValue());
        FadeTransition ft2 = new FadeTransition(Duration.millis(FADE_DURATION), fadeImageView);
        ft2.setFromValue(0);
        ft2.setToValue(1);
        ft2.setOnFinished(event -> {
            imageProperty().bind(adapter.imageProperty());
            fadeImageView.setOpacity(0);
            imageView.setOpacity(1);
        });
        ft1.play();
        ft2.play();
    }

    private void unbindStreamInfo() {
        titleProperty().unbind();
        subTitleProperty().unbind();
        imageProperty().unbind();

        downloadButton.disableProperty().unbind();
        playListButton.disableProperty().unbind();

        adapter.initialisedProperty().removeListener(updatePlayListTextListener);
    }

    private void updatePlayListButton(Observable observable) {
        playListButton.setText(getPlayListButtonText());
    }

    private String getPlayListButtonText() {
        Playlist playlist = adapter.getPlaylist();
        return playlist.getEntries().isEmpty() ? "keine Playlist" : "PlayList";
    }

    private void bindDownloader() {
        downloadProgress.progressProperty().bind(adapter.progressProperty());
        downloadProgress.visibleProperty().bind(adapter.downloadingProperty());

        downloadButton.disableProperty().unbind();
        downloadButton.disableProperty().bind(adapter.downloadingProperty());
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
        playListStage = screenManager.showScreenInNewStage(Screens.PLAYLIST);
        ControlledScreen controller = screenManager.getController(Screens.PLAYLIST);
        ((PlaylistController) controller).setPlayList(adapter.getPlaylist());
        playListStage.setOnCloseRequest(event -> playListStage = null);
    }

    @FXML
    void onDownloadPerformed() {
        LOG.info("Starting Download {}", adapter);
        adapter.download();
        bindDownloader();
    }

    @FXML
    void onPlayPerformed() {
        play(false);
    }

    private void play(boolean external) {
        if (adapter.isDownloadRunning()) {
            return;
        }
        LOG.info("Opening finished Download {}", adapter);
        File downloadedFile = adapter.getDownloadedFile();
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
        if (adapter.isDownloadRunning()) {
            return;
        }
        LOG.info("Opening download folder {}", adapter);
        File downloadedFile = adapter.getDownloadedFile();
        FileUtil.doOpenFolder(downloadedFile);
    }

    private void delete() {
        if (adapter.isDownloadRunning()) {
            return;
        }
        LOG.info("Deleting Download {}", adapter);
        File downloadedFile = adapter.getDownloadedFile();

        player.removePlaylistEntry(downloadedFile.toPath());

        FileUtil.delete(downloadedFile);
        adapter.downloadedFileProperty().setValue(null);
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

    public Parent getContent() {
        return root;
    }

}

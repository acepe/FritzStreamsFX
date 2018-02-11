package de.acepe.fritzstreams.ui;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAY;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.STOP;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.backend.LiveStream;
import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.StreamManager;
import de.jensd.fx.glyphs.GlyphsDude;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class LiveFragmentController {
    private static final Logger LOG = LoggerFactory.getLogger(LiveFragmentController.class);

    private final Player player;
    private final LiveStream liveStream;

    @FXML
    private VBox root;
    @FXML
    private ImageView imageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subTitleLabel;
    @FXML
    private Button playButton;

    @Inject
    public LiveFragmentController(Player player, StreamManager streamManager) {
        this.player = player;
        liveStream = streamManager.getLiveStream();
    }

    @FXML
    private void initialize() {
        titleProperty().bind(liveStream.titleProperty());
        subTitleProperty().bind(liveStream.subtitleProperty());
        imageProperty().bind(liveStream.imageProperty());

        liveStream.playingProperty().addListener(observable -> updateState());
        liveStream.tmpFileProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                player.currentFileProperty().setValue(nv);
                if (!player.isPlaying()) {
                    player.playOrPause();
                }
            } else {
                player.currentFileProperty().setValue(player.filesProperty().get(0));
            }
        });

        player.stoppedProperty().addListener((obs, ob, stopped) -> {
            if (stopped) {
                stopStream();
            }
        });
        player.currentFileProperty().addListener((obs, ob, nv) -> onPlayedTrackChanged());

        updateState();
    }

    @FXML
    void onPlayPerformed() {
        if (liveStream.isPlaying()) {
            stop();
        } else {
            player.stop();
            play();
        }
    }

    private void updateState() {
        boolean playing = liveStream.isPlaying();
        if (playing) {
            GlyphsDude.setIcon(playButton, STOP, "1.5em");
        } else {
            GlyphsDude.setIcon(playButton, PLAY, "1.5em");
        }
        playButton.setText(playing ? "Stop" : "Play");
    }

    private void play() {
        liveStream.play();

    }

    private void stop() {
        player.stop();
        stopStream();
    }

    private void stopStream() {
        liveStream.stop();
    }

    private void onPlayedTrackChanged() {
        if (liveStream.isPlaying() && player.getCurrentFile() != liveStream.tmpFileProperty().get()) {
            stopStream();
        }
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

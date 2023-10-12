package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.backend.Player;
import de.jensd.fx.glyphs.GlyphsDude;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAY;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.STOP;

public class LiveFragmentController {
    private final Player player;
    private final LiveStreamAdapter liveStreamAdapter;

    @FXML
    private VBox root;
    @FXML
    private ImageView onAirArtistImageView;
    @FXML
    private Label onAirArtistLabel;
    @FXML
    private Label onAirTitleLabel;
    @FXML
    private ImageView imageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subTitleLabel;
    @FXML
    private Button playButton;

    @Inject
    public LiveFragmentController(Player player, LiveStreamAdapter liveStreamAdapter) {
        this.player = player;
        this.liveStreamAdapter = liveStreamAdapter;
    }

    @FXML
    private void initialize() {
        onAirArtistProperty().bind(liveStreamAdapter.onAirArtistProperty());
        onAirTitleProperty().bind(liveStreamAdapter.onAirTitleProperty());
        onAirImageProperty().bind(liveStreamAdapter.onAirImageProperty());

        titleProperty().bind(liveStreamAdapter.titleProperty());
        subTitleProperty().bind(liveStreamAdapter.subtitleProperty());
        imageProperty().bind(liveStreamAdapter.imageProperty());

        liveStreamAdapter.playingProperty().addListener(observable -> updateState());
        liveStreamAdapter.tmpFileProperty().addListener((obs, ov, nv) -> {
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
                liveStreamAdapter.stop();
            }
        });
        player.currentFileProperty().addListener((obs, ob, nv) -> onPlayedTrackChanged());

        updateState();
    }

    @FXML
    void onPlayPerformed() {
        if (liveStreamAdapter.isPlaying()) {
            player.stop();
            liveStreamAdapter.stop();
        } else {
            player.stop();
            liveStreamAdapter.play();
        }
    }

    private void updateState() {
        boolean playing = liveStreamAdapter.isPlaying();
        if (playing) {
            GlyphsDude.setIcon(playButton, STOP, "1.5em");
        } else {
            GlyphsDude.setIcon(playButton, PLAY, "1.5em");
        }
        playButton.setText(playing ? "Stop" : "Play");
    }

    private void onPlayedTrackChanged() {
        if (liveStreamAdapter.isPlaying() && player.getCurrentFile() != liveStreamAdapter.tmpFileProperty().get()) {
            liveStreamAdapter.stop();
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

    public StringProperty onAirTitleProperty() {
        return onAirTitleLabel.textProperty();
    }

    public StringProperty onAirArtistProperty() {
        return onAirArtistLabel.textProperty();
    }

    public ObjectProperty<Image> onAirImageProperty() {
        return onAirArtistImageView.imageProperty();
    }

    public Parent getContent() {
        return root;
    }

}

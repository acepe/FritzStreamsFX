package de.acepe.fritzstreams.ui;

import static de.jensd.fx.glyphs.GlyphsDude.setIcon;

import java.io.IOException;
import java.nio.file.Path;

import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.util.ToStringConverter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

public class PlayerController extends HBox {

    private final Player player;

    @FXML
    private Button prevButton;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button nextButton;
    @FXML
    private ComboBox<Path> nowPlayingComboBox;

    public PlayerController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("player_controls.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        player = Player.getInstance();

        nowPlayingComboBox.getSelectionModel().select(player.getCurrentFile());
        player.currentFileProperty()
              .addListener((observable, oldValue, newValue) -> nowPlayingComboBox.getSelectionModel().select(newValue));
        nowPlayingComboBox.setConverter(new ToStringConverter<>(path -> path.getFileName().toString()));

        nowPlayingComboBox.itemsProperty().bindBidirectional(player.filesProperty());
        nowPlayingComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newSelection) -> {
            player.stop();
            player.currentFileProperty().setValue(newSelection);
            player.playOrPause();
        });

        playPauseButton.disableProperty().bind(player.currentFileProperty().isNull());
        player.playingProperty()
              .addListener(o -> setIcon(playPauseButton,
                                        player.isPlaying() ? FontAwesomeIcon.PAUSE : FontAwesomeIcon.PLAY,
                                        "1.5em"));
        stopButton.disableProperty().bind(player.playingProperty().or(player.pausedProperty()).not());
        nextButton.disableProperty().bind(player.hasNextProperty().not());
        prevButton.disableProperty().bind(player.hasPrevProperty().not());

        setIcon(prevButton, FontAwesomeIcon.STEP_BACKWARD, "1.5em");
        setIcon(nextButton, FontAwesomeIcon.STEP_FORWARD, "1.5em");
        setIcon(stopButton, FontAwesomeIcon.STOP, "1.5em");
        setIcon(playPauseButton, FontAwesomeIcon.PLAY, "1.5em");
    }

    @FXML
    void onNextPerformed() {
        player.next();
    }

    @FXML
    void onPlayPausePerformed() {
        player.playOrPause();
    }

    @FXML
    void onPrevPerformed() {
        player.prev();
    }

    @FXML
    void onStopPerformed() {
        player.stop();
    }

}

package de.acepe.fritzstreams.ui;

import static de.jensd.fx.glyphs.GlyphsDude.setIcon;
import static java.util.concurrent.TimeUnit.*;
import static javafx.beans.binding.Bindings.createDoubleBinding;

import java.io.IOException;
import java.nio.file.Path;

import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.util.ToStringConverter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

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
    @FXML
    private Slider progressSlider;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private Label currentTimeLabel;

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
            boolean wasPlaying = player.isPlaying();
            if (wasPlaying) {
                player.stop();
            }
            player.currentFileProperty().setValue(newSelection);
            if (wasPlaying) {
                player.playOrPause();
            }
        });

        playPauseButton.disableProperty().bind(player.currentFileProperty().isNull());
        player.playingProperty()
              .addListener(o -> setIcon(playPauseButton,
                                        player.isPlaying() ? FontAwesomeIcon.PAUSE : FontAwesomeIcon.PLAY,
                                        "1.5em"));
        stopButton.disableProperty().bind(player.playingProperty().or(player.pausedProperty()).not());
        nextButton.disableProperty().bind(player.hasNextProperty().not());
        prevButton.disableProperty().bind(player.hasPrevProperty().not());

        progressSlider.maxProperty().bind(createDoubleBinding(() -> {
            Duration totalDuration = player.getTotalDuration();
            return totalDuration == null ? 0 : totalDuration.toSeconds();
        }, player.totalDurationProperty()));

        player.currentTimeProperty()
              .addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
                  updateValues();
              });
        player.setOnReady(this::updateValues);

        progressSlider.setOnMouseClicked((MouseEvent mouseEvent) -> {
            player.seek(Duration.seconds(progressSlider.getValue()));
        });

        progressSlider.valueProperty().addListener(ov -> {
            if (progressSlider.isValueChanging()) {
                player.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        setIcon(prevButton, FontAwesomeIcon.STEP_BACKWARD, "1.5em");
        setIcon(nextButton, FontAwesomeIcon.STEP_FORWARD, "1.5em");
        setIcon(stopButton, FontAwesomeIcon.STOP, "1.5em");
        setIcon(playPauseButton, FontAwesomeIcon.PLAY, "1.5em");
        updateValues();
    }

    protected void updateValues() {
        Platform.runLater(() -> {
            Duration totalDuration = player.getTotalDuration();
            Duration currentTime = player.getCurrentTime();

            totalTimeLabel.setText(totalDuration == null ? "---" : formatDuration(totalDuration));
            currentTimeLabel.setText(currentTime == null || totalDuration == null
                    ? "---"
                    : formatDuration(currentTime));

            progressSlider.setDisable(!player.isPlaying());

            Duration duration = player.getDuration();
            if (player.isPlaying() && duration.greaterThan(Duration.ZERO) && !progressSlider.isValueChanging()) {
                progressSlider.setValue(currentTime.toSeconds());
            }
        });
    }

    private String formatDuration(Duration duration) {
        long millis = Double.valueOf(duration.toMillis()).longValue();
        return String.format("%d:%02d:%02d ",
                             MILLISECONDS.toHours(millis),
                             MILLISECONDS.toMinutes(millis) - HOURS.toMinutes(MILLISECONDS.toHours(millis)),
                             MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(millis)));
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

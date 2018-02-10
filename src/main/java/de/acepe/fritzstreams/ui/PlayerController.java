package de.acepe.fritzstreams.ui;

import static de.jensd.fx.glyphs.GlyphsDude.setIcon;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PAUSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY;
import static java.util.concurrent.TimeUnit.*;

import java.nio.file.Path;

import javax.inject.Inject;

import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.util.ToStringConverter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class PlayerController {

    private final Player player;

    @FXML
    private HBox root;
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
    private ProgressBar progress;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private Label currentTimeLabel;

    @Inject
    public PlayerController(Player player) {
        this.player = player;
    }

    @FXML
    private void initialize() {
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
        player.playingProperty().addListener(o -> {
            updateValues();
            setIcon(playPauseButton, player.isPlaying() ? PAUSE : PLAY, "1.5em");
        });
        stopButton.disableProperty().bind(player.playingProperty().or(player.pausedProperty()).not());
        nextButton.disableProperty().bind(player.hasNextProperty().not());
        prevButton.disableProperty().bind(player.hasPrevProperty().not());

        player.setOnReady(this::updateValues);

        EventHandler<MouseEvent> seekOnClickListener = (MouseEvent mouseEvent) -> {
            if (!player.isPlaying()) {
                return;
            }
            double x = mouseEvent.getX();
            double width = progress.getWidth();
            double posPercent = x / width;

            player.seek(Duration.millis(player.getTotalDuration().toMillis() * posPercent));
            updateLabels();
        };
        progress.setProgress(0);
        progress.setOnMouseClicked(seekOnClickListener);
        progress.setOnMouseDragged(seekOnClickListener);
        progress.disableProperty().bind(player.playingProperty().not());

        player.currentTimeProperty().addListener((observable, oldValue, newValue) -> updateValues());
        player.playingProperty().addListener((observable, oldValue, newValue) -> updateValues());

        setIcon(prevButton, FontAwesomeIcon.STEP_BACKWARD, "1.5em");
        setIcon(nextButton, FontAwesomeIcon.STEP_FORWARD, "1.5em");
        setIcon(stopButton, FontAwesomeIcon.STOP, "1.5em");
        setIcon(playPauseButton, PLAY, "1.5em");
        updateValues();
    }

    protected void updateValues() {
        updateLabels();
        if (player.isStopped() || player.getTotalDuration() == null) {
            return;
        }

        double total = player.getTotalDuration().toMillis();
        double percent = player.getCurrentTime().toMillis() / total;
        progress.setProgress(percent);
    }

    private void updateLabels() {
        Duration totalDuration = player.getTotalDuration();
        Duration currentTime = player.getCurrentTime();

        totalTimeLabel.setText(totalDuration == null
                ? "---"
                : "-" + formatDuration(totalDuration.subtract(currentTime)));
        currentTimeLabel.setText(currentTime == null || totalDuration == null ? "---" : formatDuration(currentTime));
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

    public Parent getContent() {
        return root;
    }

}

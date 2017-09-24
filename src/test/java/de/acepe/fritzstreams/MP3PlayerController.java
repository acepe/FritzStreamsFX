package de.acepe.fritzstreams;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.fxmisc.easybind.monadic.MonadicObservableValue;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.Settings;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Duration;

public class MP3PlayerController implements ControlledScreen {

    private static final Duration FADE_DURATION = Duration.seconds(2.5);

    private final ObjectProperty<Path> currentDirectory = new SimpleObjectProperty<>();

    @FXML
    private Slider volumeSlider;
    @FXML
    private Button playButton;
    @FXML
    private ListView<Path> musicList;
    @FXML
    private Label currentDirectoryLabel;
    @FXML
    private ProgressBar currentTimeIndicator;
    @FXML
    private Label timeLabel;
    @FXML
    private TitledPane titledPane;

    private DirectoryChooser directoryChooser = new DirectoryChooser();
    private MonadicObservableValue<MediaPlayer> player;

    // These will be used to keep track of the last known window height
    // independently for when the titled pane is collapsed or expanded.
    // When the expanded state changes, we will restore the window height to the
    // last known value for the new state.
    private double expandedWindowHeight = 400;
    private double collapsedWindowHeight = 200;
    private ScreenManager screenManager;

    double getExpandedWindowHeight() {
        return expandedWindowHeight;
    }

    double getCollapsedWindowHeight() {
        return collapsedWindowHeight;
    }

    double getWindowHeight() {
        if (titledPane == null || titledPane.isExpanded()) {
            return expandedWindowHeight;
        } else {
            return collapsedWindowHeight;
        }
    }

    public void initialize() {
        player = createObservablePlayer();

        configureCurrentDirectory();

        /*
         * // Cross-fade when changing players player.addListener((obs, oldPlayer, newPlayer) -> { fadeOut(oldPlayer);
         * fadeIn(newPlayer); });
         */

        configureSongTimeDisplay();

        // Automatically invoke stop and reset play location at end of media:
        player.addListener((obs, oldPlayer, newPlayer) -> {
            if (oldPlayer != null) {
                oldPlayer.setOnEndOfMedia(null);
            }
            if (newPlayer != null) {
                newPlayer.setOnEndOfMedia(() -> {
                    newPlayer.stop();
                    newPlayer.seek(newPlayer.getStartTime());
                });
                newPlayer.seek(newPlayer.getStartTime());
            }
        });

        // Bind text and disabled property of Play/Pause button, and register action listeners:
        configurePlayButton();

        configureMusicList();
        resizeOnTitledPaneExpand();
    }

    private void configureCurrentDirectory() {
        // Possible directories under user's home directory for startup
        Stream<String> searchPaths = Arrays.stream(new String[] { Settings.getInstance().getTargetpath() });

        // Configure current directory and label:
        final String userHome = System.getProperty("user.home");
        Path file = searchPaths.map(name -> Paths.get(name))
                               .filter(Files::exists)
                               .findFirst()
                               .orElse(Paths.get(userHome)); // should not happen...

        currentDirectory.set(file);
        currentDirectoryLabel.textProperty().bind(EasyBind.map(currentDirectory, Path::getFileName)
                                                          .map(Path::toString)
                                                          .map("Directory: "::concat));
        directoryChooser.initialDirectoryProperty().bind(EasyBind.map(currentDirectory, Path::toFile));
    }

    private void configureMusicList() {
        // Bind ListView's items to list of appropriate files in current directory

        musicList.itemsProperty()
                 .bind(EasyBind.map(currentDirectory, this::listFiles).orElse(FXCollections.emptyObservableList()));

        // custom cell factory to display only the file name, without the extension:
        musicList.setCellFactory(lv -> {
            // create a default cell
            ListCell<Path> cell = new ListCell<Path>();
            // bind the text property
            // the monadic takes care of null cell items and maps to null text
            cell.textProperty()
                .bind(EasyBind.monadic(cell.itemProperty())
                              .map(Path::getFileName)
                              .map(Path::toString)
                              .map(name -> name.substring(0, name.lastIndexOf('.'))));
            return cell;
        });
    }

    // List of supported files in the given directory:
    private ObservableList<Path> listFiles(Path dir) {

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{mp3,m4a,aiff,aif,wav}");
        try {
            return Files.list(dir)
                        .filter(matcher::matches)
                        .collect(Collectors.toCollection(FXCollections::<Path> observableArrayList));
        } catch (IOException exc) {
            return FXCollections.observableArrayList();
        }

    }

    private MonadicObservableValue<MediaPlayer> createObservablePlayer() {
        // Bind player to selected item:
        return EasyBind.monadic(musicList.getSelectionModel().selectedItemProperty()).map(this::createPlayer);
    }

    private MediaPlayer createPlayer(Path file) {
        try {
            return new MediaPlayer(new Media(file.toUri().toURL().toExternalForm()));
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
    }

    private void configureSongTimeDisplay() {
        // current position of play head for current song, in seconds, or 0 if no song:
        final MonadicBinding<Double> currentTimeBinding = player.flatMap(MediaPlayer::currentTimeProperty)
                                                                .map(Duration::toSeconds)
                                                                .orElse(0d);

        // total duration of current song, in seconds, or 0 if no song:
        final MonadicBinding<Double> totalTimeBinding = player.flatMap(player -> player.getMedia().durationProperty())
                                                              .map(Duration::toSeconds)
                                                              .orElse(0d);

        // bind text of label to formatted play head position and total time:
        timeLabel.textProperty().bind(EasyBind.combine(currentTimeBinding, totalTimeBinding, this::formatTimeLabel));

        // bind progress bar to current play head position as proportion of total time:
        currentTimeIndicator.progressProperty().bind(EasyBind.combine(currentTimeBinding,
                                                                      totalTimeBinding,
                                                                      (currentTime, totalTime) -> currentTime
                                                                                                  / totalTime));
    }

    private String formatTimeLabel(double currentTime, double totalTime) {
        return String.format("%s / %s", formatTime(currentTime), formatTime(totalTime));
    }

    private String formatTime(double time) {
        int mins = (int) time / 60;
        int secs = (int) time % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private void configurePlayButton() {

        // Disable button if there's no player
        playButton.disableProperty().bind(Bindings.isNull(player));

        BooleanBinding playing = Bindings.equal(player.flatMap(MediaPlayer::statusProperty), Status.PLAYING);

        // Bind play button text to "Pause" if there's a player with PLAYING status, "Play" otherwise
        playButton.textProperty().bind(Bindings.when(playing).then("Pause").otherwise("Play"));

        // Bind onAction to pause() if playing and play() otherwise:

        playButton.onActionProperty()
                  .bind(Bindings.when(playing)
                                .<EventHandler<ActionEvent>> then(event -> player.get().pause())
                                .otherwise(event -> player.get().play()));
    }

    /*
     * private void fadeOut(MediaPlayer mp) { if (mp != null) { mp.volumeProperty().unbind(); Timeline fadeOut = new
     * Timeline(new KeyFrame(FADE_DURATION, new KeyValue(mp.volumeProperty(), 0))); fadeOut.setOnFinished(event ->
     * mp.stop()); fadeOut.play(); } } private void fadeIn(MediaPlayer mp) { if (mp != null) {
     * mp.volumeProperty().unbind(); Timeline fadeIn = new Timeline(new KeyFrame(Duration.ZERO, new
     * KeyValue(mp.volumeProperty(), 0)), new KeyFrame(FADE_DURATION, new KeyValue(mp.volumeProperty(),
     * volumeSlider.getValue() / 100))); fadeIn.setOnFinished(event ->
     * mp.volumeProperty().bind(volumeSlider.valueProperty().divide(100))); mp.play(); fadeIn.play(); } }
     */

    private void resizeOnTitledPaneExpand() {

        // keep track of current window height (monadic binding will be empty if no scene or window yet):
        MonadicBinding<Number> windowHeight = EasyBind.select(titledPane.sceneProperty())
                                                      .select(Scene::windowProperty)
                                                      .selectObject(Window::heightProperty);

        // track last height independently for when the titled pane is collapsed or expanded:
        windowHeight.addListener((obs, oldValue, newValue) -> {
            if (titledPane.isExpanded()) {
                expandedWindowHeight = newValue.doubleValue();
            } else {
                collapsedWindowHeight = newValue.doubleValue();
            }
        });

        // restore window height to last known value for given expanded state
        // when titled pane is expanded or collapsed:
        titledPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            Window stage = titledPane.getScene().getWindow();
            if (isExpanded) {
                stage.setHeight(expandedWindowHeight);
            } else {
                stage.setHeight(collapsedWindowHeight);
            }
        });
    }

    // handler for button to change directory:
    @FXML
    private void chooseDirectory() {
        File dir = directoryChooser.showDialog(musicList.getScene().getWindow());
        if (dir != null) {
            currentDirectory.set(dir.toPath());
        }
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}
package de.acepe.fritzstreams.backend;

import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

import javax.inject.Inject;

import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

public class Player {

    private static final Logger LOG = LoggerFactory.getLogger(Player.class);

    private final ObjectProperty<Path> currentDirectory = new SimpleObjectProperty<>();
    private final InvalidationListener statusListener = observable -> update();
    private final ObjectProperty<Path> currentFile = new SimpleObjectProperty<>();
    private final ObjectProperty<Duration> totalDuration = new SimpleObjectProperty<>();
    private final ObjectProperty<Duration> currentTime = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty();
    private final BooleanProperty paused = new SimpleBooleanProperty();
    private final BooleanProperty stopped = new SimpleBooleanProperty();
    private final BooleanProperty hasPrev = new SimpleBooleanProperty();
    private final BooleanProperty hasNext = new SimpleBooleanProperty();
    private final ListProperty<Path> files = new SimpleListProperty<>();
    private final Settings settings;
    private final ObjectProperty<MediaPlayer> player = new SimpleObjectProperty<>();

    private int currentIndex = -1;
    private Runnable onReady;

    @Inject
    public Player(Settings settings) {
        this.settings = settings;
        initialize();
    }

    private void initialize() {
        player.addListener(this::updateOnPlayerChange);
        currentFile.addListener(observable -> initPlayer());

        configureCurrentDirectory();
    }

    private void initPlayer() {
        player.setValue(createPlayer(currentFile.get()));
    }

    private MediaPlayer createPlayer(Path file) {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            return new MediaPlayer(new Media(file.toUri().toURL().toExternalForm()));
        } catch (Exception exc) {
            LOG.error("Creating MediaPlayer for {} failed.", file, exc);
            return null;
        }
    }

    private void updateOnPlayerChange(ObservableValue<? extends MediaPlayer> obs,
                                      MediaPlayer oldPlayer,
                                      MediaPlayer newPlayer) {
        totalDuration.unbind();
        currentTime.unbind();
        boolean wasPlaying = false;

        if (oldPlayer != null) {
            if (oldPlayer.getStatus() == Status.PLAYING) {
                wasPlaying = true;
                oldPlayer.stop();
            }
            oldPlayer.setOnReady(null);
            oldPlayer.setOnEndOfMedia(null);
            oldPlayer.statusProperty().removeListener(statusListener);
            oldPlayer.dispose();
        }
        if (newPlayer != null) {
            totalDuration.bind(newPlayer.totalDurationProperty());
            currentTime.bind(newPlayer.currentTimeProperty());
            newPlayer.setOnEndOfMedia(() -> stopAndDispose(newPlayer));
            newPlayer.statusProperty().addListener(statusListener);
            newPlayer.setOnReady(this.onReady);
            if (wasPlaying) {
                newPlayer.play();
            }
            update();
        }
    }

    private void update() {
        MediaPlayer mp = player.get();
        if (mp == null) {
            return;
        }
        Status status = mp.getStatus();
        playing.setValue(status == Status.PLAYING);
        paused.setValue(status == Status.PAUSED);
        stopped.setValue(status == Status.STOPPED || status == null || status == Status.UNKNOWN);
        hasNext.setValue(!files.isEmpty() && currentIndex < files.size() - 1);
        hasPrev.setValue(!files.isEmpty() && currentIndex > 0);
    }

    private void configureCurrentDirectory() {
        Path downloadFolder = Paths.get(settings.getTargetpath());
        Path file = Files.exists(downloadFolder) ? downloadFolder : Paths.get(System.getProperty("user.home"));
        currentDirectory.set(file);
        files.bind(EasyBind.map(currentDirectory, this::listFiles).orElse(FXCollections.emptyObservableList()));
        if (!files.isEmpty()) {
            currentFile.setValue(files.get(0));
            currentIndex = 0;
        }
    }

    private ObservableList<Path> listFiles(Path dir) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{mp3,m4a,aiff,aif,wav}");
        try {
            return Files.list(dir)
                        .filter(matcher::matches)
                        .sorted(Comparator.comparing(Path::getFileName))
                        .collect(toCollection(FXCollections::<Path> observableArrayList));
        } catch (IOException exc) {
            return FXCollections.observableArrayList();
        }
    }

    public void playOrPause() {
        if (currentFile.get() == null) {
            return;
        }
        MediaPlayer mp = player.get();
        if (mp == null) {
            initPlayer();
            mp = player.get();
            mp.play();
            return;
        }

        Status status = mp.getStatus();
        if (status == Status.UNKNOWN || status == Status.HALTED) {
            return;
        }
        if (status == null || status == Status.PAUSED || status == Status.READY || status == Status.STOPPED) {
            mp.play();
        } else {
            mp.pause();
        }
    }

    public void stop() {
        MediaPlayer mp = player.get();
        if (mp == null) {
            return;
        }
        Status status = mp.getStatus();
        if (status == Status.UNKNOWN || status == Status.HALTED) {
            return;
        }
        stopAndDispose(mp);
        currentTimeProperty().setValue(Duration.ZERO);
    }

    public void seek(Duration duration) {
        MediaPlayer mp = player.get();
        if (mp == null) {
            return;
        }
        // mp.pause();
        mp.seek(duration);
        // mp.play();
    }

    public Duration getDuration() {
        MediaPlayer mp = player.get();
        if (mp == null) {
            return null;
        }
        return mp.getTotalDuration();
    }

    public void setOnReady(Runnable onReady) {
        MediaPlayer mp = player.get();
        if (mp == null) {
            return;
        }
        mp.setOnReady(onReady);
        this.onReady = onReady;
    }

    public void removePlaylistEntry(Path path) {
        files.remove(path);
        if (currentFileProperty().get().equals(path)) {
            stop();
            currentFileProperty().setValue(null);
        }
    }

    private void stopAndDispose(MediaPlayer mp) {
        mp.stop();
        mp.dispose();
        player.setValue(null);
    }

    public void next() {
        if (files.isEmpty()) {
            currentIndex = -1;
            currentFile.setValue(null);
            return;
        }
        currentIndex = currentIndex + 1;
        if (currentIndex >= files.size()) {
            currentIndex = 0;
        }
        currentFile.setValue(files.get(currentIndex));
    }

    public void prev() {
        if (files.isEmpty()) {
            currentIndex = -1;
            currentFile.setValue(null);
            return;
        }
        currentIndex = currentIndex - 1;
        if (currentIndex < 0) {
            currentIndex = files.size() - 1;
        }
        currentFile.setValue(files.get(currentIndex));
    }

    public Path getCurrentFile() {
        return currentFile.get();
    }

    public ObjectProperty<Path> currentFileProperty() {
        return currentFile;
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public BooleanProperty hasPrevProperty() {
        return hasPrev;
    }

    public BooleanProperty hasNextProperty() {
        return hasNext;
    }

    public ListProperty<Path> filesProperty() {
        return files;
    }

    public Duration getCurrentTime() {
        return currentTime.get();
    }

    public ObjectProperty<Duration> currentTimeProperty() {
        return currentTime;
    }

    public Duration getTotalDuration() {
        return totalDuration.get();
    }

}

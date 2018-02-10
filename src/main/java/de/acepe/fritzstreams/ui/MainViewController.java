package de.acepe.fritzstreams.ui;

import static de.acepe.fritzstreams.app.Fragments.*;
import static de.acepe.fritzstreams.backend.OnDemandStream.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.backend.OnDemandStream.Stream.SOUNDGARDEN;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.app.StreamInfoFactory;
import de.acepe.fritzstreams.backend.LiveStream;
import de.acepe.fritzstreams.backend.OnDemandStream;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class MainViewController implements ControlledScreen {
    private static final Logger LOG = LoggerFactory.getLogger(MainViewController.class);

    private static final int NUM_THREADS = 8;
    private static final int DAYS_PAST = 7;
    private static final DateTimeFormatter DAY_OF_WEEK = DateTimeFormatter.ofPattern("E").withLocale(Locale.GERMANY);
    private static final ZoneId ZONE_BERLIN = ZoneId.of("Europe/Berlin");

    private final List<Task<Void>> initTasks = new ArrayList<>(NUM_THREADS);
    private final BiMap<ToggleButton, LocalDate> toggleDayMap = HashBiMap.create();
    private final Map<LocalDate, OnDemandStream> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, OnDemandStream> nightflightStreamMap = new HashMap<>();
    // private final ObjectProperty<LocalDate> selectedDay = new SimpleObjectProperty<>();
    private final ScreenManager screenManager;
    private final StreamInfoFactory streamInfoFactory;
    private final LiveStream liveStream;

    private StreamFragmentController soundgardenView;
    private StreamFragmentController nightflightView;
    private LiveFragmentController liveStreamView;

    @FXML
    private ToggleGroup daysToggleGroup;
    @FXML
    private ToggleButton liveButton;
    @FXML
    private VBox streamList;
    @FXML
    private Button settingsButton;
    @FXML
    private VBox playerControlsContainer;

    @Inject
    public MainViewController(ScreenManager screenManager, StreamInfoFactory streamInfoFactory, LiveStream liveStream) {
        this.screenManager = screenManager;
        this.streamInfoFactory = streamInfoFactory;
        this.liveStream = liveStream;
    }

    @FXML
    private void initialize() {
        GlyphsDude.setIcon(settingsButton, FontAwesomeIcon.COG, "1.5em");

        liveStreamView = screenManager.loadFragment(LIVE_STREAM);
        soundgardenView = screenManager.loadFragment(ONDEMAND_STREAM);
        nightflightView = screenManager.loadFragment(ONDEMAND_STREAM);
        streamList.getChildren().setAll(soundgardenView.getContent(), nightflightView.getContent());

        PlayerController playerController = screenManager.loadFragment(PLAYER);
        playerControlsContainer.getChildren().addAll(playerController.getContent());

        LocalDate startDay = LocalDate.now();
        ObservableList<Toggle> toggles = daysToggleGroup.getToggles();

        for (int i = 0; i <= DAYS_PAST; i++) {
            LocalDate date = startDay.minusDays(i);
            ToggleButton toggle = (ToggleButton) toggles.get(DAYS_PAST - i);
            toggle.setText(date.format(DAY_OF_WEEK));
            toggleDayMap.put(toggle, date);

            soundgardenStreamMap.put(date, streamInfoFactory.create(date, SOUNDGARDEN));
            nightflightStreamMap.put(date, streamInfoFactory.create(date, NIGHTFLIGHT));
        }

        daysToggleGroup.selectedToggleProperty().addListener((obs, ov, toggle) -> {
            if (toggle == liveButton) {
                streamList.getChildren().setAll(liveStreamView.getContent());
            } else {
                streamList.getChildren().setAll(soundgardenView.getContent(), nightflightView.getContent());
                // noinspection SuspiciousMethodCalls
                LocalDate selectedDay = toggleDayMap.get(toggle);
                soundgardenView.streamProperty().setValue(soundgardenStreamMap.get(selectedDay));
                nightflightView.streamProperty().setValue(nightflightStreamMap.get(selectedDay));
            }
        });

        scheduleInitThreads();
        daysToggleGroup.selectToggle(liveButton);
    }

    private void scheduleInitThreads() {
        for (int i = 0; i < NUM_THREADS; i++) {
            int finalI = i;
            Task<Void> initTask = new Task<Void>() {
                @Override
                protected Void call() {
                    return initStreams(this, finalI);
                }

                @Override
                protected void done() {
                    initTasks.remove(this);
                }
            };
            initTasks.add(initTask);
            Thread initThread = new Thread(initTask);
            initThread.setName("Init-Stream-Thread");
            initThread.start();

        }
    }

    private boolean isTodayBeforeSoundgardenRelease(LocalDate date) {
        LocalDateTime todayAt2200InGermanTime = LocalDateTime.now(ZONE_BERLIN).withHour(22).withMinute(0);
        LocalDateTime nowInGermanTime = LocalDateTime.now(ZONE_BERLIN);
        return date.isEqual(LocalDate.now()) && nowInGermanTime.isBefore(todayAt2200InGermanTime);
    }

    private Void initStreams(Task<Void> task, int threadNr) {
        LocalDateTime started = LocalDateTime.now();
        List<LocalDate> values = toggleDayMap.values()
                                             .stream()
                                             .sorted(Comparator.reverseOrder())
                                             .collect(Collectors.toList());
        liveStream.init();

        for (int i = threadNr; i < values.size(); i += NUM_THREADS) {
            LocalDate day = values.get(i);
            if (task.isCancelled()) {
                return null;
            }
            OnDemandStream soundgarden = soundgardenStreamMap.get(day);
            if (isTodayBeforeSoundgardenRelease(day)) {
                Platform.runLater(() -> soundgarden.titleProperty().setValue("Noch nicht verfügbar"));
            } else {
                soundgarden.init();
            }
            OnDemandStream nightflight = nightflightStreamMap.get(day);
            nightflight.init();

            toggleDayMap.inverse().get(day).disableProperty().setValue(false);
        }
        LOG.debug("Initializing streams took: {} seconds", ChronoUnit.SECONDS.between(started, LocalDateTime.now()));
        return null;
    }

    public void stop() {
        // reverse order, so we don't run into concurrent-modification exceptions, because tasks remove themselves from
        // the list
        for (int i = initTasks.size() - 1; i >= 0; i--) {
            Task<Void> initTask = initTasks.get(i);
            if (initTask.isRunning()) {
                initTask.cancel();
            }
        }
        for (LocalDate day : toggleDayMap.values()) {
            stopDownload(soundgardenStreamMap.get(day));
            stopDownload(nightflightStreamMap.get(day));
        }
    }

    private void stopDownload(OnDemandStream onDemandStream) {
        if (onDemandStream != null) {
            onDemandStream.cancelDownload();
        }
    }

    @FXML
    void onSettingsPerformed() {
        screenManager.setScreen(Screens.SETTINGS, ScreenManager.Direction.LEFT);
    }

}

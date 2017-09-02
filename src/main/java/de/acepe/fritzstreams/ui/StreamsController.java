package de.acepe.fritzstreams.ui;

import static de.acepe.fritzstreams.backend.stream.StreamInfo.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.backend.stream.StreamInfo.Stream.SOUNDGARDEN;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.Screens;
import de.acepe.fritzstreams.backend.stream.StreamInfo;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class StreamsController implements ControlledScreen {

    private static final int DAYS_PAST = 7;
    private static final DateTimeFormatter DAY_OF_WEEK = DateTimeFormatter.ofPattern("E").withLocale(Locale.GERMANY);

    private final BiMap<ToggleButton, LocalDate> toggleDayMap = HashBiMap.create();
    private final Map<LocalDate, StreamInfo> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, StreamInfo> nightflightStreamMap = new HashMap<>();
    private final ObjectProperty<LocalDate> selectedDay = new SimpleObjectProperty<>();

    private StreamController soundgardenView;
    private StreamController nightflightView;
    private Task<Void> initTask;

    @FXML
    private ToggleGroup daysToggleGroup;
    @FXML
    private VBox streamList;
    @FXML
    private Button settingsButton;
    @FXML
    private VBox playerControlsContainer;

    private ScreenManager screenManager;
    private PlayerController playerController;

    @FXML
    private void initialize() {
        playerController = new PlayerController();
        playerControlsContainer.getChildren().addAll(playerController);

        soundgardenView = new StreamController();
        nightflightView = new StreamController();

        streamList.getChildren().add(soundgardenView);
        streamList.getChildren().add(nightflightView);

        LocalDate startDay = LocalDate.now();
        ObservableList<Toggle> toggles = daysToggleGroup.getToggles();
        for (int i = 0; i <= DAYS_PAST; i++) {
            LocalDate date = startDay.minusDays(i);
            ToggleButton toggle = (ToggleButton) toggles.get(DAYS_PAST - i);
            toggle.setText(date.format(DAY_OF_WEEK));
            toggleDayMap.put(toggle, date);

            soundgardenStreamMap.put(date, new StreamInfo(date, SOUNDGARDEN));
            nightflightStreamMap.put(date, new StreamInfo(date, NIGHTFLIGHT));
        }

        daysToggleGroup.selectedToggleProperty().addListener((oldSelectedDay, oldValue, selectedDay) -> {
            if (selectedDay != null) {
                // noinspection SuspiciousMethodCalls
                this.selectedDay.set(toggleDayMap.get(selectedDay));
            } else {
                daysToggleGroup.selectToggle(oldValue);
            }
        });

        selectedDay.addListener((observable, oldValue, selectedDate) -> {
            selectToggleForDay(selectedDate);
            soundgardenView.streamProperty().setValue(soundgardenStreamMap.get(selectedDate));
            nightflightView.streamProperty().setValue(nightflightStreamMap.get(selectedDate));
        });

        GlyphsDude.setIcon(settingsButton, FontAwesomeIcon.COG, "1.5em");

        initTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                return initStreams(this);
            }
        };
        new Thread(initTask).start();

        selectedDay.setValue(startDay);
    }

    private boolean isTodayBeforeSoundgardenRelease(LocalDate date) {
        LocalDateTime todayAt2200InGermanTime = LocalDateTime.now(ZoneId.of("Europe/Berlin"))
                                                             .withHour(22)
                                                             .withMinute(0);
        LocalDateTime nowInGermanTime = LocalDateTime.now(ZoneId.of("Europe/Berlin"));
        return date.isEqual(LocalDate.now()) && nowInGermanTime.isBefore(todayAt2200InGermanTime);
    }

    private Void initStreams(Task<Void> task) {
        Collection<LocalDate> values = toggleDayMap.values()
                                                   .stream()
                                                   .sorted(Comparator.reverseOrder())
                                                   .collect(Collectors.toList());

        for (LocalDate day : values) {
            if (task.isCancelled()) {
                return null;
            }
            if (!isTodayBeforeSoundgardenRelease(day)) {
                StreamInfo soundgarden = soundgardenStreamMap.get(day);
                soundgarden.init();
            }
            StreamInfo nightflight = nightflightStreamMap.get(day);
            nightflight.init();

            toggleDayMap.inverse().get(day).disableProperty().setValue(false);
        }
        return null;
    }

    private void selectToggleForDay(LocalDate selectedDate) {
        toggleDayMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(selectedDate))
                    .forEach(entry -> entry.getKey().setSelected(true));
    }

    public void stop() {
        if (initTask.isRunning()) {
            initTask.cancel();
        }
        for (LocalDate day : toggleDayMap.values()) {
            stopDownload(soundgardenStreamMap.get(day));
            stopDownload(nightflightStreamMap.get(day));
        }
    }

    private void stopDownload(StreamInfo streamInfo) {
        if (streamInfo != null && streamInfo.getStreamDownloader() != null) {
            streamInfo.getStreamDownloader().cancel();
        }
    }

    @FXML
    void onSettingsPerformed() {
        screenManager.setScreen(Screens.SETTINGS, ScreenManager.Direction.LEFT);
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
        soundgardenView.setScreenManager(screenManager);
        nightflightView.setScreenManager(screenManager);
    }
}

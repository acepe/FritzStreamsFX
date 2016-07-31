package de.acepe.fritzstreams;

import static de.acepe.fritzstreams.StreamInfo.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.StreamInfo.Stream.SOUNDGARDEN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class StreamsController implements ControlledScreen {
    private static final Logger LOG = LoggerFactory.getLogger(StreamsController.class);

    private static final int DAYS_PAST = 6;
    private static final DateTimeFormatter DAY_OF_WEEK = DateTimeFormatter.ofPattern("E").withLocale(Locale.GERMANY);

    private final BiMap<ToggleButton, LocalDate> toggleDayMap = HashBiMap.create();
    private final Map<LocalDate, StreamInfo> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, StreamInfo> nightflightStreamMap = new HashMap<>();
    private final ObjectProperty<LocalDate> selectedDay = new SimpleObjectProperty<>();

    private StreamView soundgardenView;
    private StreamView nightflightView;
    private Task<Void> initTask;

    @FXML
    private ToggleGroup daysToggleGroup;
    @FXML
    private VBox streamList;
    @FXML
    private Button settingsButton;
    @FXML
    private Node streamView;

    private ScreenManager screenManager;

    @FXML
    private void initialize() {
        soundgardenView = new StreamView();
        nightflightView = new StreamView();

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

    private Void initStreams(Task<Void> task) {
        Collection<LocalDate> values = toggleDayMap.values()
                                                   .stream()
                                                   .sorted((o1, o2) -> o2.compareTo(o1))
                                                   .collect(Collectors.toList());

        for (LocalDate day : values) {
            if (task.isCancelled()) {
                return null;
            }
            StreamInfo soundgarden = soundgardenStreamMap.get(day);
            soundgarden.init();
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
        if (streamInfo != null && streamInfo.getDownloader() != null) {
            streamInfo.getDownloader().cancel();
        }
    }

    @FXML
    void onSettingsPerformed() {
        screenManager.setScreen(ScreenId.SETTINGS, ScreenManager.Direction.LEFT);
    }

    @Override
    public void setScreenParent(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}

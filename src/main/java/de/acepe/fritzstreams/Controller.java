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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Controller {

    private static final int DAYS_PAST = 6;

    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final BiMap<ToggleButton, LocalDate> toggleDayMap = HashBiMap.create();
    private final Map<LocalDate, StreamInfo> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, StreamInfo> nightflightStreamMap = new HashMap<>();

    @FXML
    private ToggleGroup daysToggleGroup;
    @FXML
    private VBox streamList;

    private StreamView soundgardenView;
    private StreamView nightflightView;

    @FXML
    private void initialize() {
        soundgardenView = new StreamView();
        // soundgardenView.setDownloadConsumer(this::downloadMP3);

        nightflightView = new StreamView();
        // nightflightView.setDownloadConsumer(this::downloadMP3);

        streamList.getChildren().add(soundgardenView);
        streamList.getChildren().add(nightflightView);

        LocalDate startDay = LocalDate.now();
        ObservableList<Toggle> toggles = daysToggleGroup.getToggles();
        for (int i = 0; i <= DAYS_PAST; i++) {
            LocalDate date = startDay.minusDays(i);
            ToggleButton toggle = (ToggleButton) toggles.get(DAYS_PAST - i);
            toggle.setText(date.format(DateTimeFormatter.ofPattern("E").withLocale(Locale.GERMANY)));
            toggleDayMap.put(toggle, date);

            soundgardenStreamMap.put(date, new StreamInfo(date, SOUNDGARDEN));
            nightflightStreamMap.put(date, new StreamInfo(date, NIGHTFLIGHT));
        }

        daysToggleGroup.selectedToggleProperty().addListener((oldSelectedDay, oldValue, selectedDay) -> {
            if (selectedDay != null) {
                // noinspection SuspiciousMethodCalls
                date.set(toggleDayMap.get(selectedDay));
            } else {
                daysToggleGroup.selectToggle(oldValue);
            }
        });

        date.addListener((observable, oldValue, selectedDate) -> {
            soundgardenView.clear();
            nightflightView.clear();

            selectToggleForDay(selectedDate);
            soundgardenView.streamProperty().setValue(soundgardenStreamMap.get(selectedDate));
            nightflightView.streamProperty().setValue(nightflightStreamMap.get(selectedDate));
        });

        Task<Void> initTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Collection<LocalDate> values = toggleDayMap.values().stream().sorted().collect(Collectors.toList());

                for (LocalDate day : values) {
                    StreamInfo soundgarden = soundgardenStreamMap.get(day);
                    soundgarden.init();
                    StreamInfo nightflight = nightflightStreamMap.get(day);
                    nightflight.init();

                    toggleDayMap.inverse().get(day).disableProperty().setValue(false);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                if (date.get() == null) {
                    date.setValue(startDay);
                }
            }
        };
        new Thread(initTask).start();

    }

    private void selectToggleForDay(LocalDate selectedDate) {
        toggleDayMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(selectedDate))
                    .forEach(entry -> entry.getKey().setSelected(true));
    }

}

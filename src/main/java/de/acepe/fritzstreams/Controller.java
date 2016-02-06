package de.acepe.fritzstreams;

import static de.acepe.fritzstreams.StreamInfo.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.StreamInfo.Stream.SOUNDGARDEN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class Controller {

    private static final int DAYS_PAST = 6;

    @FXML
    private ToggleGroup daysGroup;
    @FXML
    private Label titleSoundgardenLabel;
    @FXML
    private Label subTitleSoundgardenLabel;
    @FXML
    private Button downloadSoundgardenButton;
    @FXML
    private Label titleNightflightLabel;
    @FXML
    private Label subTitleNightflightLabel;
    @FXML
    private Button downloadNightflightButton;
    @FXML
    private ImageView soundgardenImageView;
    @FXML
    private ImageView nightflightImageView;

    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final Map<Toggle, LocalDate> toggleDayMap = new HashMap<>();
    private final Map<LocalDate, StreamInfo> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, StreamInfo> nightflightStreamMap = new HashMap<>();

    @FXML
    private void initialize() {

        ArrayList<Label> labels = new ArrayList<>(4);
        labels.add(titleNightflightLabel);
        labels.add(titleSoundgardenLabel);
        labels.add(subTitleNightflightLabel);
        labels.add(subTitleSoundgardenLabel);

        downloadSoundgardenButton.disableProperty().bind(titleSoundgardenLabel.textProperty().isEmpty());
        downloadNightflightButton.disableProperty().bind(titleNightflightLabel.textProperty().isEmpty());

        LocalDate startDay = LocalDate.now();

        ObservableList<Toggle> toggles = daysGroup.getToggles();
        for (int i = 0; i <= DAYS_PAST; i++) {
            ToggleButton toggle = (ToggleButton) toggles.get(DAYS_PAST - i);
            LocalDate date = startDay.minusDays(i);
            toggleDayMap.put(toggle, date);
            toggle.setText(date.format(DateTimeFormatter.ofPattern("E").withLocale(Locale.GERMANY)));

            StreamInfo streamInfoSoundgarden = new StreamInfo(date, SOUNDGARDEN);
            soundgardenStreamMap.put(date, streamInfoSoundgarden);

            StreamInfo streamInfoNightflight = new StreamInfo(date, NIGHTFLIGHT);
            nightflightStreamMap.put(date, streamInfoNightflight);
        }

        daysGroup.selectedToggleProperty()
                 .addListener((observable, oldValue, newValue) -> date.set(toggleDayMap.get(newValue)));

        date.addListener((observable, oldValue, selectedDate) -> {
            labels.forEach(label -> label.setText(null));
            selectToggleForDay(selectedDate);

            StreamInfo soundgarden = soundgardenStreamMap.get(selectedDate);
            if (!soundgarden.getInitialised()) {
                soundgarden.init(succeeded -> setInfo(soundgarden));
            } else {
                setInfo(soundgarden);
            }

            StreamInfo nightflight = nightflightStreamMap.get(selectedDate);
            if (!nightflight.getInitialised()) {
                nightflight.init(succeeded -> setInfo(nightflight));
            } else {
                setInfo(nightflight);
            }
        });

        date.setValue(startDay);
    }

    private void selectToggleForDay(LocalDate selectedDate) {
        toggleDayMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(selectedDate))
                    .forEach(entry -> entry.getKey().setSelected(true));
    }

    private void setInfo(StreamInfo streamInfo) {
        StreamInfo.Stream stream = streamInfo.getStream();
        Label titleLabel = stream == NIGHTFLIGHT ? titleNightflightLabel : titleSoundgardenLabel;
        Label subtitleLabel = stream == NIGHTFLIGHT ? subTitleNightflightLabel : subTitleSoundgardenLabel;
        ImageView imageView = stream == NIGHTFLIGHT ? nightflightImageView : soundgardenImageView;

        titleLabel.setText(streamInfo.getTitle());
        subtitleLabel.setText(streamInfo.getSubtitle());
        imageView.setImage(streamInfo.getImage());
    }

    @FXML
    void onDownloadSoundgardenPerformed() {
        downloadMP3(soundgardenStreamMap.get(date.get()));
    }

    @FXML
    void onDownloadNightflightPerformed() {
        downloadMP3(nightflightStreamMap.get(date.get()));
    }

    private void downloadMP3(StreamInfo streamInfo) {
        Downloader downloader = new Downloader(streamInfo);

        // ProgressBar progress;
        // progress = streamInfo.getStream() == NIGHTFLIGHT ? progressNightflight : progressSoundgarden;
        // progress.progressProperty().bind(downloader.progressProperty());
        // progress.visibleProperty().bind(downloader.runningProperty());
        downloader.download();
    }

}

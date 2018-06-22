package de.acepe.fritzstreams.ui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.backend.StreamManager;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static de.acepe.fritzstreams.app.Fragments.*;
import static java.util.Locale.GERMANY;

public class MainViewController implements ControlledScreen {
    private static final Logger LOG = LoggerFactory.getLogger(MainViewController.class);
    private static final int DAYS_PAST = 7;
    private static final DateTimeFormatter DAY_OF_WEEK = DateTimeFormatter.ofPattern("E").withLocale(GERMANY);

    private final ScreenManager screenManager;
    private final StreamManager streamManager;
    private final BiMap<ToggleButton, LocalDate> toggleDayMap = HashBiMap.create();

    private OnDemandFragmentController soundgardenView;
    private OnDemandFragmentController nightflightView;
    private LiveFragmentController liveStreamView;
    private PlayerController playerController;

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
    public MainViewController(ScreenManager screenManager, StreamManager streamManager) {
        this.screenManager = screenManager;
        this.streamManager = streamManager;
    }

    @FXML
    private void initialize() {
        GlyphsDude.setIcon(settingsButton, FontAwesomeIcon.COG, "1.5em");
        initFragments();
        addFragments();
        initToggles();

        streamManager.registerInitCallback(this::onStreamInitialized);
    }

    private void initFragments() {
        liveStreamView = screenManager.loadFragment(LIVE_STREAM);
        soundgardenView = screenManager.loadFragment(ONDEMAND_STREAM);
        nightflightView = screenManager.loadFragment(ONDEMAND_STREAM);
        playerController = screenManager.loadFragment(PLAYER);
    }

    private void addFragments() {
        streamList.getChildren().setAll(soundgardenView.getContent(), nightflightView.getContent());
        playerControlsContainer.getChildren().addAll(playerController.getContent());
    }

    private void initToggles() {
        LocalDate startDay = LocalDate.now();
        ObservableList<Toggle> toggles = daysToggleGroup.getToggles();

        for (int i = 0; i <= DAYS_PAST; i++) {
            LocalDate date = startDay.minusDays(i);
            ToggleButton toggle = (ToggleButton) toggles.get(DAYS_PAST - i);
            toggle.setText(date.format(DAY_OF_WEEK));
            toggleDayMap.put(toggle, date);
        }
        updateToggles();

        daysToggleGroup.selectedToggleProperty().addListener(this::onSelectedToggleChanged);
        daysToggleGroup.selectToggle(liveButton);
    }

    private void onSelectedToggleChanged(ObservableValue<? extends Toggle> obs, Toggle ov, Toggle nv) {
        if (daysToggleGroup.getSelectedToggle() == null) {
            daysToggleGroup.selectToggle(ov);
            return;
        }
        if (nv == liveButton) {
            setLiveStreamView();
        } else {
            setOnDemandStreamViews(nv);
        }
    }

    private void setOnDemandStreamViews(Toggle nv) {
        streamList.getChildren()
                  .setAll(soundgardenView.getContent(), nightflightView.getContent());
        // noinspection SuspiciousMethodCalls
        LocalDate selectedDay = toggleDayMap.get(nv);
        soundgardenView.streamProperty().setValue(streamManager.getSoundgarden(selectedDay));
        nightflightView.streamProperty().setValue(streamManager.getNightflight(selectedDay));
    }

    private void setLiveStreamView() {
        Parent liveStreamContent = liveStreamView.getContent();
        streamList.getChildren().setAll(liveStreamContent);
        VBox.setVgrow(liveStreamContent, Priority.ALWAYS);
    }

    private void updateToggles() {
        toggleDayMap.forEach((toggleButton, date) -> toggleButton.setDisable(!streamManager.isInitialised(date)));
    }

    private void onStreamInitialized() {
        updateToggles();
    }

    @FXML
    void onSettingsPerformed() {
        screenManager.setScreen(Screens.SETTINGS, ScreenManager.Direction.LEFT);
    }

}

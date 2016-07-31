package de.acepe.fritzstreams.ui;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenId;
import de.acepe.fritzstreams.ScreenManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SettingsController implements ControlledScreen {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);
    private static final String NODE = "FritzStreams";
    private static final String DOWNLOAD_PATH = "download-path";

    @FXML
    private Label settingsLabel;
    @FXML
    private Button saveSettingsButton;
    @FXML
    private Button changeDownloadLocationButton;
    @FXML
    private TextField downloadLocationTextfield;

    private ScreenManager screenManager;
    private Preferences prefs;

    @FXML
    private void initialize() {
        prefs = Preferences.userRoot().node(NODE);
        String downloadPath = prefs.get(DOWNLOAD_PATH, System.getProperty("user.home"));
        downloadLocationTextfield.setText(downloadPath);
    }

    @FXML
    void onChangeDownloadLocationPerformed() {

    }

    @FXML
    void onSaveSettingsPerformed() {
        screenManager.setScreen(ScreenId.STREAMS, ScreenManager.Direction.RIGHT);
        try {
            prefs.put(DOWNLOAD_PATH, downloadLocationTextfield.getText());
            prefs.flush();
        } catch (BackingStoreException e) {
            LOG.error("Settings could not be persisted.");
        }
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}

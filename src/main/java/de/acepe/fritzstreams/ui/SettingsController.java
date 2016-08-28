package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenId;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.Settings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SettingsController implements ControlledScreen {

    @FXML
    private Label settingsLabel;
    @FXML
    private Button saveSettingsButton;
    @FXML
    private Button changeDownloadLocationButton;
    @FXML
    private TextField downloadLocationTextfield;

    private ScreenManager screenManager;
    private Settings settings;

    @FXML
    private void initialize() {
        settings = Settings.getInstance();
        downloadLocationTextfield.setText(settings.getTargetpath());
    }

    @FXML
    void onChangeDownloadLocationPerformed() {

    }

    @FXML
    void onSaveSettingsPerformed() {
        settings.setTargetpath(downloadLocationTextfield.getText());
        settings.persist();
        screenManager.setScreen(ScreenId.STREAMS, ScreenManager.Direction.RIGHT);
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}

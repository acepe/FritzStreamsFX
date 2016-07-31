package de.acepe.fritzstreams;

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

    @FXML
    private void initialize() {
    }

    @FXML
    void onChangeDownloadLocationPerformed() {

    }

    @FXML
    void onSaveSettingsPerformed() {
        screenManager.setScreen(ScreenId.STREAMS, ScreenManager.Direction.RIGHT);
    }

    @Override
    public void setScreenParent(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}

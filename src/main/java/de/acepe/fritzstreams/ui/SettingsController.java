package de.acepe.fritzstreams.ui;

import java.io.File;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenId;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.Settings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

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
        downloadLocationTextfield.setEditable(false);
    }

    @FXML
    void onChangeDownloadLocationPerformed() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Zielordner festlegen");
        chooser.setInitialDirectory(new File(settings.getTargetpath()));
        File selectedDir = chooser.showDialog(screenManager.getScene().getWindow());
        if (selectedDir != null) {
            downloadLocationTextfield.setText(selectedDir.getAbsolutePath());
        }
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

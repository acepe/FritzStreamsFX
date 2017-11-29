package de.acepe.fritzstreams.ui;

import java.io.File;

import javax.inject.Inject;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.Screens;
import de.acepe.fritzstreams.backend.Settings;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class SettingsController implements ControlledScreen {

    private final ScreenManager screenManager;
    private final Settings settings;

    @FXML
    private Label settingsLabel;
    @FXML
    private Button backButton;
    @FXML
    private Button changeDownloadLocationButton;
    @FXML
    private TextField downloadLocationTextfield;

    @Inject
    public SettingsController(Settings settings, ScreenManager screenManager) {
        this.settings = settings;
        this.screenManager = screenManager;
    }

    @FXML
    private void initialize() {
        GlyphsDude.setIcon(backButton, FontAwesomeIcon.CHEVRON_LEFT, "1.5em");

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
        screenManager.setScreen(Screens.STREAMS, ScreenManager.Direction.RIGHT);
    }

}

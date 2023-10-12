package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.app.ControlledScreen;
import de.acepe.fritzstreams.app.ScreenManager;
import de.acepe.fritzstreams.app.Screens;
import de.acepe.fritzstreams.backend.Settings;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.File;

public class SettingsController implements ControlledScreen {

    private final ScreenManager screenManager;
    private final Dialogs dialogs;
    private final Settings settings;

    @FXML
    private Button backButton;
    @FXML
    private TextField downloadLocationTextfield;

    @Inject
    public SettingsController(Settings settings, ScreenManager screenManager, Dialogs dialogs) {
        this.settings = settings;
        this.screenManager = screenManager;
        this.dialogs = dialogs;
    }

    @FXML
    private void initialize() {
        GlyphsDude.setIcon(backButton, FontAwesomeIcon.CHEVRON_LEFT, "1.5em");

        downloadLocationTextfield.setText(settings.getTargetpath());
        downloadLocationTextfield.setEditable(false);
    }

    @FXML
    void onChangeDownloadLocationPerformed() {
        File selectedDir = dialogs.showDirChooser();
        if (selectedDir != null) {
            downloadLocationTextfield.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    void onSaveSettingsPerformed() {
        settings.setTargetpath(downloadLocationTextfield.getText());
        settings.persist();
        screenManager.setScreen(Screens.MAIN, ScreenManager.Direction.RIGHT);
    }

}

package de.acepe.fritzstreams.ui;

import java.util.ArrayList;
import java.util.List;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.vk.VkAudioApi;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import de.acepe.fritzstreams.backend.vk.model.AudioSearchResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class VKDownloaderController implements ControlledScreen {

    private static final String RESULT_TEMPLATE = "Die Suche hat %d Audio-Dateien gefunden";

    private final List<AudioItem> audioItems = new ArrayList<>();

    @FXML
    private Label titleLabel;
    @FXML
    private TextField searchTextField;
    @FXML
    private Button searchButton;
    @FXML
    private Label resultsLabel;
    @FXML
    private VBox resultVBox;
    @FXML
    private VBox resultItemsVBox;
    @FXML
    private ProgressBar downloadProgress;
    private ScreenManager screenManager;

    @FXML
    void initialize() {
        searchButton.disableProperty().bind(searchTextField.textProperty().isEmpty());
        resultVBox.setVisible(false);
    }

    @FXML
    void onSearchPerformed() {
        AudioSearchResponse audioSearchResponse = VkAudioApi.with(Settings.APP_ID, Settings.PREFERENCES_ROOT)
                                                            .searchAudio(searchTextField.getText(), 100);
        List<AudioItem> foundItems = audioSearchResponse.getItems();

        audioItems.clear();
        audioItems.addAll(foundItems);

        resultVBox.setVisible(true);
        resultsLabel.setText(String.format(RESULT_TEMPLATE, foundItems.size()));
        populateResultList(foundItems);
    }

    private void populateResultList(List<AudioItem> audioItems) {
        resultItemsVBox.getChildren().clear();
        audioItems.forEach(this::addAudioItem);
    }

    private void addAudioItem(AudioItem audioItem) {
        AudioItemController audioItemController = new AudioItemController();
        audioItemController.setAudioItem(audioItem);
        audioItemController.setApplication(screenManager.getApplication());
        resultItemsVBox.getChildren().add(audioItemController);
    }

    public void setSearchText(String searchText) {
        searchTextField.setText(searchText);
    }

    @Override
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }
}

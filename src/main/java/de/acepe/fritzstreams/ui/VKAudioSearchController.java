package de.acepe.fritzstreams.ui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.ScreenManager;
import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.backend.vk.VkAudioApi;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import de.acepe.fritzstreams.backend.vk.model.AudioSearchResponse;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class VKAudioSearchController implements ControlledScreen {

    private static final String RESULT_TEMPLATE = "Die Suche hat %d Audio-Dateien gefunden";
    private static final String ARTIST_TITLE = "Künstler und Titel";
    private static final String ONLY_ARTIST = "nur Künstler";

    private final Settings settings;
    private final ScreenManager screenManager;
    private final DownloadManager downloadManager;

    @FXML
    private Label titleLabel;
    @FXML
    private TextField searchTextField;
    @FXML
    private ComboBox<String> searchCategoryCombo;
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
    @FXML
    private HBox downloadsHBox;

    @Inject
    public VKAudioSearchController(Settings settings, ScreenManager screenManager, DownloadManager downloadManager) {
        this.settings = settings;
        this.screenManager = screenManager;
        this.downloadManager = downloadManager;
    }

    @FXML
    void initialize() {
        searchCategoryCombo.setItems(FXCollections.observableArrayList(ARTIST_TITLE, ONLY_ARTIST));
        searchCategoryCombo.getSelectionModel().select(0);

        searchButton.disableProperty().bind(searchTextField.textProperty().isEmpty());
        resultVBox.setVisible(false);
        downloadProgress.progressProperty().bind(downloadManager.progressProperty());
        downloadsHBox.visibleProperty()
                     .bind(downloadManager.progressProperty()
                                          .greaterThan(0)
                                          .and(downloadManager.progressProperty().lessThan(1)));
    }

    @FXML
    void onSearchPerformed() {
        boolean performerOnly = searchCategoryCombo.getSelectionModel().getSelectedItem().equals(ONLY_ARTIST);

        Task<List<AudioItem>> searchTask = new Task<List<AudioItem>>() {
            @Override
            protected List<AudioItem> call() throws Exception {
                AudioSearchResponse response = VkAudioApi.with(Settings.APP_ID, Settings.PREFERENCES_ROOT)
                                                         .searchAudio(searchTextField.getText(), 300, performerOnly);
                if (response == null) {
                    return new ArrayList<>(0);
                }
                return response.getItems();
            }
        };
        searchTask.valueProperty().addListener((observable, oldValue, foundItems) -> {
            resultVBox.setVisible(true);
            resultsLabel.setText(String.format(RESULT_TEMPLATE, foundItems.size()));
            populateResultList(foundItems);
        });

        new Thread(searchTask).start();
    }

    private void populateResultList(List<AudioItem> audioItems) {
        resultItemsVBox.getChildren().clear();
        audioItems.forEach(this::addAudioItem);
    }

    private void addAudioItem(AudioItem audioItem) {
        // FIXME: inject
        AudioItemController audioItemController = new AudioItemController(settings, downloadManager);
        audioItemController.setAudioItem(audioItem);
        audioItemController.setApplication(screenManager.getApplication());
        resultItemsVBox.getChildren().add(audioItemController);
    }

    public void setSearchText(String searchText) {
        searchTextField.setText(searchText);
    }

}

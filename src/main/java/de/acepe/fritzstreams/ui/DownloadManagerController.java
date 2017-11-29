package de.acepe.fritzstreams.ui;

import java.util.List;

import de.acepe.fritzstreams.ControlledScreen;
import de.acepe.fritzstreams.backend.download.DownloadManager;
import de.acepe.fritzstreams.backend.vk.VKDownload;
import de.acepe.fritzstreams.backend.vk.model.AudioItem;
import de.acepe.fritzstreams.util.ToStringConverter;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;

public class DownloadManagerController implements ControlledScreen {

    @FXML
    private Label titleLabel;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label runningLabel;
    @FXML
    private VBox resultVBox;
    @FXML
    private ListView<VKDownload> pendingList;
    @FXML
    private ListView<VKDownload> runningList;
    @FXML
    private ListView<VKDownload> doneList;
    @FXML
    private Label doneLabel;
    @FXML
    private Label countLabel;
    @FXML
    private ProgressBar downloadProgress;
    @FXML
    private VBox downloadsVBox;

    private DownloadManager downloadManager;

    @FXML
    void initialize() {
        downloadManager = DownloadManager.getInstance();
        pendingList.setItems(downloadManager.getPendingList());
        runningList.setItems(downloadManager.getRunningList());
        doneList.setItems(downloadManager.getFinishedList());

        pendingList.setCellFactory(param -> new DowloadListCell());
        runningList.setCellFactory(param -> new DowloadListCell());
        doneList.setCellFactory(param -> new DowloadListCell());
        runningLabel.textProperty()
                    .bindBidirectional(downloadManager.runningProperty(),
                                       new ToStringConverter<>(running -> running ? "aktiv" : "gestoppt"));

        doneLabel.textProperty().bind(downloadManager.doneCountProperty().asString());
        countLabel.textProperty().bind(downloadManager.countProperty().asString());
        downloadProgress.progressProperty().bind(downloadManager.progressProperty());

        ObservableList<VKDownload> downloadList = downloadManager.getDownloadList();
        populateResultList(downloadList);
        downloadList.addListener((ListChangeListener<VKDownload>) c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(DownloadManagerController.this::addDownloadItem);
            }
        });
    }

    private void populateResultList(List<VKDownload> downloads) {
        downloadsVBox.getChildren().clear();
        downloads.forEach(this::addDownloadItem);
    }

    private void addDownloadItem(VKDownload download) {
        DownloadItemController downloadItemController = new DownloadItemController();
        downloadItemController.setDownload(download);
        downloadsVBox.getChildren().add(downloadItemController);
    }

    @FXML
    void onStartPerformed() {
        downloadManager.startDownloads();
    }

    @FXML
    void onStopPerformed() {
        downloadManager.stopDownloads();
    }

    private class DowloadListCell extends TextFieldListCell<VKDownload> {
        @Override
        public void updateItem(VKDownload download, boolean empty) {
            super.updateItem(download, empty);

            if (download != null && !empty) {
                AudioItem audioItem = download.getAudioItem();
                String text = audioItem.getArtist() + "-" + audioItem.getTitle();
                setText(text);
            } else {
                setText("");
            }
        }
    }
}

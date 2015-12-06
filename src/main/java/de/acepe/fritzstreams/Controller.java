package de.acepe.fritzstreams;

import static de.acepe.fritzstreams.StreamInfo.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.StreamInfo.Stream.SOUNDGARDEN;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class Controller {

    @FXML
    private DatePicker dateChooser;

    @FXML
    private Label titleSoundgardenLabel;

    @FXML
    private Label subtitleSoundgardenLabel;

    @FXML
    private ProgressBar progressSoundgarden;

    @FXML
    private Button downloadSoundgardenButton;

    @FXML
    private Label titleNightflightLabel;

    @FXML
    private Label subtitleNightflightLabel;

    @FXML
    private ProgressBar progressNightflight;

    @FXML
    private Button downloadNightflightButton;

    private StreamInfo streamInfoNightflight;
    private StreamInfo streamInfoSoundgarden;

    @FXML
    private void initialize() {
        ArrayList<Label> labels = new ArrayList<>(4);
        labels.add(titleNightflightLabel);
        labels.add(titleSoundgardenLabel);
        labels.add(subtitleNightflightLabel);
        labels.add(subtitleSoundgardenLabel);

        progressSoundgarden.setVisible(false);
        progressNightflight.setVisible(false);
        downloadSoundgardenButton.disableProperty().bind(titleSoundgardenLabel.textProperty().isEmpty());
        downloadNightflightButton.disableProperty().bind(titleNightflightLabel.textProperty().isEmpty());

        dateChooser.valueProperty().addListener((observable, oldValue, newValue) -> {
            labels.forEach(label -> label.setText(null));

            streamInfoNightflight = new StreamInfo(newValue, NIGHTFLIGHT);
            streamInfoNightflight.init(succeeded -> setInfo(streamInfoNightflight));

            streamInfoSoundgarden = new StreamInfo(newValue, SOUNDGARDEN);
            streamInfoSoundgarden.init(succeeded -> setInfo(streamInfoSoundgarden));
        });

        LocalDate startDay = LocalDate.now().minusDays(1);
        dateChooser.valueProperty().setValue(startDay);
    }

    private void setInfo(StreamInfo streamInfo) {
        StreamInfo.Stream stream = streamInfo.getStream();
        Label titleLabel = stream == NIGHTFLIGHT ? titleNightflightLabel : titleSoundgardenLabel;
        Label subtitleLabel = stream == NIGHTFLIGHT ? subtitleNightflightLabel : subtitleSoundgardenLabel;

        titleLabel.setText(streamInfo.getTitle());
        subtitleLabel.setText(streamInfo.getSubtitle());
    }

    @FXML
    void onDownloadSoundgardenPerformed() {
        downloadMP3(streamInfoSoundgarden);
    }

    @FXML
    void onDownloadNightflightPerformed() {
        downloadMP3(streamInfoNightflight);
    }

    private void downloadMP3(StreamInfo streamInfo) {
        int size;

        try {
            URLConnection connection = new URL(streamInfo.getStreamURL()).openConnection();
            size = connection.getContentLength();

            System.out.println("size: " + size);
        } catch (IOException e) {
            return;
        }

        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, size);

                String userHome = System.getProperty("user.home");

                String pathname = userHome
                                  + File.separator
                                  + streamInfo.getTitle()
                                  + "_"
                                  + streamInfo.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                  + ".mp3";
                File file = new File(pathname);
                try (InputStream is = new URL(streamInfo.getStreamURL()).openConnection().getInputStream();
                     OutputStream outstream = new FileOutputStream(file)) {

                    byte[] buffer = new byte[4096];
                    int downloaded = 0;
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        if (isCancelled()) {
                            break;
                        }
                        downloaded += len;
                        updateProgress(downloaded, size);
                        outstream.write(buffer, 0, len);
                    }
                    outstream.close();
                } catch (IOException e) {
                    throw new Exception(e);
                }
                return null;
            }
        };
        ProgressBar progress;
        progress = streamInfo.getStream() == NIGHTFLIGHT ? progressNightflight : progressSoundgarden;
        progress.progressProperty().bind(downloadTask.progressProperty());
        progress.visibleProperty().bind(downloadTask.runningProperty());
        new Thread(downloadTask).start();

    }

}

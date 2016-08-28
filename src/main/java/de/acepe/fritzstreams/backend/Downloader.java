package de.acepe.fritzstreams.backend;

import static javafx.scene.layout.GridPane.setHgrow;
import static javafx.scene.layout.GridPane.setVgrow;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class Downloader {
    private static final Logger LOG = LoggerFactory.getLogger(Downloader.class);

    private final StreamInfo streamInfo;

    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private Task<Void> downloadTask;
    private File targetFile;

    public Downloader(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public void download() {
        targetFile = new File(streamInfo.getDownloadFileName());

        downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                URLConnection connection = new URL(streamInfo.getStreamURL()).openConnection();
                try (InputStream is = connection.getInputStream();
                        OutputStream outstream = new FileOutputStream(targetFile)) {

                    final int size = connection.getContentLength();
                    updateProgress(0, size);

                    byte[] buffer = new byte[4096];
                    int downloadedSum = 0;
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        if (isCancelled()) {
                            break;
                        }
                        downloadedSum += len;
                        updateProgress(downloadedSum, size);
                        outstream.write(buffer, 0, len);
                    }
                    outstream.close();
                } catch (IOException e) {
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                streamInfo.setDownloadedFile(targetFile);
            }

            @Override
            protected void failed() {
                LOG.error("Failed to download stream {}", streamInfo, getException());
                Throwable ex = getException();
                showErrorDialog(ex);
            }

            @Override
            protected void cancelled() {
                LOG.info("Download was cancelled, deleting partial download.");
                if (targetFile.exists()) {
                    targetFile.delete();
                }
            }
        };
        progress.bind(downloadTask.progressProperty());
        runningProperty().bind(downloadTask.runningProperty());
        new Thread(downloadTask).start();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    private void showErrorDialog(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        String title = "Download fehlgeschlagen";
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(title + ". Grund: " + ex.getMessage());
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(800);

        Label label = new Label("Fehlermeldung:");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        setVgrow(textArea, Priority.ALWAYS);
        setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        // workaround for https://bugs.openjdk.java.net/browse/JDK-8095777
        // dialog doesn't resize if expandable is collapsed
        alert.getDialogPane().expandedProperty().addListener((l) -> {
            Platform.runLater(() -> {
                alert.getDialogPane().requestLayout();
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.sizeToScene();
            });
        });
        alert.showAndWait();
    }

}

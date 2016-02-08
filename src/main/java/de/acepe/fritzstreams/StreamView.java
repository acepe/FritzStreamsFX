package de.acepe.fritzstreams;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;

public class StreamView extends HBox {

    @FXML
    private ImageView imageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subTitleLabel;
    @FXML
    private StackPane downloadStackPane;
    @FXML
    private Button downloadButton;
    @FXML
    private ProgressBar downloadProgress;

    private ObjectProperty<StreamInfo> stream = new SimpleObjectProperty<>();

    public StreamView() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("stream_view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    private void initialize() {
        downloadButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            StreamInfo streamInfo = stream.get();
            return streamInfo == null || !streamInfo.getInitialised();
        } , stream));

        GlyphsDude.setIcon(downloadButton, MaterialDesignIcon.DOWNLOAD, "1.5em");

        streamProperty().addListener((observable, oldValue, newValue) -> {
            unbindDownloader();
            if (newValue == null) {
                clear();
                return;
            }
            titleProperty().setValue(newValue.getTitle());
            subTitleProperty().setValue(newValue.getSubtitle());
            imageProperty().setValue(newValue.getImage());
            bindDownloader();
        });
    }

    public void clear() {
        titleLabel.textProperty().setValue(null);
        subTitleLabel.textProperty().setValue(null);
    }

    public StringProperty titleProperty() {
        return titleLabel.textProperty();
    }

    public StringProperty subTitleProperty() {
        return subTitleLabel.textProperty();
    }

    public ObjectProperty<Image> imageProperty() {
        return imageView.imageProperty();
    }

    public ObjectProperty<StreamInfo> streamProperty() {
        return stream;
    }

    @FXML
    void onDownloadPerformed() {
        stream.get().download();
        bindDownloader();
    }

    private void unbindDownloader() {
        downloadProgress.progressProperty().unbind();
        downloadProgress.visibleProperty().unbind();
        downloadButton.disableProperty().unbind();
    }

    private void bindDownloader() {
        Downloader downloader = stream.get().getDownloader();
        if (downloader == null) {
            downloadProgress.setVisible(false);
            downloadButton.disableProperty().setValue(false);
            return;
        }
        downloadProgress.progressProperty().bind(downloader.progressProperty());
        downloadProgress.visibleProperty().bind(downloader.runningProperty());
        downloadButton.disableProperty().bind(downloader.runningProperty());
    }

}

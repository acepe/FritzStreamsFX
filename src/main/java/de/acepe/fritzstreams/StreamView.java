package de.acepe.fritzstreams;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

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
    private Button downloadButton;

    private ObjectProperty<StreamInfo> stream = new SimpleObjectProperty<>();
    private Consumer<StreamInfo> streamInfoConsumer;

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
            titleProperty().setValue(newValue.getTitle());
            subTitleProperty().setValue(newValue.getSubtitle());
            imageProperty().setValue(newValue.getImage());
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

    public void setDownloadConsumer(Consumer<StreamInfo> streamInfoConsumer) {
        this.streamInfoConsumer = streamInfoConsumer;
    }

    @FXML
    void onDownloadPerformed() {
        streamInfoConsumer.accept(stream.get());
    }
}

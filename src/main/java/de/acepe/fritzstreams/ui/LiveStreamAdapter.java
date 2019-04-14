package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.backend.LiveStream;
import de.acepe.fritzstreams.backend.StreamManager;
import de.acepe.fritzstreams.backend.StreamMetaData;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.image.Image;

import javax.inject.Inject;
import java.nio.file.Path;

public class LiveStreamAdapter {
    private final StringProperty onAirTitle = new SimpleStringProperty();
    private final StringProperty onAirArtist = new SimpleStringProperty();
    private final ObjectProperty<Image> onAirImage = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();
    private final ObjectProperty<Path> tmpFile = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty();

    private final LiveStream livestream;

    @Inject
    public LiveStreamAdapter(StreamManager streamManager) {
        livestream = streamManager.getLiveStream();
        livestream.registerUICallbacks(this::onMetaDataUpdated, this::onPlayingChanged);
    }

    public void onMetaDataUpdated(StreamMetaData metaData) {
        if (metaData == null) {
            return;
        }
        Platform.runLater(() -> {
            title.setValue(metaData.getTitle());
            subtitle.setValue(metaData.getSubtitle());
            image.setValue(metaData.getImage());
            onAirArtist.setValue(metaData.getOnAirArtist());
            onAirTitle.setValue(metaData.getOnAirTitle());
            onAirImage.setValue(metaData.getOnAirImage());
            initialised.setValue(metaData.getTitle() != null);
        });
    }

    public void onPlayingChanged() {
        Platform.runLater(() -> {
            Path tmpFileLieveStream = livestream.getTmpFile();
            tmpFile.setValue(tmpFileLieveStream);
            playingProperty().set(tmpFileLieveStream != null);
        });
    }

    public void play() {
        livestream.play();
        playingProperty().set(true);
    }

    public void stop() {
        livestream.stop();
        playingProperty().set(false);
    }

    public ObjectProperty<Path> tmpFileProperty() {
        return tmpFile;
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty subtitleProperty() {
        return subtitle;
    }

    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public StringProperty onAirTitleProperty() {
        return onAirTitle;
    }

    public StringProperty onAirArtistProperty() {
        return onAirArtist;
    }

    public ObjectProperty<Image> onAirImageProperty() {
        return onAirImage;
    }

}

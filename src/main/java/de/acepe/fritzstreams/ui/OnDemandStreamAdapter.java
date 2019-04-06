package de.acepe.fritzstreams.ui;

import de.acepe.fritzstreams.backend.DownloadManager;
import de.acepe.fritzstreams.backend.DownloadTask;
import de.acepe.fritzstreams.backend.OnDemandStream;
import de.acepe.fritzstreams.backend.Playlist;
import javafx.beans.property.*;
import javafx.scene.image.Image;

import javax.inject.Inject;
import java.io.File;

public class OnDemandStreamAdapter {

    private final ObjectProperty<File> downloadedFile = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty downloading = new SimpleBooleanProperty();
    private final DownloadManager downloadManager;

    @SuppressWarnings("FieldHasSetterButNoGetter")
    private OnDemandStream onDemandStream;

    @Inject
    public OnDemandStreamAdapter(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void setOnDemandStream(OnDemandStream onDemandStream) {
        this.onDemandStream = onDemandStream;
        if (!onDemandStream.isInitialized()) {
            title.setValue(null);
            subtitle.setValue(null);
            image.setValue(null);
            initialised.setValue(false);
            downloadedFile.setValue(null);
            return;
        }
        title.setValue(onDemandStream.getTitle());
        subtitle.setValue(onDemandStream.getSubtitle());
        image.setValue(onDemandStream.getImage());

        downloadedFile.setValue(tryGetExistingDownload());
        initialised.setValue(true);
    }

    public void download() {
        DownloadTask downloadTask = downloadManager.download(onDemandStream, this::setDownloadedFile);

        progress.bind(downloadTask.progressProperty());
        downloading.bind(downloadTask.runningProperty());

        new Thread(downloadTask).start();
    }

    private File tryGetExistingDownload() {
        File file = new File(onDemandStream.getDownloadFileName());
        return file.exists() ? file : null;
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

    public ReadOnlyBooleanProperty initialisedProperty() {
        return initialised.getReadOnlyProperty();
    }

    public boolean isDownloadRunning() {
        return downloadedFileProperty().get() == null;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty downloadingProperty() {
        return downloading;
    }

    public void setDownloadedFile(File downloadedFile) {
        this.downloadedFile.set(downloadedFile);
    }

    public Playlist getPlaylist() {
        return onDemandStream.getPlaylist();
    }

    public File getDownloadedFile() {
        return downloadedFile.get();
    }

    public ObjectProperty<File> downloadedFileProperty() {
        return downloadedFile;
    }

}

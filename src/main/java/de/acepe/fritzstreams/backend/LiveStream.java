package de.acepe.fritzstreams.backend;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.app.StreamCrawlerFactory;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.image.Image;

public class LiveStream {

    private static final Logger LOG = LoggerFactory.getLogger(LiveStream.class);
    private static final int WRITE_TO_FILE_BUFFER_SIZE = 4096;
    private static final int BUFFER_SECONDS = 3;
    private static final int BITRATE = 128;
    private static final int PLAY_BUFFER_SIZE_IN_KB = BUFFER_SECONDS * BITRATE / 8 * 1024;
    private static final String LIVE_STREAM_URL = "http://rbb-fritz-live.cast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3";
    private static final String CONTENT_URL = "/livestream/";

    private final StringProperty onAirTitle = new SimpleStringProperty();
    private final StringProperty onAirArtist = new SimpleStringProperty();
    private final ObjectProperty<Image> onAirImage = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();
    private final ObjectProperty<Path> tmpFile = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty();
    private final Settings settings;
    private final StreamCrawlerFactory streamCrawlerFactory;

    private boolean stopped;
    private ExecutorService downloaderService;

    @Inject
    public LiveStream(Settings settings, StreamCrawlerFactory streamCrawlerFactory) {
        this.settings = settings;
        this.streamCrawlerFactory = streamCrawlerFactory;

        deleteTmpFile();
    }

    public void init() {
        streamCrawlerFactory.create(CONTENT_URL, this::onStreamCrawled).init();
    }

    private void onStreamCrawled(StreamCrawler crawler) {
        Platform.runLater(() -> {
            title.setValue(crawler.getTitle());
            subtitle.setValue(crawler.getSubtitle());
            image.setValue(crawler.getImage());
            onAirArtist.setValue(crawler.getOnAirArtist());
            onAirTitle.setValue(crawler.getOnAirTitle());
            onAirImage.setValue(crawler.getOnAirImage());
            initialised.setValue(crawler.getTitle() != null);
        });
    }

    public void play() {
        this.stopped = false;
        downloaderService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("LiveStreamDownloader");
            return thread;
        });
        downloaderService.submit(this::downloadTemp);
        playingProperty().set(true);
    }

    public void stop() {
        this.stopped = true;
        deleteTmpFile();
        playingProperty().set(false);
    }

    private void downloadTemp() {
        try {
            URLConnection connection = new URL(LIVE_STREAM_URL).openConnection();
            writeStreamToTmpFile(connection);
        } catch (IOException e) {
            LOG.error("Could not open Connection to {}", LIVE_STREAM_URL, e);
        }
    }

    private void writeStreamToTmpFile(URLConnection connection) {
        Path target = Paths.get(settings.getTmpPath());

        try (InputStream is = connection.getInputStream();
                BufferedInputStream stream = new BufferedInputStream(is);
                OutputStream outstream = new FileOutputStream(target.toFile())) {
            byte[] buffer = new byte[WRITE_TO_FILE_BUFFER_SIZE];
            int len;
            while (!stopped && (len = stream.read(buffer)) > 0) {
                outstream.write(buffer, 0, len);
                if (tmpFileProperty().get() == null && target.toFile().length() >= PLAY_BUFFER_SIZE_IN_KB) {
                    Platform.runLater(() -> tmpFile.setValue(target));
                }
            }
        } catch (IOException e) {
            LOG.error("Could not write livestream data to temp file: {}", target, e);
        }
        if (downloaderService != null) {
            downloaderService.shutdown();
            downloaderService = null;
        }
        Platform.runLater(() -> {
            playingProperty().set(false);
            tmpFileProperty().setValue(null);
        });
    }

    private void deleteTmpFile() {
        new File(settings.getTmpPath()).delete();
        tmpFileProperty().setValue(null);
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

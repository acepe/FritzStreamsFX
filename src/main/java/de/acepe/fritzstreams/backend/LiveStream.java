package de.acepe.fritzstreams.backend;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class LiveStream {

    private static final Logger LOG = LoggerFactory.getLogger(LiveStream.class);
    private static final int WRITE_TO_FILE_BUFFER_SIZE = 4096;
    private static final int BUFFER_SECONDS = 3;
    private static final int BITRATE = 128;
    private static final int PLAY_BUFFER_SIZE_IN_KB = BUFFER_SECONDS * BITRATE / 8 * 1024;
    private static final String LIVE_STREAM_URL = "http://fritz.de/livemp3";
    private static final String CONTENT_URL = "/livestream/";

    private final Settings settings;
    private final StreamCrawler streamCrawler;

    private boolean stopped;
    private ExecutorService downloaderService;
    private Consumer<StreamMetaData> onMetaDataUpdatedCallback;
    private Runnable onPlayingChanged;
    private Path tmpFile;
    private StreamMetaData streamMetaData;

    @Inject
    public LiveStream(Settings settings, StreamCrawler streamCrawler) {
        this.settings = settings;
        this.streamCrawler = streamCrawler;

        deleteTmpFile();
    }

    public void refresh() {
        streamMetaData = streamCrawler.crawl(CONTENT_URL);
        if (onMetaDataUpdatedCallback != null) {
            onMetaDataUpdatedCallback.accept(streamMetaData);
        }
    }

    public void registerUICallbacks(Consumer<StreamMetaData> onMetaDataUpdated, Runnable onPlayingChanged) {
        this.onMetaDataUpdatedCallback = onMetaDataUpdated;
        this.onPlayingChanged = onPlayingChanged;
        onMetaDataUpdated.accept(streamMetaData);
    }

    public void play() {
        this.stopped = false;
        downloaderService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("LiveStreamDownloader");
            return thread;
        });
        downloaderService.submit(this::downloadTemp);
    }

    public void stop() {
        this.stopped = true;
        deleteTmpFile();
        onPlayingChanged.run();
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
                if (tmpFile == null && target.toFile().length() >= PLAY_BUFFER_SIZE_IN_KB) {
                    tmpFile = target;
                    onPlayingChanged.run();
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
            tmpFile = null;
            onPlayingChanged.run();
        });
    }

    private void deleteTmpFile() {
        new File(settings.getTmpPath()).delete();
        tmpFile = null;
    }

    public Path getTmpFile() {
        return tmpFile;
    }
}

package de.acepe.fritzstreams.backend;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LiveStream {

    private static final Logger LOG = LoggerFactory.getLogger(LiveStream.class);
    private static final int WRITE_TO_FILE_BUFFER_SIZE = 4096;
    private static final int BUFFER_SECONDS = 3;
    private static final int BITRATE = 128;
    private static final int PLAY_BUFFER_SIZE_IN_KB = BUFFER_SECONDS * BITRATE / 8 * 1024;
    private static final String LIVE_STREAM_URL = "http://rbb-fritz-live.cast.addradio.de/rbb/fritz/live/mp3/128/stream.mp3";

    private static final String BASE_URL = "https://www.fritz.de";
    private static final String TITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > h3 > a > span";
    private static final String SUBTITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > div > p";
    private static final String IMAGE_SELECTOR = "#main .layoutlivestream .layouthalf_2_4.count2 .layoutlivestream_info .manualteaser .manualteaserpicture img";

    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();
    private final ObjectProperty<Path> tmpFile = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty();
    private final Settings settings;
    private final OkHttpClient okHttpClient;

    private boolean stopped;
    private ExecutorService downloaderService;
    private Document doc;

    @Inject
    public LiveStream(Settings settings, OkHttpClient okHttpClient) {
        this.settings = settings;
        this.okHttpClient = okHttpClient;
        deleteTmpFile();

        // init();
    }

    public void init() {
        try {
            try {
                Request request = new Request.Builder().url(BASE_URL + "/livestream/").build();
                Response response = okHttpClient.newCall(request).execute();
                String content = response.body().string();
                doc = Jsoup.parse(content);

                Request imgRequest = new Request.Builder().url(extractImageUrl(IMAGE_SELECTOR)).build();
                Response imgResponse = okHttpClient.newCall(imgRequest).execute();
                image.setValue(new Image(imgResponse.body().byteStream()));
            } catch (IOException e) {
                LOG.error("Init stream failed: " + e);
                return;
            }

            String title = extractTitle(TITLE_SELECTOR);
            String subtitle = extractTitle(SUBTITLE_SELECTOR);

            Platform.runLater(() -> {
                this.title.setValue(title);
                this.subtitle.setValue(subtitle);
                initialised.setValue(title != null);
            });
        } catch (Exception e) {
            LOG.error("Init live stream failed", e);
        }
    }

    // TODO: refactor
    private String extractImageUrl(String imageSelector) {
        String imageUrl = doc.select(imageSelector).attr("src");
        return BASE_URL + imageUrl;
    }

    private String extractTitle(String selector) {
        Elements info = doc.select(selector);
        return info.text();
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
}

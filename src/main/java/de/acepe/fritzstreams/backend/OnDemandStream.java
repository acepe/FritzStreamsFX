package de.acepe.fritzstreams.backend;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.inject.assistedinject.Assisted;

import de.acepe.fritzstreams.app.DownloadTaskFactory;
import de.acepe.fritzstreams.app.StreamCrawlerFactory;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OnDemandStream {

    private final StreamCrawlerFactory streamCrawlerFactory;
    private final OkHttpClient okHttpClient;
    private static final Logger LOG = LoggerFactory.getLogger(OnDemandStream.class);
    private static final DateTimeFormatter TARGET_Date_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter URL_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMM");
    private static final String NIGHTFLIGHT_URL = "/livestream/liveplayer_nightflight.htm/day=%s.html";
    private static final String SOUNDGARDEN_URL = "/livestream/liveplayer_bestemusik.htm/day=%s.html";
    private static final String DOWNLOAD_SELECTOR = "#main > article > div.teaserboxgroup.first.count1.odd.layoutstandard.layouthalf_2_4 > section > article.manualteaser.last.count2.even.layoutmusikstream.layoutbeitrag_av_nur_av.doctypeteaser > div";
    private static final String PRORAMM_SELECTOR = "#sendungslink";
    private static final String DOWNLOAD_DESCRIPTOR_ATTRIBUTE = "data-media-ref";

    private static final String STREAM_TOKEN = "_stream\":\"";
    private static final String MP3_TOKEN = ".mp3";

    private final Settings settings;
    private final DownloadTaskFactory downloadTaskFactory;
    private final LocalDate date;
    private final Stream stream;
    private final Playlist playlist;
    private final ObjectProperty<File> downloadedFile = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();
    private final ReadOnlyBooleanWrapper unavailable = new ReadOnlyBooleanWrapper();
    private final ObjectProperty<Long> totalSizeInBytes = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> downloadedSizeInBytes = new SimpleObjectProperty<>();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty downloading = new SimpleBooleanProperty();

    private String downloadFileName;
    private Task<Void> downloadTask;
    private Document doc;
    private String streamURL;

    @Inject
    public OnDemandStream(StreamCrawlerFactory streamCrawlerFactory,
            OkHttpClient okHttpClient,
            Settings settings,
            DownloadTaskFactory downloadTaskFactory,
            Playlist playlist,
            @Assisted LocalDate date,
            @Assisted Stream stream) {
        this.streamCrawlerFactory = streamCrawlerFactory;
        this.okHttpClient = okHttpClient;
        this.downloadTaskFactory = downloadTaskFactory;
        this.playlist = playlist;
        this.date = date;
        this.stream = stream;
        this.settings = settings;
    }

    public void notAvailableAnymore() {
        Platform.runLater(() -> {
            title.setValue("Nicht mehr verfügbar");
            unavailable.setValue(true);
        });
    }

    public void notAvailableYet() {
        Platform.runLater(() -> {
            title.setValue("Noch nicht verfügbar");
            unavailable.setValue(true);
        });
    }

    public void init() {
        String contentURL = stream == Stream.NIGHTFLIGHT ? NIGHTFLIGHT_URL : SOUNDGARDEN_URL;
        contentURL = String.format(contentURL, date.format(URL_DATE_FORMAT));

        streamCrawlerFactory.create(contentURL, this::onStreamCrawled).init();
    }

    private void onStreamCrawled(StreamCrawler crawler) {
        doc = crawler.getDoc();
        if (doc == null) {
            unavailable.setValue(true);
            return;
        }

        String title = crawler.getTitle();
        streamURL = extractDownloadURL();
        playlist.init(title, extractProgrammUrl());

        Platform.runLater(() -> {
            this.title.setValue(streamURL != null ? title : "Nicht verfügbar");
            subtitle.setValue(crawler.getSubtitle());
            image.setValue(crawler.getImage());

            initializeDownloadFile();
            initialised.setValue(streamURL != null);
        });
    }

    private void initializeDownloadFile() {
        downloadFileName = createDownloadFileName();
        downloadedFile.setValue(tryGetExistingDownload());
    }

    private File tryGetExistingDownload() {
        File file = new File(downloadFileName);
        return file.exists() ? file : null;
    }

    private String extractProgrammUrl() {
        Elements info = doc.select(PRORAMM_SELECTOR);
        return StreamCrawler.BASE_URL + info.attr("href");
    }

    private String extractDownloadURL() {
        String downloadDescriptorURL = extractDownloadDescriptorUrl();
        if (downloadDescriptorURL == null) {
            return null;
        }

        try {
            URL downloadURL = new URL(StreamCrawler.BASE_URL + downloadDescriptorURL);
            Request request = new Request.Builder().url(downloadURL).build();
            Response response = okHttpClient.newCall(request).execute();
            String jsonText = response.body().string();

            int beginIndex = jsonText.indexOf(STREAM_TOKEN) + STREAM_TOKEN.length();
            int endIndex = jsonText.indexOf(MP3_TOKEN) + MP3_TOKEN.length();

            return jsonText.substring(beginIndex, endIndex);
        } catch (Exception e) {
            LOG.error("Couldn't extract download-URL from stream website", e);
            return null;
        }
    }

    private String extractDownloadDescriptorUrl() {
        Elements info = doc.select(DOWNLOAD_SELECTOR);
        return info.attr(DOWNLOAD_DESCRIPTOR_ATTRIBUTE);
    }

    private String createDownloadFileName() {
        String targetpath = settings.getTargetpath();

        return targetpath
               + File.separator
               + getTitle().replaceAll(" ", "_")
               + "_"
               + getDate().format(TARGET_Date_FORMAT)
               + ".mp3";
    }

    public void download() {
        initializeDownloadFile();
        downloadTask = downloadTaskFactory.create(this, this::setDownloadedFile, getPlaylist());

        progress.bind(downloadTask.progressProperty());
        downloading.bind(downloadTask.runningProperty());

        new Thread(downloadTask).start();
    }

    public void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    public String getDownloadURL() {
        return streamURL;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitle() {
        return title.get();
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("stream", stream).add("date", date).toString();
    }

    public File getDownloadedFile() {
        return downloadedFile.get();
    }

    public ObjectProperty<File> downloadedFileProperty() {
        return downloadedFile;
    }

    public void setDownloadedFile(File downloadedFile) {
        this.downloadedFile.set(downloadedFile);
    }

    public String getTargetFileName() {
        return downloadFileName;
    }

    public Long getTotalSizeInBytes() {
        return totalSizeInBytes.get();
    }

    public ObjectProperty<Long> totalSizeInBytesProperty() {
        return totalSizeInBytes;
    }

    public Long getDownloadedSizeInBytes() {
        return downloadedSizeInBytes.get();
    }

    public ObjectProperty<Long> downloadedSizeInBytesProperty() {
        return downloadedSizeInBytes;
    }

    public void setDownloadedSizeInBytes(Long downloadedSizeInBytes) {
        this.downloadedSizeInBytes.set(downloadedSizeInBytes);
    }

    public void setTotalSizeInBytes(Long sizeInBytes) {
        this.totalSizeInBytes.set(sizeInBytes);
    }

    public boolean isDownloadFinished() {
        return downloadedFileProperty().get() != null;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public boolean isDownloading() {
        return downloading.get();
    }

    public BooleanProperty downloadingProperty() {
        return downloading;
    }

    public boolean isInitialised() {
        return initialised.get() || unavailable.get();
    }
}

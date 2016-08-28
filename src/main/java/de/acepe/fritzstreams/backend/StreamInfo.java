package de.acepe.fritzstreams.backend;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.image.Image;

public class StreamInfo {

    public enum Stream {
        SOUNDGARDEN, NIGHTFLIGHT
    }

    private static final DateTimeFormatter TARGET_Date_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter URL_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMM");
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final Logger LOG = LoggerFactory.getLogger(StreamInfo.class);
    private static final String BASE_URL = "http://fritz.de%s";
    private static final String NIGHTFLIGHT_URL = "/livestream/liveplayer_nightflight.htm/day=%s.html";
    private static final String SOUNDGARDEN_URL = "/livestream/liveplayer_soundgarden.htm/day=%s.html";
    private static final String TITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > h3 > a > span";
    private static final String SUBTITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > div > p";
    private static final String DOWNLOAD_SELECTOR = "#main > article > div.teaserboxgroup.first.count1.odd.layoutstandard.layouthalf_2_4 > section > article.manualteaser.last.count2.even.layoutmusikstream.layoutbeitrag_av_nur_av.doctypeteaser > div";
    private static final String DOWNLOAD_DESCRIPTOR_ATTRIBUTE = "data-media-ref";
    private static final String IMAGE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.last.count2.even.layoutstandard.doctypeteaser > aside > div > a > img";
    private static final String STREAM_TOKEN = "_stream\":\"";
    private static final String MP3_TOKEN = ".mp3";

    private final Settings settings;
    private final LocalDate date;
    private final Stream stream;
    private final ObjectProperty<File> downloadedFile = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();

    private String downloadFileName;
    private Downloader downloader;
    private Document doc;
    private String streamURL;

    public StreamInfo(LocalDate date, Stream stream) {
        this.settings = Settings.getInstance();
        this.date = date;
        this.stream = stream;
    }

    public void init() {
        String contentURL = buildURL();
        try {
            doc = Jsoup.connect(contentURL).timeout(10000).data("query", "Java").userAgent("Mozilla").get();

        } catch (IOException e) {
            LOG.error("Init stream failed: " + e);
            return;
        }
        image.setValue(new Image(extractImageUrl(IMAGE_SELECTOR)));
        streamURL = extractDownloadURL();
        Platform.runLater(() -> {
            title.setValue(extractTitle(TITLE_SELECTOR));
            subtitle.setValue(extractTitle(SUBTITLE_SELECTOR));
            initializeDownloadFile();
        });
        initialised.setValue(streamURL != null);
    }

    private void initializeDownloadFile() {
        downloadFileName = createDownloadFileName();
        downloadedFile.setValue(tryGetExistingDownload());
    }

    private File tryGetExistingDownload() {
        File file = new File(downloadFileName);
        return file.exists() ? file : null;
    }

    private String buildURL() {
        String contentURL = streamURL(stream == Stream.NIGHTFLIGHT ? NIGHTFLIGHT_URL : SOUNDGARDEN_URL);
        return String.format(contentURL, date.format(URL_DATE_FORMAT));
    }

    private String streamURL(String subUrl) {
        return String.format(BASE_URL, subUrl);
    }

    private String extractImageUrl(String imageSelector) {
        String imageUrl = doc.select(imageSelector).attr("src");
        return String.format(BASE_URL, imageUrl);
    }

    private String extractTitle(String selector) {
        Elements info = doc.select(selector);
        return info.text();
    }

    private String extractDownloadURL() {
        String downloadDescriptorURL = extractDownloadDescriptorUrl();
        if (downloadDescriptorURL == null) {
            return null;
        }

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(new URL(streamURL(downloadDescriptorURL)).openStream(),
                                                                          UTF8))) {
            String jsonText = readAll(rd);

            int beginIndex = jsonText.indexOf(STREAM_TOKEN) + STREAM_TOKEN.length();
            int endIndex = jsonText.indexOf(MP3_TOKEN) + MP3_TOKEN.length();

            return jsonText.substring(beginIndex, endIndex);
        } catch (IOException e) {
            LOG.error("Couldn't extract download-URL from stream website", e);
            return null;
        }
    }

    private String extractDownloadDescriptorUrl() {
        Elements info = doc.select(DOWNLOAD_SELECTOR);
        return info.attr(DOWNLOAD_DESCRIPTOR_ATTRIBUTE);
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
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
        downloader = new Downloader(this);
        downloader.download();
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public String getStreamURL() {
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

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public boolean isDownloadFinished() {
        return downloadedFileProperty().get() != null;
    }
}
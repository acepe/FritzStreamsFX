package de.acepe.fritzstreams;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.image.Image;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class StreamInfo {

    public enum Stream {
        SOUNDGARDEN, NIGHTFLIGHT
    }

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

    private final LocalDate date;
    private final Stream stream;
    private Downloader downloader;

    private Document doc;
    private String title;
    private String subtitle;
    private String streamURL;
    private Image image;
    private ReadOnlyBooleanWrapper initialised = new ReadOnlyBooleanWrapper();

    public StreamInfo(LocalDate date, Stream stream) {
        this.date = date;
        this.stream = stream;
    }

    public void init() {
        String contentURL = buildURL();
        try {
            doc = Jsoup.connect(contentURL)
                       .timeout(10000)
                       .data("query", "Java")
                       .userAgent("Mozilla")
                       .timeout(3000)
                       .get();
        } catch (IOException e) {
            LOG.error("Init stream failed: " + e);
        }
        title = extractTitle(TITLE_SELECTOR);
        subtitle = extractTitle(SUBTITLE_SELECTOR);
        image = new Image(extractImageUrl(IMAGE_SELECTOR));
        streamURL = extractDownloadURL();
        initialised.setValue(true);
    }

    private String extractImageUrl(String imageSelector) {
        String imageUrl = doc.select(imageSelector).attr("src");
        return String.format(BASE_URL, imageUrl);
    }

    private String extractTitle(String selector) {
        Elements info = doc.select(selector);
        return info.text();
    }

    private String buildURL() {
        String contentURL = url(stream == Stream.NIGHTFLIGHT ? NIGHTFLIGHT_URL : SOUNDGARDEN_URL);
        return String.format(contentURL, date.format(DateTimeFormatter.ofPattern("ddMM")));
    }

    private String url(String subUrl) {
        return String.format(BASE_URL, subUrl);
    }

    private String extractDownloadURL() {
        String downloadDescriptorURL = extractDownloadDescriptorUrl();
        if (downloadDescriptorURL == null) {
            return null;
        }

        try (InputStream is = new URL(url(downloadDescriptorURL)).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);

            int beginIndex = jsonText.indexOf(STREAM_TOKEN) + STREAM_TOKEN.length();
            int endIndex = jsonText.indexOf(MP3_TOKEN) + MP3_TOKEN.length();

            return jsonText.substring(beginIndex, endIndex);
        } catch (IOException e) {
            e.printStackTrace();
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

    public void download() {
        downloader = new Downloader(this);
        downloader.download();
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getStreamURL() {
        return streamURL;
    }

    public Stream getStream() {
        return stream;
    }

    public LocalDate getDate() {
        return date;
    }

    public Image getImage() {
        return image;
    }

    public boolean getInitialised() {
        return initialised.get();
    }

    public ReadOnlyBooleanProperty initialisedProperty() {
        return initialised.getReadOnlyProperty();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("date", date).add("stream", stream).toString();
    }
}

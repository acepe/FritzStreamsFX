package de.acepe.fritzstreams;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javafx.concurrent.Task;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class StreamInfo {
    public enum Stream {
        SOUNDGARDEN, NIGHTFLIGHT
    }

    static final String BASE_URL = "http://fritz.de%s";
    static final String NIGHTFLIGHT_URL = "/livestream/liveplayer_nightflight.htm/day=%s.html";
    static final String SOUNDGARDEN_URL = "/livestream/liveplayer_soundgarden.htm/day=%s.html";
    static final String TITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > h3 > a > span";
    static final String SUBTITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > div > p";
    static final String DOWNLOAD_SELECTOR = "#main > article > div.teaserboxgroup.first.count1.odd.layoutstandard.layouthalf_2_4 > section > article.manualteaser.last.count2.even.layoutmusikstream.layoutbeitrag_av_nur_av.doctypeteaser > div";
    static final String DOWNLOAD_DESCRIPTOR_ATTRIBUTE = "data-media-ref";

    private final LocalDate date;
    private final Stream stream;
    private Document doc;
    private String title;
    private String subtitle;
    private String streamURL;

    public StreamInfo(LocalDate date, Stream stream) {
        this.date = date;
        this.stream = stream;
    }

    public void init(Consumer<Boolean> callback) {
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                init();
                return null;
            }
        };
        task.setOnFailed(event -> callback.accept(false));
        task.setOnSucceeded(event -> callback.accept(true));
        new Thread(task).start();
    }

    private void init() throws IOException {
        String contentURL = buildURL();
        doc = Jsoup.connect(contentURL).data("query", "Java").userAgent("Mozilla").timeout(3000).get();
        title = extractTitle(TITLE_SELECTOR);
        subtitle = extractTitle(SUBTITLE_SELECTOR);
        streamURL = extractDownloadURL();
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

            String streamToken = "_stream\":\"";
            int beginIndex = jsonText.indexOf(streamToken) + streamToken.length();

            String mp3Token = ".mp3";
            int endIndex = jsonText.indexOf(mp3Token) + mp3Token.length();

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
}

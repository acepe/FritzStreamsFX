package de.acepe.fritzstreams.backend;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;
import de.acepe.fritzstreams.app.StreamCrawlerFactory;
import de.acepe.fritzstreams.backend.json.OnDemandDownload;
import de.acepe.fritzstreams.backend.json.OnDemandStreamDescriptor;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class OnDemandStream {

    private static final Logger LOG = LoggerFactory.getLogger(OnDemandStream.class);
    private static final ZoneId ZONE_BERLIN = ZoneId.of("Europe/Berlin");
    private static final String DOWNLOAD_SELECTOR = "#main > article > div.count1.first.layouthalf_2_4.layoutstandard.odd.teaserboxgroup > section > article.count2.doctypeteaser.even.last.layoutbeitrag_av_nur_av.layoutmusikstream.manualteaser > div";
    private static final String PRORAMM_SELECTOR = "#sendungslink";
    private static final String DOWNLOAD_DESCRIPTOR_ATTRIBUTE = "data-jsb";
    private static final int LENGTH_OF_DATE = 10;
    private static final int LENGTH_OF_TIME = 4;
    private static final int LENGTH_FROM_END_OF_URL = "0000.html".length();

    private final Settings settings;
    private final Playlist playlist;
    private final StreamCrawlerFactory streamCrawlerFactory;
    private final OkHttpClient okHttpClient;
    private final String url;

    private final Gson gson;
    private final ZonedDateTime time;
    private final String id;

    private Document doc;
    private OnDemandStreamDescriptor onDemandStreamDescriptor;
    private String downloadFileName;
    private String title;
    private String subtitle;
    private String streamURL;
    private Image image;
    private boolean initialized;

    @Inject
    public OnDemandStream(StreamCrawlerFactory streamCrawlerFactory,
                          OkHttpClient okHttpClient,
                          Settings settings,
                          Playlist playlist,
                          @Assisted("initialTitle") String initialTitle,
                          @Assisted("url") String url) {
        this.streamCrawlerFactory = streamCrawlerFactory;
        this.okHttpClient = okHttpClient;
        this.playlist = playlist;
        this.settings = settings;
        this.url = url;

        String dateString = initialTitle.substring(initialTitle.length() - LENGTH_OF_DATE);
        LocalDate day = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        int beginIndexOftimeString = url.length() - LENGTH_FROM_END_OF_URL;
        String timeString = url.substring(beginIndexOftimeString, beginIndexOftimeString + LENGTH_OF_TIME);
        LocalTime localTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HHmm"));
        time = day.atStartOfDay(ZONE_BERLIN).with(localTime);

        id = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));

        gson = new Gson();
    }

    public void init() {
        try {
            streamCrawlerFactory.create(url, this::onStreamCrawled).init();
        } catch (Exception e) {
            LOG.error("Couldn't initialize Stream {}", id, e);
        }
    }

    private void onStreamCrawled(StreamCrawler crawler) {
        doc = crawler.getDoc();
        if (doc == null) {
            return;
        }

        extractDownloadURL();

        title = streamURL != null ? crawler.getTitle() : "Nicht verfügbar";
        subtitle = crawler.getSubtitle();
        image = crawler.getImage();

        playlist.init(title, extractProgrammUrl());
        initialized = true;
    }

    private String extractProgrammUrl() {
        Elements info = doc.select(PRORAMM_SELECTOR);
        return StreamCrawler.BASE_URL + info.attr("href");
    }

    private void extractDownloadURL() {
        String downloadDescriptorURL = extractDownloadDescriptorUrl();
        if (downloadDescriptorURL == null) {
            LOG.error("Couldn't find download-descriptor-URL for {} on stream website", id);
            return;
        }

        try {
            URL downloadURL = new URL(StreamCrawler.BASE_URL + downloadDescriptorURL);
            Request request = new Request.Builder().url(downloadURL).build();
            Response response = okHttpClient.newCall(request).execute();
            String jsonText = response.body().string();

            onDemandStreamDescriptor = gson.fromJson(jsonText, OnDemandStreamDescriptor.class);
            streamURL = onDemandStreamDescriptor.getMediaArray()
                                                .get(0)
                                                .getMediaStreamArray()
                                                .stream()
                                                .filter(m -> m.getQuality() != null)
                                                .findFirst()
                                                .orElseThrow(IOException::new)
                                                .getStream();

            initDownloadFileName();
            LOG.info("Thread: {}, Stream: {}, URL: {}", Thread.currentThread().getName(), id, streamURL);
        } catch (IOException e) {
            LOG.error("Couldn't extract download-URL for {} from stream website", id, e);
        }
    }

    private String extractDownloadDescriptorUrl() {
        Elements info = doc.select(DOWNLOAD_SELECTOR);
        String downloadJSON = info.attr(DOWNLOAD_DESCRIPTOR_ATTRIBUTE);
        OnDemandDownload download = gson.fromJson(downloadJSON, OnDemandDownload.class);

        return download.getMedia();
    }

    private void initDownloadFileName() {
        String rbbhandle = onDemandStreamDescriptor.getRbbhandle();
        String filename = rbbhandle.substring(rbbhandle.lastIndexOf('/'));
        downloadFileName = settings.getTargetpath() + File.separator + filename + ".mp3";
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public Image getImage() {
        return image;
    }

    public String getStreamURL() {
        return streamURL;
    }

    public LocalDate getDay() {
        return time.toLocalDate();
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", id).toString();
    }

    public boolean isInitialized() {
        return initialized;
    }

}

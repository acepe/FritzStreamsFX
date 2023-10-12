package de.acepe.fritzstreams.backend;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;
import de.acepe.fritzstreams.backend.json.OnDemandStreamDescriptor;
import jakarta.inject.Inject;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final int LENGTH_OF_HTML = ".html".length();
    private static final int LENGTH_OF_DATETIME = "31120000".length();
    private static final DateTimeFormatter FIXED_URL_DATETIME_FORMAT = DateTimeFormatter.ofPattern("ddMMHHmmyyyy");
    private static final DateTimeFormatter ID_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final Settings settings;
    private final Playlist playlist;
    private final StreamCrawler streamCrawler;
    private final OkHttpClient okHttpClient;
    private final String url;
    private String title;

    private final Gson gson;
    private final ZonedDateTime time;
    private final String id;

    private OnDemandStreamDescriptor onDemandStreamDescriptor;
    private String downloadFileName;
    private String subtitle;
    private String streamURL;
    private Image image;
    private boolean initialized;

    @Inject
    public OnDemandStream(StreamCrawler streamCrawler,
                          Gson gson,
                          OkHttpClient okHttpClient,
                          Settings settings,
                          Playlist playlist,
                          @Assisted("initialTitle") String initialTitle,
                          @Assisted("url") String url) {
        this.streamCrawler = streamCrawler;
        this.gson = gson;
        this.okHttpClient = okHttpClient;
        this.playlist = playlist;
        this.settings = settings;
        this.url = url;

        title = parseTitle(initialTitle);
        time = parseDate();
        id = time.format(ID_DATE_FORMATTER);
    }

    private String parseTitle(String initialTitle) {
        initialTitle = initialTitle.replaceAll("<.*?>", "");
        initialTitle = splitAt(initialTitle, " |");
        initialTitle = splitAt(initialTitle, ", ");
        return initialTitle;
    }

    private String splitAt(String title, String seperatorString) {
        int endIndex = title.indexOf(seperatorString);
        if (endIndex != -1) {
            return title.substring(0, endIndex);
        }
        return title;
    }

    private ZonedDateTime parseDate() {
        int beginIndexOfDateTimeString = url.length() - LENGTH_OF_DATETIME - LENGTH_OF_HTML;
        String datetimeString = url
                .substring(beginIndexOfDateTimeString, beginIndexOfDateTimeString + LENGTH_OF_DATETIME);
        datetimeString = datetimeString.concat(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")));
        ZonedDateTime parsedTime = ZonedDateTime.now().withZoneSameLocal(ZONE_BERLIN)
                                                .with(LocalTime.parse(datetimeString, FIXED_URL_DATETIME_FORMAT))
                                                .with(LocalDate.parse(datetimeString, FIXED_URL_DATETIME_FORMAT));
        return fixDateOnYearChanged(parsedTime);
    }

    private ZonedDateTime fixDateOnYearChanged(ZonedDateTime parsedTime) {
        if (parsedTime.isAfter(ZonedDateTime.now())) {
            parsedTime = parsedTime.withYear(parsedTime.getYear() - 1);
        }
        return parsedTime;
    }

    public void init() {
        try {
            StreamMetaData streamMetaData = streamCrawler.crawl(url);
            onStreamCrawled(streamMetaData);
        } catch (Exception e) {
            LOG.error("Couldn't initialize Stream {}", id, e);
        }
    }

    private void onStreamCrawled(StreamMetaData streamMetaData) {
        if (streamMetaData == null) {
            return;
        }

        extractDownloadURL();

        if (streamURL == null) {
            title = "Nicht verfügbar";
        }
        subtitle = streamMetaData.getSubtitle();
        image = streamMetaData.getImage();

        playlist.init(title, streamCrawler.getPlaylistURL(time));
        initialized = true;
    }

    private void extractDownloadURL() {
        String downloadDescriptorURL = streamCrawler
                .extractDownloadDescriptorUrl();
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

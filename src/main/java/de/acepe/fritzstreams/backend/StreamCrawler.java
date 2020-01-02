package de.acepe.fritzstreams.backend;

import com.google.gson.Gson;
import de.acepe.fritzstreams.backend.json.OnAirData;
import de.acepe.fritzstreams.backend.json.OnDemandDownload;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ofPattern;

public class StreamCrawler {
    private static final Logger LOG = LoggerFactory.getLogger(StreamCrawler.class);

    public static final String BASE_URL = "https://www.fritz.de";
    private static final String ON_AIR_CONTENT_URL = "/content/dam/rbb/frz";
    private static final String TITLE_SELECTOR = "#main > article > div.count2.even.last.layouthalf_2_4.layoutstandard.teaserboxgroup > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > h3 > a > span";
    private static final String SUBTITLE_SELECTOR = "#main > article > div.count2.even.last.layouthalf_2_4.layoutstandard.teaserboxgroup > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > div > p";
    private static final String IMAGE_SELECTOR = "#main .layoutlivestream .layouthalf_2_4.count2 .layoutlivestream_info .manualteaser .manualteaserpicture img";
    private static final String ON_AIR_URL = "/include/frz/zeitstrahl/nowonair.json";
    private static final String DEFAULT_IMAGE = "https://www.fritz.de/content/dam/rbb/frz/zeitstrahl/78/135/500027477.jpg.jpg/img.jpg";
    private static final String DOWNLOAD_SELECTOR = "#main > article > div.count1.first.layouthalf_2_4.layoutstandard.odd.teaserboxgroup > section > article.count2.doctypeteaser.even.last.layoutbeitrag_av_nur_av.layoutmusikstream.manualteaser > div";
    private static final String DOWNLOAD_DESCRIPTOR_ATTRIBUTE = "data-jsb";
    private static final String PLAYLIST_URL_FORMAT = "/programm/sendungen/playlists/%s_am_%s/playlisten/%s/%s/%s.html";

    private final OkHttpClient okHttpClient;
    private final Gson gson;

    private Document doc;
    private String contentURL;

    @Inject
    public StreamCrawler(OkHttpClient okHttpClient, Gson gson) {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

    public StreamMetaData crawl(String contentURL) {
        this.contentURL = contentURL;
        try {
            Request request = new Request.Builder().url(BASE_URL + contentURL).build();
            Response response = okHttpClient.newCall(request).execute();
            try (ResponseBody body = response.body()) {
                String content = body.string();
                doc = Jsoup.parse(content);
            }

            String title = extractTitle(TITLE_SELECTOR);
            String subtitle = extractTitle(SUBTITLE_SELECTOR);

            String imageUrl = extractImageUrl();
            Image image = null;
            Image onAirImage = new Image(DEFAULT_IMAGE);
            String onAirArtist = null;
            String onAirTitle = null;
            if (imageUrl != null) {
                Request imgRequest = new Request.Builder().url(imageUrl).build();
                Response imgResponse = okHttpClient.newCall(imgRequest).execute();
                try (ResponseBody body = imgResponse.body()) {
                    image = new Image(body.byteStream());
                }
                Request onAirRequest = new Request.Builder().url(BASE_URL + ON_AIR_URL).build();
                Response onAirResponse = okHttpClient.newCall(onAirRequest).execute();
                try (ResponseBody body = onAirResponse.body()) {
                    if (onAirResponse.isSuccessful()) {
                        String onAirContent = body.string();
                        OnAirData onAirData = new Gson().fromJson(onAirContent, OnAirData.class);
                        onAirArtist = onAirData.getArtist();
                        onAirTitle = onAirData.getTitle();

                        if (onAirData.getImg() != null) {
                            Request onAirImgRequest = new Request.Builder().url(BASE_URL
                                    + ON_AIR_CONTENT_URL
                                    + onAirData.getImg().getLnk()).build();
                            Response onAirImgResponse = okHttpClient.newCall(onAirImgRequest).execute();
                            try (ResponseBody onAirImgResponseBody = onAirImgResponse.body()) {
                                onAirImage = new Image(onAirImgResponseBody.byteStream());
                            }
                        }
                    }
                }
            }
            return new StreamMetaData(title, subtitle, image, onAirImage, onAirArtist, onAirTitle);
        } catch (IOException e) {
            LOG.error("Crawling stream failed", e);
            doc = null;
        }
        return null;
    }

    private String extractImageUrl() {
        String imageUrl = doc.select(IMAGE_SELECTOR).attr("src");
        if (imageUrl.isEmpty()) {
            return null;
        }
        return BASE_URL + imageUrl;
    }

    private String extractTitle(String selector) {
        Elements info = doc.select(selector);
        return info.text();
    }

    String extractDownloadDescriptorUrl() {
        Elements info = doc.select(DOWNLOAD_SELECTOR);
        String downloadJSON = info.attr(DOWNLOAD_DESCRIPTOR_ATTRIBUTE);
        OnDemandDownload download = gson.fromJson(downloadJSON, OnDemandDownload.class);

        return download.getMedia();
    }

    String getPlaylistURL(ZonedDateTime time) {
        //  String nightflight = "/programm/sendungen/playlists/beste_musik_am_mittwoch/playlisten/2019/04/190410_2000.html";
        //  String beste_musik = "/programm/sendungen/playlists/nightflight_am_freitag/playlisten/2019/04/190412_0000.html";
        String streamType = contentURL.contains("nightflight") ? "nightflight" : "beste_musik";
        String day = time.format(ofPattern("eeee").withLocale(Locale.GERMANY)).toLowerCase(Locale.GERMANY);
        String year = time.format(ofPattern("yyyy"));
        String month = time.format(ofPattern("MM"));
        String datetime = time.format(ofPattern("yyMMdd_HHmm"));
        return StreamCrawler.BASE_URL + String.format(PLAYLIST_URL_FORMAT, streamType, day, year, month, datetime);
    }

}

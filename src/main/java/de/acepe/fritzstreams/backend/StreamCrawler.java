package de.acepe.fritzstreams.backend;

import java.io.IOException;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;

import de.acepe.fritzstreams.backend.json.OnAirData;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StreamCrawler {
    private static final Logger LOG = LoggerFactory.getLogger(StreamCrawler.class);

    public static final String BASE_URL = "https://www.fritz.de";
    private static final String ON_AIR_CONTENT_URL = "/content/dam/rbb/frz";
    private static final String TITLE_SELECTOR = "#main > article > div.count2.even.last.layouthalf_2_4.layoutstandard.teaserboxgroup > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > h3 > a > span";
    private static final String SUBTITLE_SELECTOR = "#main > article > div.count2.even.last.layouthalf_2_4.layoutstandard.teaserboxgroup > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > div > p";
    private static final String IMAGE_SELECTOR = "#main .layoutlivestream .layouthalf_2_4.count2 .layoutlivestream_info .manualteaser .manualteaserpicture img";
    private static final String ON_AIR_URL = "/include/frz/zeitstrahl/nowonair.json";
    private static final String DEFAULT_IMAGE = "https://www.fritz.de/content/dam/rbb/frz/zeitstrahl/78/135/500027477.jpg.jpg/img.jpg";

    private final OkHttpClient okHttpClient;
    private final String contentURL;
    private final Consumer<StreamCrawler> onStreamCrawledCallback;

    private Document doc;
    private String title;
    private String subtitle;
    private String onAirArtist;
    private String onAirTitle;
    private Image image;
    private Image onAirImage;

    @Inject
    public StreamCrawler(OkHttpClient okHttpClient,
            @Assisted String contentURL,
            @Assisted Consumer<StreamCrawler> onStreamCrawledCallback) {
        this.okHttpClient = okHttpClient;
        this.contentURL = contentURL;
        this.onStreamCrawledCallback = onStreamCrawledCallback;
    }

    public void init() {
        try {
            Request request = new Request.Builder().url(BASE_URL + contentURL).build();
            Response response = okHttpClient.newCall(request).execute();
            try (ResponseBody body = response.body()) {
                String content = body.string();
                doc = Jsoup.parse(content);
            }

            title = extractTitle(TITLE_SELECTOR);
            subtitle = extractTitle(SUBTITLE_SELECTOR);

            String url = extractImageUrl(IMAGE_SELECTOR);
            if (url != null) {
                Request imgRequest = new Request.Builder().url(url).build();
                Response imgResponse = okHttpClient.newCall(imgRequest).execute();
                try (ResponseBody body = imgResponse.body()) {
                    image = new Image(body.byteStream());
                }

                Request onAirRequest = new Request.Builder().url(BASE_URL + ON_AIR_URL).build();
                Response onAirResponse = okHttpClient.newCall(onAirRequest).execute();
                try (ResponseBody body = onAirResponse.body()) {
                    String onAirContent = body.string();
                    OnAirData onAirData = new Gson().fromJson(onAirContent, OnAirData.class);
                    onAirArtist = onAirData.getArtist();
                    onAirTitle = onAirData.getTitle();

                    if (onAirData.getImg() == null) {
                        onAirImage = new Image(DEFAULT_IMAGE);
                    } else {
                        Request onAirImgRequest = new Request.Builder().url(BASE_URL
                                                                            + ON_AIR_CONTENT_URL
                                                                            + onAirData.getImg().getLnk())
                                                                       .build();
                        Response onAirImgResponse = okHttpClient.newCall(onAirImgRequest).execute();
                        try (ResponseBody onAirImgResponseBody = onAirImgResponse.body()) {
                            onAirImage = new Image(onAirImgResponseBody.byteStream());
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Crawling stream failed", e);
            doc = null;
        }
        onStreamCrawledCallback.accept(this);
    }

    private String extractImageUrl(String imageSelector) {
        String imageUrl = doc.select(imageSelector).attr("src");
        if (imageUrl.isEmpty()) {
            return null;
        }
        return BASE_URL + imageUrl;
    }

    private String extractTitle(String selector) {
        Elements info = doc.select(selector);
        return info.text();
    }

    public Document getDoc() {
        return doc;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Image getImage() {
        return image;
    }

    public String getOnAirArtist() {
        return onAirArtist;
    }

    public String getOnAirTitle() {
        return onAirTitle;
    }

    public Image getOnAirImage() {
        return onAirImage;
    }
}

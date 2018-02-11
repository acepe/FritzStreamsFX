package de.acepe.fritzstreams.backend;

import java.io.IOException;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StreamCrawler {
    private static final Logger LOG = LoggerFactory.getLogger(StreamCrawler.class);

    public static final String BASE_URL = "https://www.fritz.de";
    private static final String TITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > h3 > a > span";
    private static final String SUBTITLE_SELECTOR = "#main > article > div.teaserboxgroup.intermediate.count2.even.layoutstandard.layouthalf_2_4 > section > article.manualteaser.first.count1.odd.layoutlaufende_sendung.doctypesendeplatz > div > p";
    private static final String IMAGE_SELECTOR = "#main .layoutlivestream .layouthalf_2_4.count2 .layoutlivestream_info .manualteaser .manualteaserpicture img";

    private final OkHttpClient okHttpClient;
    private final String contentURL;
    private final Consumer<StreamCrawler> onStreamCrawledCallback;

    private Document doc;
    private String title;
    private String subtitle;
    private Image image;

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
            String content = response.body().string();
            doc = Jsoup.parse(content);

            Request imgRequest = new Request.Builder().url(extractImageUrl(IMAGE_SELECTOR)).build();
            Response imgResponse = okHttpClient.newCall(imgRequest).execute();
            image = new Image(imgResponse.body().byteStream());

            title = extractTitle(TITLE_SELECTOR);
            subtitle = extractTitle(SUBTITLE_SELECTOR);
        } catch (IOException e) {
            LOG.error("Crawling stream failed", e);
            doc = null;
        }
        onStreamCrawledCallback.accept(this);
    }

    private String extractImageUrl(String imageSelector) {
        String imageUrl = doc.select(imageSelector).attr("src");
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
}

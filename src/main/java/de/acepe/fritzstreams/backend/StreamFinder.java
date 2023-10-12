package de.acepe.fritzstreams.backend;

import de.acepe.fritzstreams.app.OnDemandStreamFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StreamFinder {

    private static final Logger LOG = LoggerFactory.getLogger(StreamFinder.class);
    private static final String STREAM_BOX_URL = "https://www.fritz.de/livestream";
    private static final String STREAM_LIST_SELECTOR = "#main > article > div.count1.first.layouthalf_2_4.layoutstandard.odd.teaserboxgroup > div > ul > li > a";

    private final OkHttpClient okHttpClient;
    private final OnDemandStreamFactory onDemandStreamFactory;

    @Inject
    public StreamFinder(OkHttpClient okHttpClient, OnDemandStreamFactory onDemandStreamFactory) {
        this.okHttpClient = okHttpClient;
        this.onDemandStreamFactory = onDemandStreamFactory;
    }

    public void init(Consumer<List<OnDemandStream>> streamConsumer) {
        try {
            Request request = new Request.Builder().url(STREAM_BOX_URL).build();
            Response response = okHttpClient.newCall(request).execute();
            try (ResponseBody body = response.body()) {
                String content = body.string();
                Document doc = Jsoup.parse(content);

                streamConsumer.accept(parseStreamList(doc));
                return;
            }
        } catch (IOException e) {
            LOG.error("Getting stream list from stream-box failed", e);
        }
        streamConsumer.accept(new ArrayList<>(0));
    }

    private List<OnDemandStream> parseStreamList(Document doc) {
        List<OnDemandStream> onDemandStreams = new ArrayList<>();

        Elements list = doc.select(STREAM_LIST_SELECTOR);
        for (Element element : list) {
            String initialTitle = element.attr("title");
            String url = element.attr("href");
            LOG.info(initialTitle + ", " + url);

            if (!initialTitle.toLowerCase().contains("livestream")) {
                onDemandStreams.add(onDemandStreamFactory.create(initialTitle, url));
            }
        }
        return onDemandStreams;
    }

}

package de.acepe.fritzstreams.backend;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Playlist {
    private static final Logger LOG = LoggerFactory.getLogger(Playlist.class);
    private static final String TABLE_SELECTOR = "#col1 > div > div > table";
    private final OkHttpClient httpClient;

    private String title;
    private List<PlayListEntry> entries = new LinkedList<>();

    @Inject
    public Playlist(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void init(String title, String url) {
        this.title = title;
        LOG.info("Playlist: "+title+", "+url);
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = httpClient.newCall(request).execute();
            String content = response.body().string();

            Document doc = Jsoup.parse(content);
            Elements table = doc.select(TABLE_SELECTOR);

            entries = extractPlayListEntries(table);
        } catch (IOException e) {
            LOG.error("Init stream failed: " + e);
        }
    }

    private List<PlayListEntry> extractPlayListEntries(Elements table) {
        for (Element row : table.select("tr")) {
            if (!row.hasClass("play_track")) {
                continue;
            }
            String artist = row.select("td.trackinterpret").text();
            String title = row.select("td.tracktitle").text();
            entries.add(new PlayListEntry(artist, title));
        }
        return entries;
    }

    public List<PlayListEntry> getEntries() {
        return entries;
    }

    public String getTitle() {
        return title;
    }
}

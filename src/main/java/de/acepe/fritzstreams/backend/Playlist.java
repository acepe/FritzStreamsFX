package de.acepe.fritzstreams.backend;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Playlist {
    private static final Logger LOG = LoggerFactory.getLogger(Playlist.class);
    private static final String TABLE_SELECTOR = "#main > div.playlist_tables > div > table > tbody";

    private final String title;
    private final String programmUrl;
    private List<PlayListEntry> entries;

    public Playlist(String title, String programmUrl) {
        this.title = title;
        this.programmUrl = programmUrl;
        init();
    }

    private void init() {
        try {
            Document doc = Jsoup.connect(programmUrl).timeout(10000).data("query", "Java").userAgent("Mozilla").get();
            Elements table = doc.select(TABLE_SELECTOR);

            entries = extractPlayListEntries(table);
        } catch (IOException e) {
            LOG.error("Init stream failed: " + e);
        }
    }

    private List<PlayListEntry> extractPlayListEntries(Elements table) {
        List<PlayListEntry> entries = new LinkedList<>();
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

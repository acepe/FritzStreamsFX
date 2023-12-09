package de.acepe.fritzstreams;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import de.acepe.fritzstreams.app.OnDemandStreamFactory;
import de.acepe.fritzstreams.backend.*;
import okhttp3.*;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StahlwerkDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(StahlwerkDownloader.class);

    private final OkHttpClient httpClient;
    private Playlist playlist;

    public static void main(String[] args) {
        System.setProperty("https.protocols", "TLSv1.2");

        String targetPath = args.length > 0 ? args[0] : System.getProperty("user.dir");
        new StahlwerkDownloader(targetPath);
    }

    public StahlwerkDownloader(String targetPath) {
        httpClient = new OkHttpClient();
        Gson gson = new Gson();
        Settings settings = new Settings();

        if (Strings.isNullOrEmpty(targetPath)) {
            LOG.error("Please specify target path as first argument.");
            return;
        }

        Path path = Paths.get(targetPath);
        if (Files.notExists(path)) {
            LOG.error("Target path not accessable.");
            return;
        }

        if (!Files.isWritable(path)) {
            LOG.error("Target path not writable.");
            return;
        }
        settings.setTargetpath(targetPath);

        StreamCrawler streamCrawler = new StreamCrawler(httpClient, gson);
        OnDemandStreamFactory odsFactory = (initialTitle, url) -> new OnDemandStream(streamCrawler, gson, httpClient, settings, new Playlist(httpClient), initialTitle, url);

        StreamFinder streamFinder = new StreamFinder(httpClient, odsFactory);
        streamFinder.init(this::manageStreams);
    }

    private void manageStreams(List<OnDemandStream> onDemandStreams) {
        Optional<OnDemandStream> ods = onDemandStreams.stream().filter(s -> s.getTitle().contains("Stahlwerk"))
                                                      .findFirst();
        if (ods.isPresent()) {
            OnDemandStream stahlwerk = ods.get();
            stahlwerk.init();
            download(stahlwerk);
        }
        System.exit(0);
    }

    private void download(OnDemandStream stahlwerk) {
        try {
            URL url = URI.create(stahlwerk.getStreamURL()).toURL();

            Call call = httpClient.newCall(new Request.Builder().url(url).get().build());

            File targetFile = new File(stahlwerk.getDownloadFileName());
            playlist = stahlwerk.getPlaylist();

            try (Response response = call.execute()) {
                if (response.code() == 200 || response.code() == 201) {
                    Headers responseHeaders = response.headers();
                    for (int i = 0; i < responseHeaders.size(); i++) {
                        LOG.info("Download: Response {} : {}", responseHeaders.name(i), responseHeaders.value(i));
                    }

                    try (var is = response.body().byteStream(); OutputStream os = new FileOutputStream(targetFile)) {
                        long size = response.body().contentLength();
                        LOG.info("Download size: " + size);

                        byte[] buffer = new byte[1024 * 40];
                        int downloadedSum = 0;
                        int len;
                        int oldPercentDone = 0;
                        while ((len = is.read(buffer)) > 0) {
                            downloadedSum += len;
                            int percentDone = Math.round((float) downloadedSum / size * 100);
                            if (percentDone > oldPercentDone) {
                                LOG.info("Downloaded: {} %, ", percentDone);
                            }
                            oldPercentDone = percentDone;
                            os.write(buffer, 0, len);
                        }
                        writePlaylistMetaData(stahlwerk);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error during download: ", e);
            System.exit(-1);
        }
    }

    private void writePlaylistMetaData(OnDemandStream stahlwerk) {
        String fileName = stahlwerk.getDownloadFileName();
        LOG.info("Writing Playlist to File: " + fileName);

        try {
            File targetFile = new File(fileName);
            if (playlist != null && !playlist.getEntries().isEmpty()) {
                String playlistAsString = playlist.getEntries()
                                                  .stream()
                                                  .map(this::playlistEntryAsString)
                                                  .collect(Collectors.joining("\n"));
                MP3File f = (MP3File) AudioFileIO.read(targetFile);
                f.getTag().setField(FieldKey.COMMENT, playlistAsString);

                LOG.info("Saving file...");
                f.save();
                LOG.info("Download complete.");
            }
        } catch (Exception e) {
            LOG.error("Failed to add playlist to ID3-tag");
        }
    }

    private String playlistEntryAsString(PlayListEntry playListEntry) {
        return playListEntry.getArtist() + " - " + playListEntry.getTitle();
    }

}

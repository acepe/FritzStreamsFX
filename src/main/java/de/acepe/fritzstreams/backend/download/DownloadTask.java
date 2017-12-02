package de.acepe.fritzstreams.backend.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.backend.PlayListEntry;
import de.acepe.fritzstreams.backend.Playlist;
import de.acepe.fritzstreams.ui.Dialogs;
import javafx.application.Platform;
import javafx.concurrent.Task;
import okhttp3.*;

public class DownloadTask<T extends Downloadable> extends Task<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadTask.class);

    private final OkHttpClient httpClient;
    private final T downloadable;
    private final File targetFile;
    private final Playlist playlist;
    private final Consumer<File> downloadedFileConsumer;

    public DownloadTask(OkHttpClient httpClient, T downloadable) {
        this(httpClient, downloadable, file -> {
            /* nop */}, null);
    }

    public DownloadTask(OkHttpClient httpClient,
            T downloadable,
            Consumer<File> downloadedFileConsumer,
            Playlist playlist) {
        this.httpClient = httpClient;
        this.downloadable = downloadable;
        this.downloadedFileConsumer = downloadedFileConsumer;
        targetFile = new File(downloadable.getTargetFileName());
        this.playlist = playlist;
    }

    @Override
    protected Void call() throws Exception {
        URL url = new URL(downloadable.getDownloadURL());
        Call call = httpClient.newCall(new Request.Builder().url(url).get().build());

        Response response = call.execute();
        if (response.code() == 200 || response.code() == 201) {
            Headers responseHeaders = response.headers();
            for (int i = 0; i < responseHeaders.size(); i++) {
                LOG.debug("DownloadTask Response {}", responseHeaders.name(i) + ": " + responseHeaders.value(i));
            }

            try (InputStream is = response.body().byteStream(); OutputStream os = new FileOutputStream(targetFile)) {
                long size = response.body().contentLength();
                Platform.runLater(() -> downloadable.setTotalSizeInBytes(size));
                updateProgress(0, size);

                byte[] buffer = new byte[1024 * 4];
                int downloadedSum = 0;
                int len;
                while ((len = is.read(buffer)) > 0) {
                    if (isCancelled()) {
                        break;
                    }
                    downloadedSum += len;
                    long finalDownloadedSum = downloadedSum;
                    Platform.runLater(() -> downloadable.setDownloadedSizeInBytes(finalDownloadedSum));
                    updateProgress(downloadedSum, size);
                    os.write(buffer, 0, len);
                }
                writePlaylistMetaData();
            }
        }
        return null;
    }

    private void writePlaylistMetaData() {
        try {
            if (playlist != null && !playlist.getEntries().isEmpty()) {
                String playlistAsString = playlist.getEntries()
                                                  .stream()
                                                  .map(this::playlistEntryAsString)
                                                  .collect(Collectors.joining("\n"));
                MP3File f = (MP3File) AudioFileIO.read(targetFile);
                f.getTag().setField(FieldKey.COMMENT, playlistAsString);
                f.save();
            }
        } catch (Exception e) {
            LOG.error("Failed to add playlist to ID3-tag");
        }
    }

    private String playlistEntryAsString(PlayListEntry playListEntry) {
        return playListEntry.getArtist() + " - " + playListEntry.getTitle();
    }

    @Override
    protected void succeeded() {
        LOG.info("Download {} completed", downloadable);
        downloadedFileConsumer.accept(targetFile);
    }

    @Override
    protected void failed() {
        Throwable ex = getException();
        LOG.error("Failed to download {}", downloadable, ex);
        Dialogs.showErrorDialog(ex);
    }

    @Override
    protected void cancelled() {
        LOG.info("Download {} was cancelled, deleting partial download.", downloadable);
        if (targetFile.exists()) {
            targetFile.delete();
        }
    }

    public T getDownloadable() {
        return downloadable;
    }
}

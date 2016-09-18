package de.acepe.fritzstreams.backend.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.ui.Dialogs;
import javafx.concurrent.Task;

public class DownloadTask<T extends Downloadable> extends Task<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadTask.class);

    private final T downloadable;
    private final File targetFile;
    private final Consumer<File> downloadedFileConsumer;

    public DownloadTask(T downloadable) {
        this(downloadable, file -> {/* nop */});
    }

    public DownloadTask(T downloadable, Consumer<File> downloadedFileConsumer) {
        this.downloadable = downloadable;
        this.downloadedFileConsumer = downloadedFileConsumer;
        targetFile = new File(downloadable.getTargetFileName());
    }

    @Override
    protected Void call() throws Exception {
        URLConnection connection = new URL(downloadable.getDownloadURL()).openConnection();
        try (InputStream is = connection.getInputStream(); OutputStream outstream = new FileOutputStream(targetFile)) {

            int size = connection.getContentLength();
            updateProgress(0, size);

            byte[] buffer = new byte[4096];
            int downloadedSum = 0;
            int len;
            while ((len = is.read(buffer)) > 0) {
                if (isCancelled()) {
                    break;
                }
                downloadedSum += len;
                updateProgress(downloadedSum, size);
                outstream.write(buffer, 0, len);
            }
            outstream.close();
        }
        return null;
    }

    @Override
    protected void succeeded() {
        downloadedFileConsumer.accept(targetFile);
        LOG.info("Download {} completed", downloadable);
    }

    @Override
    protected void failed() {
        LOG.error("Failed to download {}", downloadable, getException());
        Throwable ex = getException();
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

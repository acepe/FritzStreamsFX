package de.acepe.fritzstreams.backend;

import de.acepe.fritzstreams.app.DownloadTaskFactory;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class DownloadManager {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadManager.class);

    private final DownloadTaskFactory downloadTaskFactory;
    private final List<DownloadTask> tasks = new LinkedList<>();

    @Inject
    public DownloadManager(DownloadTaskFactory downloadTaskFactory) {
        this.downloadTaskFactory = downloadTaskFactory;
    }

    public DownloadTask download(OnDemandStream onDemandStream, Consumer<File> downloadedFileConsumer) {
        DownloadTask downloadTask = downloadTaskFactory.create(onDemandStream, downloadedFileConsumer);
        tasks.add(downloadTask);

        new Thread(downloadTask).start();
        return downloadTask;
    }

    public void stop() {
        LOG.info("Shutting down");
        tasks.forEach(Task::cancel);
    }
}

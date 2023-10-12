package de.acepe.fritzstreams.backend;

import de.acepe.fritzstreams.app.DownloadTaskFactory;
import jakarta.inject.Inject;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DownloadManager {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadManager.class);

    private final DownloadTaskFactory downloadTaskFactory;
    private final Map<OnDemandStream, DownloadTask> tasks = new LinkedHashMap<>();

    @Inject
    public DownloadManager(DownloadTaskFactory downloadTaskFactory) {
        this.downloadTaskFactory = downloadTaskFactory;
    }

    public DownloadTask download(OnDemandStream onDemandStream, Consumer<File> downloadedFileConsumer) {
        DownloadTask downloadTask = downloadTaskFactory.create(onDemandStream, downloadedFileConsumer);
        tasks.put(onDemandStream, downloadTask);

        new Thread(downloadTask).start();
        return downloadTask;
    }

    public DownloadTask getDownloadTask(OnDemandStream onDemandStream){
        return tasks.get(onDemandStream);
    }

    public void stop() {
        LOG.info("Shutting down");
        tasks.values().forEach(Task::cancel);
    }
}

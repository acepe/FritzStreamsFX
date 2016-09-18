package de.acepe.fritzstreams.backend.download;

import static javafx.beans.binding.Bindings.size;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.backend.vk.VKDownload;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class DownloadManager {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadManager.class);

    private static final int PARALLEL_DOWNLOADS = 3;
    private static DownloadManager instance;

    private final List<DownloadTask<VKDownload>> runningTasks = new ArrayList<>(PARALLEL_DOWNLOADS);
    private final ObservableList<VKDownload> downloadList = FXCollections.observableArrayList();
    private final ObservableList<VKDownload> pendingList = FXCollections.observableArrayList();
    private final ObservableList<VKDownload> runningList = FXCollections.observableArrayList();
    private final ObservableList<VKDownload> finishedList = FXCollections.observableArrayList();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final BooleanProperty running = new SimpleBooleanProperty();
    private final IntegerProperty doneCount = new SimpleIntegerProperty();
    private final IntegerProperty count = new SimpleIntegerProperty();
    private final ChangeListener<Number> trackTaskProgress = this::trackTaskProgress;
    private final EventHandler<WorkerStateEvent> onTaskFinished = this::onTaskFinished;

    public static DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    private DownloadManager() {
        count.bind(size(pendingList).add(size(runningList)).add(size(finishedList)));
        doneCount.bind(size(finishedList));
    }

    public void addDownload(VKDownload download) {
        downloadList.add(download);
        pendingList.add(download);
        scheduleDownload();
    }

    public void startDownloads() {
        if (running.get()) {
            return;
        }
        running.set(true);
        scheduleDownload();
    }

    private void scheduleDownload() {
        if (!running.get()) {
            LOG.debug("Not running, will not start Downloading");
            return;
        }

        int pendingDownloads = pendingList.size();
        int running = runningTasks.size();
        int taskToStart = Math.min(pendingDownloads, PARALLEL_DOWNLOADS - running);
        LOG.debug("Running Tasks {}", running);
        LOG.debug("Pending Downloads {}", pendingDownloads);
        LOG.debug("Tasks to Start {}", taskToStart);
        LOG.debug("------------------------------");

        if (!pendingList.isEmpty() && taskToStart != 0) {
            for (int i = 0; i < taskToStart; i++) {
                VKDownload download = pendingList.get(pendingDownloads - 1 - i);
                pendingList.remove(download);
                runningList.add(download);

                DownloadTask<VKDownload> task = new DownloadTask<>(download);
                download.setTask(task);

                task.progressProperty().addListener(trackTaskProgress);
                task.addEventHandler(WorkerStateEvent.ANY, onTaskFinished);

                runningTasks.add(task);
                new Thread(task).start();
            }
        }
    }

    private void trackTaskProgress(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        double sumProgress = doneCount.get();
        for (DownloadTask<VKDownload> t : runningTasks) {
            sumProgress += t.progressProperty().doubleValue();
        }
        progress.setValue(sumProgress / count.get());
    }

    private void onTaskFinished(WorkerStateEvent evt) {
        if (evt.getEventType() == WorkerStateEvent.WORKER_STATE_SUCCEEDED
            || evt.getEventType() == WorkerStateEvent.WORKER_STATE_CANCELLED
            || evt.getEventType() == WorkerStateEvent.WORKER_STATE_FAILED) {

            @SuppressWarnings("unchecked")
            DownloadTask<VKDownload> task = (DownloadTask<VKDownload>) evt.getSource();
            task.removeEventHandler(WorkerStateEvent.ANY, onTaskFinished);
            task.progressProperty().removeListener(trackTaskProgress);

            VKDownload download = task.getDownloadable();
            runningList.remove(download);
            finishedList.add(download);

            runningTasks.remove(task);

            if (runningList.isEmpty() && finishedList.isEmpty()) {
                progress.setValue(0);
            }
            if (finishedList.size() == count.get()) {
                progress.setValue(1);
            }

            scheduleDownload();
        }
    }

    public void stopDownloads() {
        running.set(false);
        for (int i = runningTasks.size() - 1; i >= 0; i--) {
            runningTasks.get(i).cancel();
        }
    }

    public void cancel(VKDownload download) {
        download.cancel();
        if (pendingList.contains(download)) {
            pendingList.remove(download);
        }
        if (runningList.contains(download)) {
            pendingList.remove(download);
        }
        downloadList.remove(download);
    }

    public ObservableList<VKDownload> getPendingList() {
        return pendingList;
    }

    public ObservableList<VKDownload> getRunningList() {
        return runningList;
    }

    public ObservableList<VKDownload> getFinishedList() {
        return finishedList;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    public IntegerProperty doneCountProperty() {
        return doneCount;
    }

    public IntegerProperty countProperty() {
        return count;
    }

    public ObservableList<VKDownload> getDownloadList() {
        return downloadList;
    }
}

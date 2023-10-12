package de.acepe.fritzstreams.backend;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.currentThread;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;

public class StreamManager {
    private static final Logger LOG = LoggerFactory.getLogger(StreamManager.class);
    private static final int NUM_THREADS = 6;

    private final Map<LocalDate, List<OnDemandStream>> streamMap = new HashMap<>();
    private final LiveStream liveStream;
    private final StreamFinder streamFinder;
    private final List<Task<Void>> initTasks = new ArrayList<>(NUM_THREADS);
    private final ScheduledExecutorService liveStreamUpdateService;

    private Runnable callback;

    @Inject
    public StreamManager(LiveStream liveStream, StreamFinder streamFinder) {
        this.liveStream = liveStream;
        this.streamFinder = streamFinder;

        liveStreamUpdateService = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("Periodic-Live-Stream-Update");
            return thread;
        });
        liveStreamUpdateService.scheduleAtFixedRate(liveStream::refresh, 0, 10, TimeUnit.SECONDS);
    }

    public void init(Runnable callback) {
        this.callback = callback;
        streamFinder.init(this::manageStreams);
    }

    private void manageStreams(List<OnDemandStream> onDemandStreams) {
        for (OnDemandStream stream : onDemandStreams) {
            List<OnDemandStream> streamsForDay = streamMap.computeIfAbsent(stream.getDay(), d -> new ArrayList<>());
            streamsForDay.add(stream);
        }

        scheduleInitThreads();
    }

    private void scheduleInitThreads() {
        List<LocalDate> days = streamMap.keySet()
                                        .stream()
                                        .sorted(reverseOrder())
                                        .collect(toList());

        int batchSize = days.size() / NUM_THREADS;
        int rest = days.size() % NUM_THREADS;

        for (int i = 0; i < NUM_THREADS; i++) {
            int batchBeginIndex = i * batchSize;
            int batchEndIndex = batchBeginIndex + batchSize;
            if (i == NUM_THREADS - 1 && rest != 0) {
                batchEndIndex += rest;
            }
            List<LocalDate> batch = new ArrayList<>(days.subList(batchBeginIndex, batchEndIndex));
            Task<Void> initTask = new InitTask(batch);
            initTasks.add(initTask);

            Thread initThread = new Thread(initTask);
            initThread.setName("Init-Stream-Thread (" + (i + 1) + "/" + NUM_THREADS + ")");
            initThread.start();
        }
    }

    public void stop() {
        LOG.info("Shutting down");
        liveStreamUpdateService.shutdownNow();

        // reverse order, so we don't run into concurrent-modification exceptions, because tasks remove themselves from
        // the list
        for (int i = initTasks.size() - 1; i >= 0; i--) {
            Task<Void> initTask = initTasks.get(i);
            if (initTask.isRunning()) {
                initTask.cancel();
            }
        }
    }

    public List<OnDemandStream> getStreams(LocalDate day) {
        return streamMap.getOrDefault(day, new ArrayList<>(0));
    }

    public boolean isInitialized(LocalDate date) {
        return getStreams(date).stream().anyMatch(OnDemandStream::isInitialized);
    }

    public LiveStream getLiveStream() {
        return liveStream;
    }

    private class InitTask extends Task<Void> {
        private final List<LocalDate> days;

        public InitTask(List<LocalDate> days) {
            this.days = days;
        }

        @Override
        protected Void call() {
            LocalDateTime started = now();
            days.forEach(day -> {
                if (isCancelled()) {
                    return;
                }
                streamMap.get(day).forEach(OnDemandStream::init);

                Platform.runLater(() -> callback.run());
            });
            LOG.debug("Thread: {}: Initializing streams took: {} seconds", currentThread().getName(),
                    SECONDS.between(started, now()));
            return null;
        }

        @Override
        protected void done() {
            Platform.runLater(() -> {
                Throwable exception = getException();
                if (exception != null) {
                    LOG.error("Error during initialisation", exception);
                }
                initTasks.remove(this);
            });
        }
    }
}

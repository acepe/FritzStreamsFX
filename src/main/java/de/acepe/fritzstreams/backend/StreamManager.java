package de.acepe.fritzstreams.backend;

import static de.acepe.fritzstreams.backend.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.backend.Stream.SOUNDGARDEN;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acepe.fritzstreams.app.StreamInfoFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class StreamManager {
    private static final Logger LOG = LoggerFactory.getLogger(StreamManager.class);

    private static final int DAYS_PAST = 7;
    private static final int NUM_THREADS = 8;
    private static final ZoneId ZONE_BERLIN = ZoneId.of("Europe/Berlin");

    private final Map<LocalDate, OnDemandStream> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, OnDemandStream> nightflightStreamMap = new HashMap<>();
    private final StreamInfoFactory streamInfoFactory;
    private final LiveStream liveStream;
    private final List<Task<Void>> initTasks = new ArrayList<>(NUM_THREADS);
    private final ScheduledExecutorService liveStreamUpdateService;

    private StreamInitCallback callback;

    @Inject
    public StreamManager(StreamInfoFactory streamInfoFactory, LiveStream liveStream) {
        this.streamInfoFactory = streamInfoFactory;
        this.liveStream = liveStream;

        liveStreamUpdateService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Periodic-Live-Stream-Update");
                return thread;
            }
        });
        liveStreamUpdateService.schedule(liveStream::init, 1, TimeUnit.MINUTES);
    }

    public void init() {
        LocalDate startDay = LocalDate.now();
        for (int i = 0; i <= DAYS_PAST; i++) {
            LocalDate date = startDay.minusDays(i);

            soundgardenStreamMap.put(date, streamInfoFactory.create(date, SOUNDGARDEN));
            nightflightStreamMap.put(date, streamInfoFactory.create(date, NIGHTFLIGHT));
        }

        scheduleInitThreads();
    }

    private void scheduleInitThreads() {
        for (int i = 0; i < NUM_THREADS; i++) {
            Task<Void> initTask = new InitTask(i);
            initTasks.add(initTask);

            Thread initThread = new Thread(initTask);
            initThread.setName("Init-Stream-Thread");
            initThread.start();
        }
    }

    private Void initStreams(Task<Void> task, int threadNr) {
        LocalDateTime started = LocalDateTime.now();
        List<LocalDate> days = soundgardenStreamMap.keySet()
                                                   .stream()
                                                   .sorted(Comparator.reverseOrder())
                                                   .collect(Collectors.toList());

        if (threadNr == 0) {
            liveStream.init();
        }
        for (int i = threadNr; i < days.size(); i += NUM_THREADS) {
            LocalDate day = days.get(i);
            if (task.isCancelled()) {
                return null;
            }

            OnDemandStream soundgarden = soundgardenStreamMap.get(day);
            if (day.isEqual(LocalDate.now()) && isBeforeSoundgardenRelease()) {
                soundgarden.notAvailableYet();
            } else {
                soundgarden.init();
            }

            OnDemandStream nightflight = nightflightStreamMap.get(day);
            if (day.isEqual(LocalDate.now().minusDays(DAYS_PAST)) && isBeforeSoundgardenRelease()) {
                nightflight.notAvailableAnymore();
            } else {
                nightflight.init();
            }

            if (callback != null) {
                Platform.runLater(() -> callback.onStreamInitialized(day));
            }
        }
        LOG.debug("Initializing streams took: {} seconds", ChronoUnit.SECONDS.between(started, LocalDateTime.now()));
        return null;
    }

    private boolean isBeforeSoundgardenRelease() {
        LocalDateTime todayAt2200InGermanTime = LocalDateTime.now(ZONE_BERLIN).withHour(22).withMinute(0);
        LocalDateTime nowInGermanTime = LocalDateTime.now(ZONE_BERLIN);
        return nowInGermanTime.isBefore(todayAt2200InGermanTime);
    }

    public void stop() {
        liveStreamUpdateService.shutdownNow();

        // reverse order, so we don't run into concurrent-modification exceptions, because tasks remove themselves from
        // the list
        for (int i = initTasks.size() - 1; i >= 0; i--) {
            Task<Void> initTask = initTasks.get(i);
            if (initTask.isRunning()) {
                initTask.cancel();
            }
        }
        for (LocalDate day : soundgardenStreamMap.keySet()) {
            stopDownload(soundgardenStreamMap.get(day));
            stopDownload(nightflightStreamMap.get(day));
        }
    }

    private void stopDownload(OnDemandStream onDemandStream) {
        if (onDemandStream != null) {
            onDemandStream.cancelDownload();
        }
    }

    public OnDemandStream getNightflight(LocalDate day) {
        return nightflightStreamMap.get(day);
    }

    public OnDemandStream getSoundgarden(LocalDate day) {
        return soundgardenStreamMap.get(day);
    }

    public void registerInitCallback(StreamInitCallback callback) {
        this.callback = callback;
    }

    public boolean isInitialised(LocalDate date) {
        return soundgardenStreamMap.get(date).isInitialised() && nightflightStreamMap.get(date).isInitialised();
    }

    public interface StreamInitCallback {
        void onStreamInitialized(LocalDate date);
    }

    private class InitTask extends Task<Void> {
        private final int threadNr;

        public InitTask(int threadNr) {
            this.threadNr = threadNr;
        }

        @Override
        protected Void call() {
            return initStreams(this, threadNr);
        }

        @Override
        protected void done() {
            initTasks.remove(this);
        }
    }
}

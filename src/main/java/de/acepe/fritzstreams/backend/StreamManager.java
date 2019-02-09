package de.acepe.fritzstreams.backend;

import de.acepe.fritzstreams.app.OnDemandStreamFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.acepe.fritzstreams.backend.Stream.NIGHTFLIGHT;
import static de.acepe.fritzstreams.backend.Stream.SOUNDGARDEN;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;

public class StreamManager {
    private static final Logger LOG = LoggerFactory.getLogger(StreamManager.class);

    private static final int DAYS_PAST = 7;
    private static final int NUM_THREADS = 6;
    private static final ZoneId ZONE_BERLIN = ZoneId.of("Europe/Berlin");

    private final Map<LocalDate, OnDemandStream> soundgardenStreamMap = new HashMap<>();
    private final Map<LocalDate, OnDemandStream> nightflightStreamMap = new HashMap<>();
    private final OnDemandStreamFactory onDemandStreamFactory;
    private final LiveStream liveStream;
    private final List<Task<Void>> initTasks = new ArrayList<>(NUM_THREADS);
    private final ScheduledExecutorService liveStreamUpdateService;

    private Runnable callback;

    @Inject
    public StreamManager(OnDemandStreamFactory onDemandStreamFactory, LiveStream liveStream) {
        this.onDemandStreamFactory = onDemandStreamFactory;
        this.liveStream = liveStream;

        liveStreamUpdateService = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("Periodic-Live-Stream-Update");
            return thread;
        });
        liveStreamUpdateService.scheduleAtFixedRate(liveStream::init, 10, 10, TimeUnit.SECONDS);
    }

    public void init() {
        LocalDate startDay = LocalDate.now();
        for (int i = 0; i <= DAYS_PAST; i++) {
            LocalDate date = startDay.minusDays(i);

            soundgardenStreamMap.put(date, onDemandStreamFactory.create(date, SOUNDGARDEN));
            nightflightStreamMap.put(date, onDemandStreamFactory.create(date, NIGHTFLIGHT));
        }

        scheduleInitThreads();
    }

    private void scheduleInitThreads() {
        for (int i = 0; i < NUM_THREADS; i++) {
            Task<Void> initTask = new InitTask(i);
            initTasks.add(initTask);

            Thread initThread = new Thread(initTask);
            initThread.setName("Init-Stream-Thread (" + i + "/" + NUM_THREADS + ")");
            initThread.start();
        }
    }

    private Void initStreams(Task<Void> task, int threadNr) {
        LocalDateTime started = now();
        List<LocalDate> days = soundgardenStreamMap.keySet()
                                                   .stream()
                                                   .sorted(reverseOrder())
                                                   .collect(toList());

        if (threadNr == 0) {
            liveStream.init();
        }
        for (int i = threadNr; i < days.size(); i += NUM_THREADS) {
            if (task.isCancelled()) {
                return null;
            }

            LocalDate day = days.get(i);
            initSoundgarden(day);
            initNightflight(day);

            Platform.runLater(() -> callback.run());
        }
        LOG.debug("Initializing streams took: {} seconds", SECONDS.between(started, now()));
        return null;
    }

    private void initNightflight(LocalDate day) {
        OnDemandStream nightflight = nightflightStreamMap.get(day);
        if (day.isEqual(LocalDate.now()
                                 .minusDays(DAYS_PAST)) && isBeforeSoundgardenRelease()) {
            nightflight.notAvailableAnymore();
        } else {
            nightflight.init();
        }
    }

    private void initSoundgarden(LocalDate day) {
        OnDemandStream soundgarden = soundgardenStreamMap.get(day);
        if (day.isEqual(LocalDate.now()) && isBeforeSoundgardenRelease()) {
            soundgarden.notAvailableYet();
        } else {
            soundgarden.init();
        }
    }

    private boolean isBeforeSoundgardenRelease() {
        LocalDateTime todayAt2200InGermanTime = now(ZONE_BERLIN).withHour(22)
                                                                .withMinute(0);
        LocalDateTime nowInGermanTime = now(ZONE_BERLIN);
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

    public void registerInitCallback(Runnable callback) {
        this.callback = callback;
    }

    public boolean isInitialised(LocalDate date) {
        return soundgardenStreamMap.get(date).isInitialised() || nightflightStreamMap.get(date).isInitialised();
    }

    public LiveStream getLiveStream() {
        return liveStream;
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

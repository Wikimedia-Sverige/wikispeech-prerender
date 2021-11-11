package se.wikimedia.wikispeech.prerender.site;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.mediawiki.RecentChangesApi;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.query.PageNeedsToBeSegmented;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushSegmentPageAndQueueForSynthesis;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecentChangesPoller {

    private final Logger log = LogManager.getLogger();

    @Setter
    private Duration maximumSynthesizedVoiceAge = Duration.ofDays(30);

    @Setter
    private Duration sleepBetweenRequests = Duration.ofMinutes(1);

    private RecentChangesApi.Item lastRecentChangesItemProcessed;
    private RemoteSite remoteSite;

    public RecentChangesPoller(RemoteSite remoteSite) {
        this.remoteSite = remoteSite;
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch stoppedLatch;

    public void start() {
        if (running.get()) {
            throw new RuntimeException("Already running");
        }
        running.set(true);
        stoppedLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (running.get()) {
                        try {
                            poll();
                        } catch (Exception e) {
                            log.error("Failed polling {}", remoteSite.getName(), e);
                        }
                        try {
                            Thread.sleep(sleepBetweenRequests.toMillis());
                        } catch (InterruptedException ie) {
                            log.error("Interrupted! Thread will shut down!");
                            break;
                        }
                    }
                } finally {
                    log.info("Thread is shutting down");
                    stoppedLatch.countDown();
                }
            }
        }).start();
    }

    public void stop() {
        if (!running.get()) {
            throw new RuntimeException("Already stopped!");
        }
        running.set(false);
        try {
            stoppedLatch.await();
            stoppedLatch = null;
        } catch (InterruptedException ie) {
            log.error("Interrupted while awaiting threads to close down.", ie);
        }
    }


    public void poll() throws IOException {
        RecentChangesApi api = new RecentChangesApi();
        api.open();
        api.get(remoteSite.getConsumerUrl(), 0, lastRecentChangesItemProcessed == null ? null : lastRecentChangesItemProcessed.getTimestamp(), new Collector<RecentChangesApi.Item>() {
            @Override
            public boolean collect(RecentChangesApi.Item recentlyChanged) {
                try {
                    if (Prevalence.getInstance().execute(
                            new PageNeedsToBeSegmented(
                                    maximumSynthesizedVoiceAge,
                                    recentlyChanged.getRevid(),
                                    remoteSite.getConsumerUrl(),
                                    recentlyChanged.getTitle()
                            )
                    )) {
                        Prevalence.getInstance().execute(
                                PushSegmentPageAndQueueForSynthesis.factory(
                                        remoteSite.getConsumerUrl(),
                                        recentlyChanged.getTitle(),
                                        remoteSite.getLanguage(),
                                        remoteSite.getVoices(),
                                        20
                                )
                        );
                    } else {
                        System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    log.error("Failed to queue segment page {} for {}", recentlyChanged.getTitle(), remoteSite.getName(), e);
                }

                if (lastRecentChangesItemProcessed == null) {
                    lastRecentChangesItemProcessed = recentlyChanged;
                } else if (recentlyChanged.getTimestamp().isAfter(lastRecentChangesItemProcessed.getTimestamp())) {
                    lastRecentChangesItemProcessed = recentlyChanged;
                }

                return true;
            }
        });

    }
}

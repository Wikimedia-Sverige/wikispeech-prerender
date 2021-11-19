package se.wikimedia.wikispeech.prerender.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.mediawiki.RecentChangesApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.PageNeedsToBeSegmented;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.SetWikiTimestampOfLastRecentChangesItemProcessed;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Service
public class RecentChangesService extends ExecutorService implements SmartLifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    private final Duration delayBetweenRequests = Duration.ofMinutes(1);
    private final Prevalence prevalence;
    private final SegmentService segmentService;
    private final RecentChangesApi recentChangesApi;

    private final Map<String, RecentChangesApi.Item> lastProcessedRecentChangesItemByConsumerUrl;

    public RecentChangesService(
            @Autowired Prevalence prevalence,
            @Autowired SegmentService segmentService,
            @Autowired RecentChangesApi recentChangesApi
    ) {
        this.prevalence = prevalence;
        this.segmentService = segmentService;
        this.recentChangesApi = recentChangesApi;

        this.lastProcessedRecentChangesItemByConsumerUrl = new ConcurrentHashMap<>();
    }

    @Override
    protected void execute() {

        Collection<Wiki> wikis;
        try {
            wikis = prevalence.execute(new Query<Root, Collection<Wiki>>() {
                @Override
                public Collection<Wiki> query(Root root, Date date) throws Exception {
                    return new HashSet<>(root.getWikiByConsumerUrl().values());
                }
            });
        } catch (Exception e) {
            log.error("Failed to gather wikis", e);
            return;
        }
        if (!wikis.isEmpty()) {
            CountDownLatch doneLatch = new CountDownLatch(wikis.size());
            for (Wiki wiki : wikis) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pollExhaustive(wiki);
                        } catch (Exception e) {
                            log.error("Failed polling recent changes for {}", wiki.getName(), e);
                        } finally {
                            doneLatch.countDown();
                        }
                    }

                }).start();
            }
            try {
                doneLatch.await();
            } catch (InterruptedException ie) {
                log.info("Interrupted while awaiting threads to finish", ie);
            }
        }

        LocalDateTime pauseEnds = LocalDateTime.now().plus(delayBetweenRequests);
        while (isRunning() && LocalDateTime.now().isBefore(pauseEnds)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                log.info("Interrupted while pausing between requests", ie);
                break;
            }
        }

    }

    /**
     *
     * @param wiki
     * @return false if no more recent changes available
     * @throws Exception
     */
    private void pollExhaustive(Wiki wiki) throws Exception {
        while (poll(wiki));
    }

    /**
     * @param wiki
     * @return True if there are potential more changes to be polled
     * @throws Exception
     */
    private boolean poll(Wiki wiki) throws Exception {
        log.info("Polling recent changes for {} since {}", wiki.getName(), wiki.getTimestampOfLastRecentChangesItemProcessed());

        RecentChangesApi.Item lastProcessedRecentChangesItemAtStart = lastProcessedRecentChangesItemByConsumerUrl.get(wiki.getConsumerUrl());

        recentChangesApi.get(
                wiki.getConsumerUrl(),
                wiki.getPollRecentChangesNamespaces(),
                wiki.getTimestampOfLastRecentChangesItemProcessed() == null ? null : wiki.getTimestampOfLastRecentChangesItemProcessed(),
                new Collector<RecentChangesApi.Item>() {
                    @Override
                    public boolean collect(RecentChangesApi.Item recentlyChanged) {
                        if (!isRunning()) {
                            return false;
                        }
                        try {
                            lastProcessedRecentChangesItemByConsumerUrl.put(wiki.getConsumerUrl(), recentlyChanged);

                            if (recentlyChanged.getType() != RecentChangesApi.ItemType.created
                                    && recentlyChanged.getType() != RecentChangesApi.ItemType.edit) {
                                // skipping
                                return true;
                            }

                            if (prevalence.execute(
                                    new PageNeedsToBeSegmented(
                                            wiki.getConsumerUrl(),
                                            recentlyChanged.getTitle(),
                                            recentlyChanged.getRevisionIdentity()
                                    )
                            )) {
                                if (segmentService.queue(
                                        wiki.getConsumerUrl(),
                                        recentlyChanged.getTitle()
                                ) ) {
                                    log.debug("Queued command to segment based on {} at {}", recentlyChanged, wiki.getName());
                                } else {
                                    log.trace("The queue already contains a command to segment based on {} at {}", recentlyChanged, wiki.getName());
                                }
                            } else {
                                log.info("Already up to date with recent change for {} at {}", recentlyChanged.getTitle(), wiki.getName());
                            }

                        } catch (Exception e) {
                            log.error("Caught exception while processing {}", recentlyChanged, e);
                        }
                        return true;
                    }
                });

        RecentChangesApi.Item lastProcessedRecentChangesItem = lastProcessedRecentChangesItemByConsumerUrl.get(wiki.getConsumerUrl());
        if (lastProcessedRecentChangesItemAtStart == null
                || lastProcessedRecentChangesItemAtStart.getRecentChangesIdentity() != lastProcessedRecentChangesItem.getRecentChangesIdentity()) {
            prevalence.execute(
                    new SetWikiTimestampOfLastRecentChangesItemProcessed(
                            wiki.getConsumerUrl(),
                            lastProcessedRecentChangesItem.getTimestamp()
                    ));
            // not exhausted, polling more.
            return true;
        } else {
            log.info("No new recent changes at {}", wiki.getName());
            return false;
        }
    }


}

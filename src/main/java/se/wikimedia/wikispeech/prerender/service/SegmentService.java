package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.mediawiki.PageApi;
import se.wikimedia.wikispeech.prerender.mediawiki.WikispeechApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.*;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateSegmentedPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreatePageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.FinalizedPageSegmented;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class SegmentService extends ExecutorService implements SmartLifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    private final Queue<QueueItem> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Set<String>> queueMap = new ConcurrentHashMap<>();

    private final Prevalence prevalence;

    private final WikispeechApi wikispeechApi;
    private final PageApi pageApi;


    public SegmentService(
            @Autowired Prevalence prevalence,
            @Autowired WikispeechApi wikispeechApi,
            @Autowired PageApi pageApi
    ) {
        this.prevalence = prevalence;
        this.wikispeechApi = wikispeechApi;
        this.pageApi = pageApi;
        setNumberOfWorkerThreads(1);
    }

    /**
     * @param consumerUrl
     * @param title
     * @return true if queued, false if already in queue.
     */
    public synchronized boolean queue(String consumerUrl, String title) {
        if (queueMap.computeIfAbsent(consumerUrl, k -> new HashSet<>(1000)).add(title)) {
            queue.add(new QueueItem(consumerUrl, title));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void execute() {
        QueueItem queueItem = queue.poll();
        if (queueItem == null) {
            try {
                log.trace("Waiting for queue to be populated...");
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                log.info("Interrupted while waiting for data to arrive on the queue!", ie);
            }
        } else {
            try {
                queueMap.get(queueItem.getConsumerUrl()).remove(queueItem.getTitle());
                segment(queueItem.getConsumerUrl(), queueItem.getTitle());
            } catch (Exception e) {
                log.error("Failed to execute {}", queueItem, e);
            }
        }
    }

    @Data
    private static class QueueItem {
        private String consumerUrl;
        private String title;

        public QueueItem(String consumerUrl, String title) {
            this.consumerUrl = consumerUrl;
            this.title = title;
        }
    }

    public void segment(String consumerUrl, String title) throws Exception {
        Long currentRevision = wikispeechApi.getCurrentRevision(consumerUrl, title);
        if (currentRevision == null) {
            // page no longer exists.
            // todo consider removing page from prevalence.
            // todo but also consider edit wars, in that case we don't want to re-execute over and over.
            return;
        }

        // if revision haven't changed since last visit, then no need to process.
        if (!prevalence.execute(
                new PageNeedsToBeSegmented(
                        consumerUrl,
                        title,
                        currentRevision
                )
        )) {
            // no need
            return;
        }

        PageApi.PageInfo pageInfo = pageApi.getPageInfo(consumerUrl, title);

        String pageLanguage = pageInfo.getPageLanguage();
        if (pageLanguage == null) {
            pageLanguage = prevalence.execute(new GetWikiDefaultLanguage(consumerUrl));
        }

        if (!prevalence.execute(
                new PageExists(
                        consumerUrl,
                        title
                ))) {
            prevalence.execute(
                    new CreateSegmentedPage(
                            consumerUrl,
                            title,
                            pageLanguage,
                            currentRevision
                        ));
        }

        Set<String> deletedSegmentStringHashes = new HashSet<>();
        for (byte[] hash : prevalence.execute(
                new GatherPageSegmentHashes(
                        consumerUrl,
                        title
                ))) {
            deletedSegmentStringHashes.add(Hex.encodeHexString(hash));
        }

        log.info("Segmenting page {} at {}", consumerUrl, title);

        wikispeechApi.segment(consumerUrl, title, new Collector<>() {
            int index = -1;

            @Override
            public boolean collect(WikispeechApi.Segment segment) {
                try {
                    index++;
                    byte[] hash = Hex.decodeHex(segment.getHash());
                    deletedSegmentStringHashes.remove(Hex.encodeHexString(hash));
                    if (!prevalence.execute(
                            new PageSegmentExists(
                                    consumerUrl,
                                    title,
                                    hash
                            ))) {
                        prevalence.execute(
                                new CreatePageSegment(
                                        consumerUrl,
                                        title,
                                        index,
                                        hash
                                ));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        });

        Set<byte[]> deletedSegmentByteHashes = new HashSet<>(deletedSegmentStringHashes.size());
        for (String hash : deletedSegmentStringHashes) {
            deletedSegmentByteHashes.add(Hex.decodeHex(hash));
        }

        prevalence.execute(
                new FinalizedPageSegmented(
                        consumerUrl,
                        title,
                        pageLanguage,
                        currentRevision,
                        deletedSegmentByteHashes
                )
        );

    }

    public void flushSegments() {
        // todo delete old segments that has not been touched for a very long time.
    }

}

package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.PriorityQueue;
import se.wikimedia.wikispeech.prerender.mediawiki.WikispeechApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateOrUpdatePageSegmentVoice;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

@Service
public class SynthesizeService extends AbstractLifecycle implements SmartLifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    private final Queue<SynthesizeCommand> queue = new ConcurrentLinkedQueue<>();

    private final Prevalence prevalence;

    private final WikispeechApi wikispeechApi;

    private final PriorityService priorityService;

    private final int numberOfWorkerThreads = 2;
    private final int maximumNumberOfCandidates = 100;

    private ExecutorService workers;

    private CountDownLatch populatorStoppedLatch;
    private Thread populator;

    public SynthesizeService(
            @Autowired Prevalence prevalence,
            @Autowired WikispeechApi wikispeechApi,
            @Autowired PriorityService priorityService
    ) {
        this.prevalence = prevalence;
        this.wikispeechApi = wikispeechApi;
        this.priorityService = priorityService;

        workers = new ExecutorService() {
            @Override
            protected void execute() {
                SynthesizeCommand command = queue.poll();
                if (command == null) {
                    try {
                        log.trace("Queue is empty. Waiting for more commands.");
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        log.info("Interrupted while waiting for queue", ie);
                    }
                } else {
                    synthesize(command);
                }
            }
        };

        populator = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning()) {
                    if (!queue.isEmpty()) {
                        try {
                            Thread.sleep(Duration.ofSeconds(5).toMillis());
                        } catch (InterruptedException ie) {
                            log.info("Interrupted while waiting for queue to be processed.", ie);
                            return;
                        }
                    } else {
                        List<SynthesizeCommand> commands;
                        try {
                            commands = prevalence.execute(
                                    new GatherCandidatesQuery(
                                            priorityService, maximumNumberOfCandidates));
                        } catch (Exception e) {
                            log.error("Failed to gather candidates for synthesis", e);
                            continue;
                        }
                        // todo if empty, then sleep for a while
                        queue.addAll(commands);
                    }
                }
                populatorStoppedLatch.countDown();
                log.info("Thread stops.");
            }
        });
    }

    @Override
    protected void doStart() {
        populatorStoppedLatch = new CountDownLatch(1);
        populator.start();
        workers.start();
    }

    @Override
    protected void doStop() {
        try {
            populatorStoppedLatch.await();
        } catch (InterruptedException ie) {
            log.warn("Interrupted while waiting for populator to stop", ie);
        }
        workers.stop();
    }

    private void synthesize(SynthesizeCommand command) {
        try {
            log.info("Synthesizing {}", command);
            WikispeechApi.ListenResponseEnvelope responseEnvelope =
                    wikispeechApi.listen(
                            command.getConsumerUrl(),
                            command.getTitle(),
                            Hex.encodeHexString(command.getHash()),
                            command.getRevision(),
                            command.getLanguage(),
                            command.getVoice()
                    );
            prevalence.execute(
                    new CreateOrUpdatePageSegmentVoice(
                            command.getConsumerUrl(),
                            command.getTitle(),
                            command.getHash(),
                            responseEnvelope.getRevision(),
                            command.getVoice()
                    ));
        } catch (Exception e) {
            log.error("Failed to synthesize {}", command, e);
            // todo add failure to {@link PageSegmentVoice}
        }
    }

    @Data
    public static class SynthesizeCommand {
        private String consumerUrl;
        private String title;
        private long revision;
        private byte[] hash;
        private String language;
        private String voice;
    }

    @Data
    public static class GatherCandidatesQuery implements Query<Root, List<SynthesizeCommand>> {

        private PriorityService priorityService;

        private int maximumNumberOfCandidates;

        public GatherCandidatesQuery(PriorityService priorityService, int maximumNumberOfCandidates) {
            this.maximumNumberOfCandidates = maximumNumberOfCandidates;
            this.priorityService = priorityService;
        }

        @Override
        public List<SynthesizeCommand> query(Root root, Date date) throws Exception {
            Set<String> wikiLanguageDefaultVoices = new HashSet<>(1);
            wikiLanguageDefaultVoices.add(null);

            SegmentVoiceToBeSynthesizedComparator comparator = new SegmentVoiceToBeSynthesizedComparator(priorityService);
            se.wikimedia.wikispeech.prerender.PriorityQueue<SegmentVoiceToBeSynthesized> candidates = new PriorityQueue<>(maximumNumberOfCandidates, comparator);
            for (Wiki wiki : root.getWikiByConsumerUrl().values()) {

                LocalDateTime resynthesizeTimestamp = LocalDateTime.now().plus(wiki.getMaximumSynthesizedVoiceAge());

                for (Page page : wiki.getPagesByTitle().values()) {

                    if (page.getRevisionAtSegmentation() == null) {
                        // not segmented
                        continue;
                    }

                    String language = page.getLanguageAtSegmentation() != null ? page.getLanguageAtSegmentation() : wiki.getDefaultLanguage();
                    Set<String> voices = wiki.getVoicesPerLanguage().get(language);
                    if (voices == null) {
                        voices = wikiLanguageDefaultVoices;
                    }

                    for (PageSegment segment : page.getSegments()) {
                        // add all registered voices as candidates
                        Map<String, PageSegmentVoice> synthesizedSegmentVoices = new HashMap<>(segment.getSynthesizedVoices().size());
                        if (segment.getSynthesizedVoices() != null) {
                            for (PageSegmentVoice pageSegmentVoice : segment.getSynthesizedVoices()) {
                                synthesizedSegmentVoices.put(pageSegmentVoice.getVoice(), pageSegmentVoice);
                            }
                        }
                        for (String voice : voices) {
                            PageSegmentVoice segmentVoice = synthesizedSegmentVoices.get(voice);
                            if (segmentVoice == null
                                    || segmentVoice.getTimestampSynthesized() == null
                                    || segmentVoice.getTimestampSynthesized().isAfter(resynthesizeTimestamp)) {
                                candidates.add(new SegmentVoiceToBeSynthesized(
                                        wiki, page, segment, segmentVoice, language, voice
                                ));
                            }
                        }
                    }

                }
            }

            List<SynthesizeCommand> response = new ArrayList<>(maximumNumberOfCandidates);
            for (SegmentVoiceToBeSynthesized candidate : candidates.toList()) {
                SynthesizeCommand item = new SynthesizeCommand();
                item.setConsumerUrl(candidate.getWiki().getConsumerUrl());
                item.setTitle(candidate.getPage().getTitle());
                item.setLanguage(candidate.getLanguage());
                item.setHash(candidate.getPageSegment().getHash());
                item.setRevision(candidate.getPage().getRevisionAtSegmentation());
                item.setVoice(candidate.getVoice());
                response.add(item);
            }
            return response;
        }

        @Data
        public static class SegmentVoiceToBeSynthesized {
            private Wiki wiki;
            private Page page;
            private PageSegment pageSegment;
            private PageSegmentVoice pageSegmentVoice;
            private String language;
            private String voice;

            public SegmentVoiceToBeSynthesized(Wiki wiki, Page page, PageSegment pageSegment, PageSegmentVoice pageSegmentVoice, String language, String voice) {
                this.wiki = wiki;
                this.page = page;
                this.pageSegment = pageSegment;
                this.pageSegmentVoice = pageSegmentVoice;
                this.language = language;
                this.voice = voice;
            }
        }

        public static class SegmentVoiceToBeSynthesizedComparator implements Comparator<SegmentVoiceToBeSynthesized> {

            private PriorityService priorityService;

            public SegmentVoiceToBeSynthesizedComparator(PriorityService priorityService) {
                this.priorityService = priorityService;
            }

            private Map<SegmentVoiceToBeSynthesized, Double> prioritiesCache = new HashMap<>(1000);

            public double calculatePriority(SegmentVoiceToBeSynthesized segmentVoiceToBeSynthesized) {

                double priority = 1D;
                priority *= segmentVoiceToBeSynthesized.getPage().getPriority();

                if (segmentVoiceToBeSynthesized.getPageSegmentVoice() != null
                        && segmentVoiceToBeSynthesized.getPageSegmentVoice().getFailedAttempts() != null
                        && !segmentVoiceToBeSynthesized.getPageSegmentVoice().getFailedAttempts().isEmpty()) {
                    priority /= segmentVoiceToBeSynthesized.getPageSegmentVoice().getFailedAttempts().size();
                }

                // lower segment index in page is slightly more prioritized
                priority += 1D - Math.min(1000, segmentVoiceToBeSynthesized.getPageSegment().getLowestIndexAtSegmentation())/1000D;

                // no synthesized voice at all is slightly more prioritized
                if (segmentVoiceToBeSynthesized.getPageSegmentVoice() == null) {
                    priority += 0.001D;
                }

                priority *= priorityService.getMultiplier(
                        segmentVoiceToBeSynthesized.getWiki(),
                        segmentVoiceToBeSynthesized.getPage(),
                        segmentVoiceToBeSynthesized.getPageSegment(),
                        segmentVoiceToBeSynthesized.getPageSegmentVoice(),
                        segmentVoiceToBeSynthesized.getVoice()
                );

                return priority;
            }

            @Override
            public int compare(SegmentVoiceToBeSynthesized o1, SegmentVoiceToBeSynthesized o2) {
                Double priority1 = prioritiesCache.get(o1);
                if (priority1 == null) {
                    priority1 = calculatePriority(o1);
                    prioritiesCache.put(o1, priority1);
                }

                Double priority2 = prioritiesCache.get(o2);
                if (priority2 == null) {
                    priority2 = calculatePriority(o2);
                    prioritiesCache.put(o2, priority2);
                }

                int ret = Double.compare(priority2, priority1);

                if (ret == 0) {
                    // pages that was segmented a long time ago is more prioritized
                    ret = o1.getPage().getTimestampSegmented().compareTo(o2.getPage().getTimestampSegmented());
                }

                return ret;
            }
        }

    }
}

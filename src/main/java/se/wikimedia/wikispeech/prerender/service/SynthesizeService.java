package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.mediawiki.WikispeechApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.AddSegmentVoiceFailure;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateOrUpdatePageSegmentVoice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SynthesizeService extends AbstractLifecycle implements SmartLifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    @Getter
    private List<CandidateToBeSynthesized> candidates = new ArrayList<>();
    @Getter
    private final Queue<CandidateToBeSynthesized> queue = new ConcurrentLinkedQueue<>();

    private final Prevalence prevalence;

    private final WikispeechApi wikispeechApi;

    private final PriorityService priorityService;

    private final int numberOfWorkerThreads = 2;
    private final int maximumNumberOfCandidates = 250;

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
                CandidateToBeSynthesized command = queue.poll();
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
                        List<CandidateToBeSynthesized> candidates;
                        try {
                            long started = System.currentTimeMillis();
                            candidates = prevalence.execute(
                                    new GatherCandidatesQuery());
                            long millisecondsSpend = System.currentTimeMillis() - started;
                            log.debug("Gathered {} segments to synthesize in {} milliseconds.", candidates.size(), millisecondsSpend);
                        } catch (Exception e) {
                            log.error("Failed to gather candidates for synthesis", e);
                            continue;
                        }
                        // todo if empty, then sleep for a while
                        long started = System.currentTimeMillis();
                        candidates.sort(new GatherCandidatesQuery.SegmentVoiceToBeSynthesizedComparator(priorityService, true));
                        long millisecondsSpend = System.currentTimeMillis() - started;
                        log.debug("Prioritized {} segments to synthesize in {} milliseconds.", candidates.size(), millisecondsSpend);
                        queue.addAll(candidates);
                        SynthesizeService.this.candidates = candidates;

                        if (candidates.isEmpty()) {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException ie) {
                                log.error("Interrupted while pausing to await new segments.", ie);
                                return;
                            }
                        }

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

    private void synthesize(CandidateToBeSynthesized candidate) {
        try {
            log.info("Synthesizing voice {} for hash {} in page {} at {}", candidate.getVoice(), candidate.getPageSegment().getHash(), candidate.getPage().getTitle(),candidate.getWiki().getConsumerUrl());
            WikispeechApi.ListenResponseEnvelope responseEnvelope =
                    wikispeechApi.listen(
                            candidate.getWiki().getConsumerUrl(),
                            candidate.getPage().getTitle(),
                            Hex.encodeHexString(candidate.getPageSegment().getHash()),
                            candidate.getPage().getRevisionAtSegmentation(),
                            candidate.getLanguage(),
                            candidate.getVoice()
                    );
            prevalence.execute(
                    new CreateOrUpdatePageSegmentVoice(
                            candidate.getWiki().getConsumerUrl(),
                            candidate.getPage().getTitle(),
                            candidate.getPageSegment().getHash(),
                            responseEnvelope.getRevision(),
                            candidate.getVoice()
                    ));
        } catch (Exception e) {
            log.error("Failed to synthesize {}", candidate, e);
            StringWriter stacktrace = new StringWriter(4096);
            stacktrace.append(e.getMessage());
            stacktrace.append("\n");
            PrintWriter pw = new PrintWriter(stacktrace);
            e.printStackTrace(pw);
            pw.flush();
            prevalence.execute(
                    new AddSegmentVoiceFailure(
                            candidate.getWiki().getConsumerUrl(),
                            candidate.getPage().getTitle(),
                            candidate.getPageSegment().getHash(),
                            candidate.getVoice(),
                            stacktrace.toString()
                    ));
        }
    }

    @Data
    public static class GatherCandidatesQuery implements Query<Root, List<CandidateToBeSynthesized>> {

        @Override
        public List<SynthesizeService.CandidateToBeSynthesized> query(Root root, Date date) throws Exception {
            Set<String> wikiLanguageDefaultVoices = new HashSet<>(1);
            wikiLanguageDefaultVoices.add(null);

            List<SynthesizeService.CandidateToBeSynthesized> candidates = new ArrayList<>(25000);
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
                                candidates.add(new SynthesizeService.CandidateToBeSynthesized(
                                        wiki, page, segment, segmentVoice, language, voice
                                ));
                            }
                        }
                    }

                }
            }
            return candidates;
        }

        public static class SegmentVoiceToBeSynthesizedComparator implements Comparator<SynthesizeService.CandidateToBeSynthesized> {

            private PriorityService priorityService;
            private boolean explain;

            public SegmentVoiceToBeSynthesizedComparator(PriorityService priorityService, boolean explain) {
                this.priorityService = priorityService;
                this.explain = explain;
            }

            @Override
            public int compare(SynthesizeService.CandidateToBeSynthesized o1, SynthesizeService.CandidateToBeSynthesized o2) {
                if (o1.getPriority() == null) {
                    o1.setPriority(priorityService.calculatePriority(o1, explain));
                }
                if (o2.getPriority() == null) {
                    o2.setPriority(priorityService.calculatePriority(o2, explain));
                }

                int ret = Double.compare(o2.getPriority().getValue(), o1.getPriority().getValue());

                if (ret == 0) {
                    // pages that was segmented a long time ago is more prioritized
                    ret = o1.getPage().getTimestampSegmented().compareTo(o2.getPage().getTimestampSegmented());
                }

                return ret;
            }
        }

    }

    @Data
    public static class CandidateToBeSynthesized {
        private PriorityService.CalculatedPriority priority;
        private Wiki wiki;
        private Page page;
        private PageSegment pageSegment;
        private PageSegmentVoice pageSegmentVoice;
        private String language;
        private String voice;

        public CandidateToBeSynthesized(Wiki wiki, Page page, PageSegment pageSegment, PageSegmentVoice pageSegmentVoice, String language, String voice) {
            this.wiki = wiki;
            this.page = page;
            this.pageSegment = pageSegment;
            this.pageSegmentVoice = pageSegmentVoice;
            this.language = language;
            this.voice = voice;
        }
    }
}

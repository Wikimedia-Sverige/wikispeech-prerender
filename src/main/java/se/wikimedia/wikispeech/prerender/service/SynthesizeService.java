package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.mediawiki.WikispeechApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.AddSegmentVoiceFailure;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.FlushSegmentPageVoice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

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

    private final SegmentService segmentService;

    /**
     * If gathered candidates is more than this number,
     * then cut candidates at end of queue and flush from prevalence.
     */
    private final int flushCandidatesThreshold = 100000;

    private final int numberOfWorkerThreads = 2;
    private final int maximumNumberOfCandidates = 250;

    private ExecutorService workers;

    @Autowired
    public SynthesizeService(
            Prevalence prevalence,
            WikispeechApi wikispeechApi,
            PriorityService priorityService,
            SegmentService segmentService
    ) {
        this.prevalence = prevalence;
        this.wikispeechApi = wikispeechApi;
        this.priorityService = priorityService;
        this.segmentService = segmentService;

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
    }


    @Override
    protected void doStart() {
        workers.start();
    }

    @Override
    protected void doStop() {
        workers.stop();
    }

    @Scheduled(initialDelay = 10, fixedDelay = 60 * 5, timeUnit = TimeUnit.SECONDS)
    public void repopulateCandidates() {
        log.info("Repopulating candidates to be synthesized...");
        List<CandidateToBeSynthesized> candidates;
        try {
            long started = System.currentTimeMillis();
            candidates = prevalence.execute(new GatherCandidatesQuery());
            long millisecondsSpend = System.currentTimeMillis() - started;
            log.debug("Gathered {} segments to synthesize in {} milliseconds.", candidates.size(), millisecondsSpend);
        } catch (Exception e) {
            log.error("Failed to gather candidates for synthesis", e);
            return;
        }

        long started = System.currentTimeMillis();
        candidates.sort(new GatherCandidatesQuery.SegmentVoiceToBeSynthesizedComparator(priorityService, false));
        long millisecondsSpend = System.currentTimeMillis() - started;
        log.debug("Prioritized {} segments to synthesize in {} milliseconds.", candidates.size(), millisecondsSpend);

        if (candidates.size() >= flushCandidatesThreshold) {
            log.info("There are {} candidates, {} will be flushed...", candidates.size(), candidates.size() - flushCandidatesThreshold);
            List<CandidateToBeSynthesized> candidatesToBeFlushed = candidates.subList(flushCandidatesThreshold, candidates.size() - 1);
            Map<Wiki, Set<Page>> pagesTouchedPerWiki = new HashMap<>();
            int segmentVoicesFlushed = 0;
            for (CandidateToBeSynthesized candidate : candidatesToBeFlushed) {
                Set<Page> pagesTouched = pagesTouchedPerWiki.computeIfAbsent(candidate.getWiki(), k -> new HashSet<>(10000));
                pagesTouched.add(candidate.getPage());
                // remove pagesegementvoice from pagesegment
                // and remove segment from page if no voices left
                // and remove page from wiki if no segments left
                prevalence.execute(new FlushSegmentPageVoice(
                        candidate.getWiki().getConsumerUrl(),
                        candidate.getPage().getTitle(),
                        candidate.getPageSegment().getHash(),
                        candidate.getVoice()));
                segmentVoicesFlushed++;
            }
            log.info("Flushed out {} voices from {} pages in {} wikis", segmentVoicesFlushed, pagesTouchedPerWiki.values().stream().mapToInt(Set::size).sum(), pagesTouchedPerWiki.size());
            candidates = candidates.subList(0, flushCandidatesThreshold);
        }

        queue.addAll(candidates);
        SynthesizeService.this.candidates = candidates;

    }

    private void synthesize(CandidateToBeSynthesized candidate) {
        try {
            log.info("Synthesizing voice '{}' for hash {} at segment index {} in page '{}' of wiki {} with priority {}", candidate.getVoice(), Base64.getEncoder().encodeToString(candidate.getPageSegment().getHash()), candidate.getPageSegment().getLowestIndexAtSegmentation(), candidate.getPage().getTitle(), candidate.getWiki().getConsumerUrl(), candidate.getPriority().getValue());
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
                    new FlushSegmentPageVoice(
                            candidate.getWiki().getConsumerUrl(),
                            candidate.getPage().getTitle(),
                            candidate.getPageSegment().getHash(),
                            candidate.getVoice()
                    ));
        } catch (Exception e) {

            if (e instanceof WikispeechApi.MWException) {
                WikispeechApi.MWException mwException = (WikispeechApi.MWException) e;
                if (mwException.getError().get("error").hasNonNull("errorclass")) {
                    String errorClass = mwException.getError().get("error").get("errorclass").textValue();
                    if ("MediaWiki\\\\Wikispeech\\\\Segment\\\\OutdatedOrInvalidRevisionException".equals(errorClass)) {
                        // send page back to segmentation
                        segmentService.queue(candidate.getWiki().getConsumerUrl(), candidate.getPage().getTitle());
                        return;
                    }
                }
            }

            log.error("Failed to synthesize voice '{}' for hash {} at segment index {} in page '{}' of wiki {} with priority {}", candidate.getVoice(), Base64.getEncoder().encodeToString(candidate.getPageSegment().getHash()), candidate.getPageSegment().getLowestIndexAtSegmentation(), candidate.getPage().getTitle(), candidate.getWiki().getConsumerUrl(), candidate.getPriority().getValue(), e);
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

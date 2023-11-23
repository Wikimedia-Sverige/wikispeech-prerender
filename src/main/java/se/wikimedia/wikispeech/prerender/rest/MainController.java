package se.wikimedia.wikispeech.prerender.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.LocalCache;
import se.wikimedia.wikispeech.prerender.mediawiki.PageApi;
import se.wikimedia.wikispeech.prerender.mediawiki.WikispeechApi;
import se.wikimedia.wikispeech.prerender.service.*;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.PageSegmentVoiceReference;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@RestController()
@RequestMapping(path = "/api")
public class MainController {

    private final Prevalence prevalence;
    private final WikispeechApi wikispeechApi;
    private final PageApi pageApi;
    private final SegmentService segmentService;
    private final SynthesizeService synthesizeService;
    private final YesterdaysMostReadArticlesPrioritizer yesterdaysMostReadPrioritizer;
    private final ObjectMapper objectMapper;

    @Autowired
    public MainController(
            Prevalence prevalence,
            SegmentService segmentService,
            PageApi pageApi,
            WikispeechApi wikispeechApi,
            SynthesizeService synthesizeService,
            YesterdaysMostReadArticlesPrioritizer yesterdaysMostReadPrioritizer
    ) {
        this.prevalence = prevalence;
        this.pageApi = pageApi;
        this.wikispeechApi = wikispeechApi;
        this.segmentService = segmentService;
        this.synthesizeService = synthesizeService;
        this.yesterdaysMostReadPrioritizer = yesterdaysMostReadPrioritizer;

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            path = "wiki",
            produces = "application/json"
    )
    public ResponseEntity<String> createWiki(
            @RequestParam String consumerUrl,
            @RequestParam(defaultValue = "60") Integer initialLastRecentChangesLimitInMinutes,
            @RequestParam(defaultValue = "10") Float mainPagePriority,
            @RequestParam(defaultValue = "30") Integer maximumSynthesizedVoiceAgeInDays
    ) throws Exception {

        Wiki wiki = new CreateWikiCommand(prevalence, segmentService, pageApi, consumerUrl)
                .setVoicesPerLanguage(null)
                .setMainPagePriority(mainPagePriority)
                .setTimestampOfLastRecentChangesItemProcessed(OffsetDateTime.now().minusMinutes(initialLastRecentChangesLimitInMinutes))
                .setMaximumSynthesizedVoiceAge(Duration.ofDays(maximumSynthesizedVoiceAgeInDays))
                .execute();

        return ResponseEntity.ok(objectMapper.writeValueAsString(wiki));
    }

    @Data
    private static class ConsumerUrlAndTitle {
        private String consumerUrl;
        private String title;

        public ConsumerUrlAndTitle(String consumerUrl, String title) {
            this.consumerUrl = consumerUrl;
            this.title = title;
        }
    }

    private LocalCache<ConsumerUrlAndTitle, List<WikispeechApi.Segment>> segments = new LocalCache<ConsumerUrlAndTitle, List<WikispeechApi.Segment>>(1000) {
        @Override
        public List<WikispeechApi.Segment> doGet(ConsumerUrlAndTitle key) {
            List<WikispeechApi.Segment> segments = new ArrayList<>(1000);
            try {
                wikispeechApi.segment(key.getConsumerUrl(), key.getTitle(), new Collector<WikispeechApi.Segment>() {
                    @Override
                    public boolean collect(WikispeechApi.Segment collected) {
                        segments.add(collected);
                        return true;
                    }
                });
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            return segments;
        }
    };

    private WikispeechApi.Segment getSegment(String consumerUrl, String title, byte[] hash) throws DecoderException {
        for (WikispeechApi.Segment segment : segments.get(new ConsumerUrlAndTitle(consumerUrl, title))) {
            if (Arrays.equals(Hex.decodeHex(segment.getHash()), hash)) {
                return segment;
            }
        }
        return null;
    }

    @Data
    public static class SynthesisErrorsResponse {
        private int totalHits;
        private List<SynthesisError> errors;

        @Data
        public static class SynthesisError {
            private String consumerUrl;
            private String title;
            private byte[] hash;
            private String voice;
            private LinkedHashMap<LocalDateTime, String> failedAttempts;
            private WikispeechApi.Segment segment;

            public SynthesisError() {
            }

            public SynthesisError(String consumerUrl, String title, byte[] hash, String voice, LinkedHashMap<LocalDateTime, String> failedAttempts, WikispeechApi.Segment segment) {
                this.consumerUrl = consumerUrl;
                this.title = title;
                this.hash = hash;
                this.voice = voice;
                this.failedAttempts = failedAttempts;
                this.segment = segment;
            }
        }
    }

    @Data
    public static class CandidateToBeSynthesizedDto {
        private PriorityService.CalculatedPriority priority;
        private String consumerUrl;
        private String title;
        private byte[] hash;
        private String voice;
        private Map<LocalDateTime, String> failures;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "synthesis/queue/candidates",
            produces = "application/json"
    )
    public ResponseEntity<List<CandidateToBeSynthesizedDto>> getSynthesisQueueCandidates(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "0") int startOffset
    ) throws Exception {
        List<CandidateToBeSynthesizedDto> dtos = new ArrayList<>(limit);
        for (SynthesizeService.CandidateToBeSynthesized candidate : synthesizeService.getCandidates().subList(startOffset, Math.min(startOffset + limit, synthesizeService.getCandidates().size() -1))) {
            CandidateToBeSynthesizedDto dto = new CandidateToBeSynthesizedDto();
            dto.setConsumerUrl(candidate.getWiki().getConsumerUrl());
            dto.setTitle(candidate.getPage().getTitle());
            dto.setHash(candidate.getPageSegment().getHash());
            dto.setVoice(candidate.getVoice());
            dto.setFailures(candidate.getPageSegmentVoice() != null ? candidate.getPageSegmentVoice().getFailedAttempts() : null);
            dto.setPriority(candidate.getPriority());
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }

    @RequestMapping(
            method = RequestMethod.DELETE,
            path = "synthesis/queue",
            produces = "application/json"
    )
    public void clearSynthesisQueue() {
        synthesizeService.getQueue().clear();
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "synthesis/errors",
            produces = "application/json"
    )
    public ResponseEntity<SynthesisErrorsResponse> getSynthesisErrors(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "0") int startOffset
    ) throws Exception {
        List<PageSegmentVoiceReference> references = prevalence.execute(new Query<Root, List<PageSegmentVoiceReference>>() {
            @Override
            public List<PageSegmentVoiceReference> query(Root root, Date date) throws Exception {
                List<PageSegmentVoiceReference> references = new ArrayList<>(1000);
                for (Wiki wiki : root.getWikiByConsumerUrl().values()) {
                    for (Page page : wiki.getPagesByTitle().values()) {
                        for (PageSegment pageSegment : page.getSegments()) {
                            for (PageSegmentVoice pageSegmentVoice : pageSegment.getSynthesizedVoices()) {
                                if (pageSegmentVoice.getFailedAttempts() != null
                                        && !pageSegmentVoice.getFailedAttempts().isEmpty()) {
                                    references.add(new PageSegmentVoiceReference(
                                            wiki, page, pageSegment, pageSegmentVoice
                                    ));
                                }
                            }
                        }
                    }
                }
                return references;
            }
        });
        references.sort(new Comparator<PageSegmentVoiceReference>() {
            @Override
            public int compare(PageSegmentVoiceReference o1, PageSegmentVoiceReference o2) {
                return o1.getPageSegmentVoice().getFailedAttempts().keySet().iterator().next()
                        .compareTo(o2.getPageSegmentVoice().getFailedAttempts().keySet().iterator().next());
            }
        });
        SynthesisErrorsResponse response = new SynthesisErrorsResponse();
        response.setTotalHits(references.size());
        response.setErrors(new ArrayList<>(limit));
        for (PageSegmentVoiceReference reference : references.subList(startOffset, Math.min(startOffset + limit, references.size() - 1))) {
            response.getErrors().add(new SynthesisErrorsResponse.SynthesisError(
                    reference.getWiki().getConsumerUrl(),
                    reference.getPage().getTitle(),
                    reference.getPageSegment().getHash(),
                    reference.getPageSegmentVoice().getVoice(),
                    reference.getPageSegmentVoice().getFailedAttempts(),
                    getSegment(reference.getWiki().getConsumerUrl(), reference.getPage().getTitle(), reference.getPageSegment().getHash())
            ));
        }
        return ResponseEntity.ok(response);
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "page",
            produces = "application/json"
    )
    public ResponseEntity<Page> getPage(
            @RequestParam String consumerUrl,
            @RequestParam String title
    ) throws Exception {
        return ResponseEntity.ok(prevalence.execute(new Query<Root, Page>() {
            @Override
            public Page query(Root root, Date date) throws Exception {
                return root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title);
            }
        }));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "prevalence/snapshot",
            produces = "application/json"
    )
    public ResponseEntity<Page> snapshot() throws Exception {
        prevalence.takeSnapshot();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
        return ResponseEntity.ok().build();
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "prioritizeYesterdaysMetrics",
            produces = "application/json"
    )
    public ResponseEntity<Void> prioritizeYesterdaysMetrics() throws Exception {
        yesterdaysMostReadPrioritizer.process();
        return ResponseEntity.ok().build();
    }
}
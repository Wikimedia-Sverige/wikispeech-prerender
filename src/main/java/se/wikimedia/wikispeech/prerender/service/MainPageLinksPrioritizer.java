package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.mediawiki.PageApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.GetPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateOrGetPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.SetPageTimestampDontFlushUntil;
import se.wikimedia.wikispeech.prerender.site.ScrapePageForWikiLinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MainPageLinksPrioritizer {

    private final Logger log = LogManager.getLogger(getClass());

    private final Prevalence prevalence;
    private final PriorityService priorityService;
    private final SegmentService segmentService;
    private final PageApi pageApi;

    private final double priorityMultiplier;
    private final Duration priorityTimeToLive;
    private final Duration timestampDontFlushUntil;


    @Autowired
    public MainPageLinksPrioritizer(
            Prevalence prevalence,
            PriorityService priorityService,
            SegmentService segmentService,
            PageApi pageApi,
            Settings settings
    ) {
        this.prevalence = prevalence;
        this.priorityService = priorityService;
        this.segmentService = segmentService;
        this.pageApi = pageApi;

        priorityMultiplier = settings.getDouble("MainPageLinksPrioritizer.priorityMultiplier", 5d);
        priorityTimeToLive = settings.getDuration("MainPageLinksPrioritizer.priorityTimeToLive", Duration.ofDays(1));
        timestampDontFlushUntil = settings.getDuration("MainPageLinksPrioritizer.timestampDontFlushUntil", Duration.ofDays(5));

    }

    private final Map<String, OffsetDateTime> lastChangedByWikiConsumerUrl = new HashMap<>();

    private boolean initialRun = true;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES, initialDelay = 0)
    public void run() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime priorityTimeToLive = now.plus(this.priorityTimeToLive);
        LocalDateTime timestampDontFlushUntil = now.plus(this.timestampDontFlushUntil);

        for (Wiki wiki : prevalence.execute(new Query<Root, Set<Wiki>>() {
            @Override
            public Set<Wiki> query(Root root, Date date) {
                Set<Wiki> wikis = new HashSet<>(root.getWikiByConsumerUrl().values());
                wikis.removeIf( w -> w.getMainPage() == null);
                return wikis;
            }
        })) {
            OffsetDateTime lastChanged = OffsetDateTime.parse(pageApi.getHttpHeaders(wiki.getConsumerUrl(), wiki.getMainPage().getTitle()).get("Last-Modified"), DateTimeFormatter.RFC_1123_DATE_TIME);
            OffsetDateTime previousLastChanged = lastChangedByWikiConsumerUrl.put(wiki.getConsumerUrl(), lastChanged);
            if (initialRun || !lastChanged.equals(previousLastChanged)) {
                // re-segment main page
                segmentService.segment(wiki.getConsumerUrl(), wiki.getMainPage().getTitle());

                // priority service is not persistent, we need to reapply priority from main page on restart.
                initialRun = false;
                log.info("Setting priority for links in {} of {}", wiki.getMainPage().getTitle(), wiki.getName());
                ScrapePageForWikiLinks scraper = new ScrapePageForWikiLinks();
                scraper.setConsumerUrl(wiki.getConsumerUrl());
                scraper.setTitle(wiki.getMainPage().getTitle());
                scraper.setCollector(new Collector<String>() {
                    @Override
                    public boolean collect(String title) {

                        // don't flush pages linked from main page in x days.
                        try {
                            Page page = prevalence.execute(new GetPage(wiki.getConsumerUrl(), title));
                            if (page == null)
                                page = prevalence.execute(new CreateOrGetPage(wiki.getConsumerUrl(), title));
                            if (page.getTimestampDontFlushUntil() == null || page.getTimestampDontFlushUntil().isBefore(timestampDontFlushUntil))
                                prevalence.execute(new SetPageTimestampDontFlushUntil(wiki.getConsumerUrl(), title, timestampDontFlushUntil));
                        } catch (Exception e) {
                            log.error("Failed to set timestampDontFlushUntil of page {} linked from main page on wiki {}", title, wiki.getConsumerUrl(), e);
                        }

                        // keep priority of factor 5 for x days
                        priorityService.put(
                                new ConsumerUrlAndTitle(wiki.getConsumerUrl(), title),
                                new PriorityService.PagePrioritySetting(
                                        now, priorityTimeToLive, priorityMultiplier,
                                        wiki.getConsumerUrl(), title
                                )
                        );
                        segmentService.queue(wiki.getConsumerUrl(), title);
                        return true;
                    }
                });
                scraper.execute();

            }
        }
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
}

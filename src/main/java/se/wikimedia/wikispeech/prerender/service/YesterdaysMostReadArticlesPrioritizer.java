package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.wikimedia.wikispeech.prerender.mediawiki.PageApi;
import se.wikimedia.wikispeech.prerender.mediawiki.PageUtil;
import se.wikimedia.wikispeech.prerender.mediawiki.WikipediaMetricsApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.GetPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.GetWikis;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateNonSegmentedPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateOrGetPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.SetPageTimestampDontFlushUntil;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class YesterdaysMostReadArticlesPrioritizer {

    private final Logger log = LogManager.getLogger(getClass());

    private final Prevalence prevalence;
    private final PriorityService priorityService;
    private final SegmentService segmentService;
    private final WikipediaMetricsApi wikipediaMetricsApi;
    private final Settings settings;
    private final PageApi pageApi;


    @Autowired
    public YesterdaysMostReadArticlesPrioritizer(
            Settings settings,
            Prevalence prevalence,
            PriorityService priorityService,
            SegmentService segmentService,
            WikipediaMetricsApi wikipediaMetricsApi,
            PageApi pageApi
    ) {
        this.settings = settings;
        this.prevalence = prevalence;
        this.priorityService = priorityService;
        this.segmentService = segmentService;
        this.wikipediaMetricsApi = wikipediaMetricsApi;
        this.pageApi = pageApi;
    }

    @Scheduled(fixedDelay = 30, initialDelay = 0, timeUnit = TimeUnit.MINUTES)
    public void process() throws Exception {
        // todo should be yesterday seen from the tz of the remote server
        // todo check response header date: Tue, 28 Mar 2023 22:33:23 GMT
        process(LocalDate.now().minusDays(1),
                settings.getDouble("YesterdaysMostReadArticlesPrioritizer.topRankMultiplier", 4d),
                settings.getDuration("YesterdaysMostReadArticlesPrioritizer.priorityTimeToLive", Duration.ofDays(1)),
                settings.getDuration("YesterdaysMostReadArticlesPrioritizer.dontFlushPageUntil", Duration.ofDays(3)),
                settings.getInteger("YesterdaysMostReadArticlesPrioritizer.maximumArticles", 200)
        );
    }

    private final Map<Wiki, WikipediaMetricsApi.PageViews> mostRecentlyProcessedMetricsPerWiki = new HashMap<>();

    public void process(
            LocalDate date,
            double topRankMultiplier,
            Duration priorityTimeToLive,
            Duration dontFlushPageUntil,
            int maximumArticles
    ) throws Exception {
        for (Wiki wiki : prevalence.execute(new GetWikis())) {
            WikipediaMetricsApi.PageViews pageViews = getPageViews(wiki, date);
            WikipediaMetricsApi.PageViews previousProcessedPageViews = mostRecentlyProcessedMetricsPerWiki.get(wiki);
            if (pageViews != null && !pageViews.equals(previousProcessedPageViews)) {
                process(wiki, pageViews, topRankMultiplier, priorityTimeToLive, dontFlushPageUntil, maximumArticles);
                mostRecentlyProcessedMetricsPerWiki.put(wiki, pageViews);
            }
        }
    }

    public void process(
            Wiki wiki,
            WikipediaMetricsApi.PageViews pageViews,
            double topRankMultiplier,
            Duration priorityTimeToLive,
            Duration dontFlushPageUntil,
            int maximumArticles
    ) throws Exception  {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timestampPriorityTimeToLive = now.plus(priorityTimeToLive);
        LocalDateTime timestampDontFlushPageUntil = now.plus(dontFlushPageUntil);
        double multiplier = topRankMultiplier;
        double multiplierRankSubtrahend = 1d / (double)maximumArticles;
        int articlesCounter = 0;
        for (WikipediaMetricsApi.PageViewArticle article : pageViews.getArticles()) {

            // the article name contains "_" instead of " ", etc, so we clean it up this way
            // so that it's normalized with data retrieved from elsewhere in this project.

            PageApi.PageInfo pageInfo = pageApi.getPageInfo(wiki.getConsumerUrl(), article.getArticle());
            if (pageInfo == null)
                // this is probably a special page. Search, recent changes, etc.
                continue;

            String title = pageInfo.getTitle();

            Page page = prevalence.execute(new GetPage(wiki.getConsumerUrl(), title));
            if (page == null)
                page = prevalence.execute(new CreateOrGetPage(wiki.getConsumerUrl(), title));

            if (page.getTimestampDontFlushUntil() == null || page.getTimestampDontFlushUntil().isBefore(timestampDontFlushPageUntil))
                prevalence.execute(new SetPageTimestampDontFlushUntil(wiki.getConsumerUrl(), title, timestampDontFlushPageUntil));

            segmentService.queue(wiki.getConsumerUrl(), title);

            priorityService.put(
                    new ConsumerUrlAndTitle(wiki.getConsumerUrl(), title),
                    new PriorityService.PagePrioritySetting(now, timestampPriorityTimeToLive, multiplier, wiki.getConsumerUrl(), title));

            if (articlesCounter++ >= maximumArticles) break;
            multiplier -= multiplierRankSubtrahend;
        }
    }

    private WikipediaMetricsApi.PageViews getPageViews(Wiki wiki, LocalDate date) throws IOException {
        String host = new URL(wiki.getConsumerUrl()).getHost();
        if (!host.endsWith(".wikipedia.org")) throw new UnsupportedOperationException("Wiki with consumerUrl " + wiki.getConsumerUrl() + "is not supported by Wikipedia metrics.");
        String metricsWikiName = host.replaceAll("^([a-z]+\\.wikipedia)\\.org$", "$1");
        return wikipediaMetricsApi.getPageViewsTop(metricsWikiName, date);
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

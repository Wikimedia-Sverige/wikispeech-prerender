package se.wikimedia.wikispeech.prerender.site;

import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushSegmentPageAndQueueForSynthesis;

import java.util.Collections;
import java.util.List;

public class EnglishWikipedia extends RemoteSite {

    public static final String CONSUMER_URL_EN_WIKIPEDIA = "https://en.wikipedia.org/w";

    @Override
    public String getName() {
        return "English Wikipedia";
    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public List<String> getVoices() {
        return Collections.singletonList(null);
    }

    @Override
    public String getConsumerUrl() {
        return CONSUMER_URL_EN_WIKIPEDIA;
    }

    private RecentChangesPoller recentChangesPoller;

    @Override
    public void start() {
        recentChangesPoller = new RecentChangesPoller(this);
        recentChangesPoller.start();
    }

    @Override
    public void stop() {
        recentChangesPoller.stop();
    }

    @Override
    public void queueCommands() throws Exception {

        // synthesize main page.
        Prevalence.getInstance().execute(
                PushSegmentPageAndQueueForSynthesis.factory(
                        getConsumerUrl(),
                        "Main_Page",
                        getLanguage(),
                        getVoices()
                )
        );

        // scrape main page content for wiki links and synthesize those.
        Prevalence.getInstance().execute(
                PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation.factory(
                        getConsumerUrl(),
                        "Main_Page",
                        getLanguage(),
                        getVoices(),
                        "//*[@id='mp-upper']//A[starts-with(@href, '/wiki/')]",
                        "/wiki/[^:]+"
                )
        );
    }
}

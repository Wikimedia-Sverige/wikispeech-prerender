package se.wikimedia.wikispeech.prerender.site;

import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushSegmentPageAndQueueForSynthesis;

import java.util.Collections;
import java.util.List;

/**
 * Gathers the titles of the 5000 most edited pages.
 */
public class SwedishWikipedia extends RemoteSite {

    public static final String CONSUMER_URL_SV_WIKIPEDIA = "https://sv.wikipedia.org/w";

    @Override
    public String getName() {
        return "Svenska Wikipedia";
    }

    @Override
    public String getLanguage() {
        return "sv";
    }

    @Override
    public List<String> getVoices() {
        return Collections.singletonList(null);
    }

    @Override
    public String getConsumerUrl() {
        return CONSUMER_URL_SV_WIKIPEDIA;
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

//        Prevalence.getInstance().execute(
//                PushCrawlSite.factory(
//                        getConsumerUrl(),
//                        "Portal:Huvudsida",
//                        5,
//                        getLanguage(),
//                        getVoices(),
//                        "//DIV[@id='bodyContent']//A[starts-with(@href, '/wiki/')]",
//                        "/wiki/[^:]+"
//                )
//        );
//
//
//        if (true) {
//            return;
//        }
//

        // synthesize main page.
        Prevalence.getInstance().execute(
                PushSegmentPageAndQueueForSynthesis.factory(
                        getConsumerUrl(),
                        "Portal:Huvudsida",
                        getLanguage(),
                        getVoices()
                )
        );


        // scrape main page content for wiki links and synthesize those.
        Prevalence.getInstance().execute(
                PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation.factory(
                        getConsumerUrl(),
                        "Portal:Huvudsida",
                        getLanguage(),
                        getVoices(),
                        "//DIV[@class='frontPageLeft']//A[starts-with(@href, '/wiki/')]",
                        "/wiki/[^:]+"
                )
        );

    }

}

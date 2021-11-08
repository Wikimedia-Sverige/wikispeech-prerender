package se.wikimedia.wikispeech.prerender.site;

import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushSegmentPageAndQueueForSynthesis;

import java.time.LocalDateTime;
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

    @Override
    public void queueCommands() throws Exception {
        // synthesize main page.
        for (String voice : getVoices()) {
            Prevalence.getInstance().execute(
                    PushSegmentPageAndQueueForSynthesis.factory(
                            getConsumerUrl(),
                            "Portal:Huvudsida",
                            getLanguage(),
                            voice
                    )
            );
        }

        // scrape main page content for wiki links and synthesize those.
        for (String voice : getVoices()) {
            Prevalence.getInstance().execute(
                    PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation.factory(
                            getConsumerUrl(),
                            "Portal:Huvudsida",
                            getLanguage(),
                            voice,
                            "//DIV[@class='frontPageLeft']//A[starts-with(@href, '/wiki/')]",
                            "/wiki/[^:]+"
                    )
            );
        }
    }

}

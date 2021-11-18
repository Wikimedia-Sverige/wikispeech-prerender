package se.wikimedia.wikispeech.prerender.site;


import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

/**
 * Gathers the titles of the 5000 most edited pages.
 */
public class SwedishWikipedia  {

    public static final String CONSUMER_URL_SV_WIKIPEDIA = "https://sv.wikipedia.org/w";

    public static double calculatePriority(
            Wiki wiki,
            Page page,
            PageSegment pageSegment,
            PageSegmentVoice pageSegmentVoice,
            String voice
    ) {
        double priority = 1d;
        if ("Portal:Huvudsida".equals(page.getTitle())) {
            priority *= 10d;
        }

        if (pageSegmentVoice != null && pageSegmentVoice.getFailedAttempts() != null && !pageSegmentVoice.getFailedAttempts().isEmpty()) {
            priority /= pageSegmentVoice.getFailedAttempts().size();
        }

        priority += 1d - Math.min(10000, pageSegment.getLowestIndexAtSegmentation())/10000d;



        return priority;
    }



    //
//    @Override
//    public String getName() {
//        return "Svenska Wikipedia";
//    }
//
//    @Override
//    public String getLanguage() {
//        return "sv";
//    }
//
//    @Override
//    public List<String> getVoices() {
//        return Collections.singletonList(null);
//    }
//
//    @Override
//    public String getConsumerUrl() {
//        return CONSUMER_URL_SV_WIKIPEDIA;
//    }
//
//    @Override
//    public void start() {
//        recentChangesPoller = new RecentChangesPoller(this);
//        recentChangesPoller.start();
//    }
//
//    @Override
//    public void stop() {
//        recentChangesPoller.stop();
//    }
//
//    @Override
//    public void queueCommands() throws Exception {
//
////        Prevalence.getInstance().execute(
////                PushCrawlSite.factory(
////                        getConsumerUrl(),
////                        "Portal:Huvudsida",
////                        5,
////                        getLanguage(),
////                        getVoices(),
////                        "//DIV[@id='bodyContent']//A[starts-with(@href, '/wiki/')]",
////                        "/wiki/[^:]+"
////                )
////        );
////
////
////        if (true) {
////            return;
////        }
////
//
//        // synthesize main page.
//        Prevalence.getInstance().execute(
//                PushSegmentPageAndQueueForSynthesis.factory(
//                        getConsumerUrl(),
//                        "Portal:Huvudsida",
//                        getLanguage(),
//                        getVoices(),
//                        null
//                )
//        );
//
//
//        // scrape main page content for wiki links and synthesize those.
//        Prevalence.getInstance().execute(
//                PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation.factory(
//                        getConsumerUrl(),
//                        "Portal:Huvudsida",
//                        getLanguage(),
//                        getVoices(),
//                        "//DIV[@class='frontPageLeft']//A[starts-with(@href, '/wiki/')]",
//                        "/wiki/[^:]+"
//                )
//        );
//
//    }

}

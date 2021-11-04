package se.wikimedia.wikispeech.prerender.site;

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
    public void collectTitles(TitleCollector collector) {
        collector.collect("Portal:Huvudsida", LocalDateTime.now());
        try {
            new MainPageParser("//DIV[@class='frontPageLeft']").collect(collector, "https://sv.wikipedia.org/wiki/Portal:Huvudsida");
            new SpecialPagesWithMostVersionsParser().collect(collector, "https://sv.wikipedia.org/w/index.php?title=Special:Flest_versioner&limit=5000&offset=0");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

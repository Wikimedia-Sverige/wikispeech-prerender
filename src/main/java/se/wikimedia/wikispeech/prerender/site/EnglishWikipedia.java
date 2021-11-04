package se.wikimedia.wikispeech.prerender.site;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Gathers the titles of the 5000 most edited pages.
 */
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

    @Override
    public void collectTitles(TitleCollector collector) {
        collector.collect("Main_Page", LocalDateTime.now());
        try {
            new MainPageParser("//TABLE[@id='mp-upper']").collect(collector, "https://en.wikipedia.org/wiki/Main_Page");
            // todo top 25
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

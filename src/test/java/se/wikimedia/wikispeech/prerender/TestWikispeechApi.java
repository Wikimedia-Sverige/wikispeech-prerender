package se.wikimedia.wikispeech.prerender;

import org.junit.Test;
import se.wikimedia.wikispeech.prerender.mediawiki.WikispeechApi;
import se.wikimedia.wikispeech.prerender.site.SwedishWikipedia;

import java.util.ArrayList;
import java.util.List;

public class TestWikispeechApi {

    @Test
    public void test() throws Exception {
        String consumerUrl = SwedishWikipedia.CONSUMER_URL_SV_WIKIPEDIA;
        WikispeechApi api = new WikispeechApi();
        api.open();
        String title = "Barack Obama";
        List<WikispeechApi.Segment> segments = new ArrayList<>();
        api.segment(consumerUrl, "Barack Obama", new Collector<WikispeechApi.Segment>() {
            @Override
            public void collect(WikispeechApi.Segment collected) {
                segments.add(collected);
            }
        });
        long currentRevision = api.getCurrentRevision(consumerUrl, title);
        for (WikispeechApi.Segment segment : segments) {
            WikispeechApi.ListenResponseEnvelope response = api.listen(consumerUrl, title, segment.getHash(), currentRevision, "sv");
            System.currentTimeMillis();
        }
        System.currentTimeMillis();
    }
}

package se.wikimedia.wikispeech.prerender;

import org.junit.Test;
import se.wikimedia.wikispeech.prerender.site.SwedishWikipedia;

import java.util.List;

public class TestWikispeechApi {

    @Test
    public void test() throws Exception {
        String consumerUrl = SwedishWikipedia.CONSUMER_URL_SV_WIKIPEDIA;
        WikispeechApi api = new WikispeechApi();
        api.open();
        List<WikispeechApi.Segment> segments = api.segment(consumerUrl, "Barack Obama");
        long currentRevision = api.getCurrentRevision(consumerUrl, "Barack Obama");
        for (WikispeechApi.Segment segment : segments) {
            WikispeechApi.ListenResponse response = api.listen(consumerUrl, segment.getHash(), currentRevision, "sv");
            System.currentTimeMillis();
        }
        System.currentTimeMillis();
    }
}

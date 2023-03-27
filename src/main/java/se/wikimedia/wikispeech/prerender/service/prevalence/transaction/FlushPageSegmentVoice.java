package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

@Data
public class FlushPageSegmentVoice implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private String wikiConsumerUrl;
    private String pageTitle;
    private byte[] segmentHash;
    private String voice;

    public FlushPageSegmentVoice() {
    }

    public FlushPageSegmentVoice(String wikiConsumerUrl, String pageTitle, byte[] segmentHash, String voice) {
        this.wikiConsumerUrl = wikiConsumerUrl;
        this.pageTitle = pageTitle;
        this.segmentHash = segmentHash;
        this.voice = voice;
    }

    @Override
    public void executeOn(Root root, Date executionTime) {
        Wiki wiki = root.getWikiByConsumerUrl().get(wikiConsumerUrl);
        Page page = wiki.getPagesByTitle().get(pageTitle);

        for (Iterator<PageSegment> pageSegments = page.getSegments().iterator(); pageSegments.hasNext(); ) {
            PageSegment pageSegment = pageSegments.next();
            if (Arrays.equals(segmentHash, pageSegment.getHash())) {
                pageSegment.getSynthesizedVoices().removeIf(pageSegmentVoice -> voice.equals(pageSegmentVoice.getVoice()));
                if (pageSegment.getSynthesizedVoices() == null || pageSegment.getSynthesizedVoices().isEmpty())
                    pageSegments.remove();
                break;
            }
        }
    }

}

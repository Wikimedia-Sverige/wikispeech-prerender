package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Arrays;
import java.util.Date;

public class PageSegmentExists implements Query<Root, Boolean> {

    private String consumerUrl;
    private String title;
    private byte[] hash;

    public PageSegmentExists(String consumerUrl, String title, byte[] hash) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.hash = hash;
    }

    @Override
    public Boolean query(Root root, Date date) throws Exception {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        Page page = wiki.getPagesByTitle().get(title);
        if (page == null) return false;
        for (PageSegment segment : page.getSegments()) {
            if (Arrays.equals(hash, segment.getHash())) {
                return true;
            }
        }
        return false;
    }
}

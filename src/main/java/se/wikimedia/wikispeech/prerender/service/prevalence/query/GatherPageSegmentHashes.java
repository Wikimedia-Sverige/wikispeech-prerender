package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GatherPageSegmentHashes implements Query<Root, Set<byte[]>> {

    private String consumerUrl;
    private String title;

    public GatherPageSegmentHashes(String consumerUrl, String title) {
        this.consumerUrl = consumerUrl;
        this.title = title;
    }

    @Override
    public Set<byte[]> query(Root root, Date date) throws Exception {
        Page page = root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title);
        Set<byte[]> hashes = new HashSet<>(page.getSegments().size());
        for (PageSegment segment : page.getSegments()) {
            hashes.add(segment.getHash());
        }
        return hashes;
    }
}

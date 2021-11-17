package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;

import java.util.Date;

@Data
public class CreatePageSegment implements TransactionWithQuery<Root, PageSegment> {

    private static final long serialVersionUID = 1L;


    private String consumerUrl;
    private String title;
    private int lowestIndexAtSegmentation;
    private byte[] hash;

    public CreatePageSegment() {
    }

    public CreatePageSegment(String consumerUrl, String title, int lowestIndexAtSegmentation, byte[] hash) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.lowestIndexAtSegmentation = lowestIndexAtSegmentation;
        this.hash = hash;
    }

    @Override
    public PageSegment executeAndQuery(Root root, Date date) throws Exception {
        PageSegment pageSegment = new PageSegment();
        pageSegment.setLowestIndexAtSegmentation(lowestIndexAtSegmentation);
        pageSegment.setHash(hash);
        root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title).getSegments().add(pageSegment);
        return pageSegment;
    }
}

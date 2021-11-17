package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;

import java.time.OffsetDateTime;
import java.util.Date;

public class GetWikiTimestampOfLastRecentChangesItemProcessed implements Query<Root, OffsetDateTime> {

    private String consumerUrl;

    public GetWikiTimestampOfLastRecentChangesItemProcessed(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    @Override
    public OffsetDateTime query(Root root, Date date) throws Exception {
        return root.getWikiByConsumerUrl().get(consumerUrl).getTimestampOfLastRecentChangesItemProcessed();
    }
}

package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
public class SetWikiTimestampOfLastRecentChangesItemProcessed implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private OffsetDateTime timestampOfLastRecentChangesItemProcessed;

    public SetWikiTimestampOfLastRecentChangesItemProcessed(String consumerUrl, OffsetDateTime timestampOfLastRecentChangesItemProcessed) {
        this.consumerUrl = consumerUrl;
        this.timestampOfLastRecentChangesItemProcessed = timestampOfLastRecentChangesItemProcessed;
    }

    @Override
    public void executeOn(Root root, Date date) {
        root.getWikiByConsumerUrl().get(consumerUrl).setTimestampOfLastRecentChangesItemProcessed(timestampOfLastRecentChangesItemProcessed);
    }
}


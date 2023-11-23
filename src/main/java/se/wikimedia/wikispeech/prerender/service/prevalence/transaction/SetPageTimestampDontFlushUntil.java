package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class SetPageTimestampDontFlushUntil implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private LocalDateTime timestampDontFlushUntil;

    public SetPageTimestampDontFlushUntil() {
    }

    public SetPageTimestampDontFlushUntil(String consumerUrl, String title, LocalDateTime timestampDontFlushUntil) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.timestampDontFlushUntil = timestampDontFlushUntil;
    }

    @Override
    public void executeOn(Root root, Date executionTime) {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        Page page = wiki.getPagesByTitle().get(title);
        page.setTimestampDontFlushUntil(timestampDontFlushUntil);
    }
}

package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Data
public class SetWikiMainPage implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    public SetWikiMainPage(String consumerUrl, String title) {
        this.consumerUrl = consumerUrl;
        this.title = title;
    }

    @Override
    public void executeOn(Root root, Date date) {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        Page mainPage = wiki.getPagesByTitle().get(title);
        mainPage.setTimestampDontFlushUntil(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneOffset.UTC));
        wiki.setMainPage(mainPage);
    }
}


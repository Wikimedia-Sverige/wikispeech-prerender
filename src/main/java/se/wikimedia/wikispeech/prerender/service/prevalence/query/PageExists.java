package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Date;

public class PageExists implements Query<Root, Boolean> {

    private String consumerUrl;
    private String title;

    public PageExists(String consumerUrl, String title) {
        this.consumerUrl = consumerUrl;
        this.title = title;
    }

    @Override
    public Boolean query(Root root, Date date) throws Exception {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        if (wiki == null) {
            return null;
        }
        return wiki.getPagesByTitle().containsKey(title);
    }
}

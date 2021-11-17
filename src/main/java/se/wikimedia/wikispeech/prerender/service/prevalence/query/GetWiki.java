package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Date;

public class GetWiki implements Query<Root, Wiki> {

    private String consumerUrl;

    public GetWiki(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    @Override
    public Wiki query(Root root, Date date) throws Exception {
        return root.getWikiByConsumerUrl().get(consumerUrl);
    }
}

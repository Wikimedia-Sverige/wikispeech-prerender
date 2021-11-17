package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;

import java.util.Date;

public class GetWikiDefaultLanguage implements Query<Root, String> {

    private String consumerUrl;

    public GetWikiDefaultLanguage(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    @Override
    public String query(Root root, Date date) throws Exception {
        return root.getWikiByConsumerUrl().get(consumerUrl).getDefaultLanguage();
    }
}

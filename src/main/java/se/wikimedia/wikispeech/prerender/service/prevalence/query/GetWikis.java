package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class GetWikis implements Query<Root, Collection<Wiki>> {

    @Override
    public Collection<Wiki> query(Root root, Date executionTime) throws Exception {
        return new HashSet<Wiki>(root.getWikiByConsumerUrl().values());
    }
}

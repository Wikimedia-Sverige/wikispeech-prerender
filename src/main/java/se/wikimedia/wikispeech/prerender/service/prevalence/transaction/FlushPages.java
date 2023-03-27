package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.*;

@Data
public class FlushPages implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private Map<String, Collection<String>> pageTitlesByWikiConsumerUrl = new HashMap<>();

    public FlushPages() {
    }

    public FlushPages(Map<String, Collection<String>> pageTitlesByWikiConsumerUrl) {
        this.pageTitlesByWikiConsumerUrl = pageTitlesByWikiConsumerUrl;
    }

    @Override
    public void executeOn(Root root, Date executionTime) {
        for (Map.Entry<String, Collection<String>> pageTitlesByWikiConsumerUrl : this.pageTitlesByWikiConsumerUrl.entrySet()) {
            Wiki wiki = root.getWikiByConsumerUrl().get(pageTitlesByWikiConsumerUrl.getKey());
            for (String pageTitle : pageTitlesByWikiConsumerUrl.getValue()) {
                wiki.getPagesByTitle().remove(pageTitle);
            }
        }
    }
}

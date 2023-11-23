package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Date;

@Data
public class CreateOrGetPage implements TransactionWithQuery<Root, Page> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    public CreateOrGetPage() {
    }

    public CreateOrGetPage(String consumerUrl, String title) {
        this.consumerUrl = consumerUrl;
        this.title = title;
    }

    @Override
    public Page executeAndQuery(Root root, Date date) throws Exception {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        Page page = wiki.getPagesByTitle().get(title);
        if (page != null)
            return page;
        page = new Page();
        page.setTitle(title);
        wiki.getPagesByTitle().put(title, page);
        return page;
    }
}

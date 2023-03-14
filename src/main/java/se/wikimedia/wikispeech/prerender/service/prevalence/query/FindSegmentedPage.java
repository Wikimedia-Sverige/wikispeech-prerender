package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Date;

@Data
public class FindSegmentedPage implements Query<Root, Page> {

    private String consumerUrl;
    private String title;

    public FindSegmentedPage(String remoteSiteConsumerUrl, String pageTitle) {
        this.consumerUrl = remoteSiteConsumerUrl;
        this.title = pageTitle;
    }

    @Override
    public Page query(Root root, Date date) throws Exception {
        Wiki remoteSite = root.getWikiByConsumerUrl().get(consumerUrl);
        if (remoteSite == null) {
            return null;
        }
        return remoteSite.getPagesByTitle().get(title);

    }
}

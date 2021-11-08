package se.wikimedia.wikispeech.prerender.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SegmentedPage;

import java.util.Date;

@Data
public class FindSegmentedPage implements Query<Root, SegmentedPage> {

    private String consumerUrl;
    private String title;

    public FindSegmentedPage(String remoteSiteConsumerUrl, String pageTitle) {
        this.consumerUrl = remoteSiteConsumerUrl;
        this.title = pageTitle;
    }

    @Override
    public SegmentedPage query(Root root, Date date) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(consumerUrl);
        if (remoteSite == null) {
            return null;
        }
        return remoteSite.getPagesByTitle().get(title);

    }
}

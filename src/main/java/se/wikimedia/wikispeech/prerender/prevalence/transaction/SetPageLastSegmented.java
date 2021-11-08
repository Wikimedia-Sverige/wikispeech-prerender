package se.wikimedia.wikispeech.prerender.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;

import java.util.Date;

@Data
public class SetPageLastSegmented implements TransactionWithQuery<Root, SegmentedPage> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private Long revision;

    public SetPageLastSegmented() {
    }

    public SetPageLastSegmented(String consumerUrl, String title, Long revision) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.revision = revision;
    }

    @Override
    public SegmentedPage executeAndQuery(Root root, Date date) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(consumerUrl);
        if (remoteSite == null) {
            remoteSite = new RemoteSite();
            remoteSite.setConsumerUrl(consumerUrl);
            root.getRemoteSiteByConsumerUrl().put(consumerUrl, remoteSite);
        }
        SegmentedPage segmentedPage = remoteSite.getPagesByTitle().get(title);
        if (segmentedPage == null) {
            segmentedPage = new SegmentedPage();
            segmentedPage.setTitle(title);
            remoteSite.getPagesByTitle().put(title, segmentedPage);
        }
        segmentedPage.setLastSegmented(Prevalence.toLocalDateTime(date));
        segmentedPage.setLastSegmentedRevision(revision);
        return segmentedPage;
    }
}

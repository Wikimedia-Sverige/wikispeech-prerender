package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Date;

@Data
public class PageNeedsToBeSegmented implements Query<Root, Boolean> {

    private long currentRevision;
    private String consumerUrl;
    private String title;

    public PageNeedsToBeSegmented(String consumerUrl, String pageTitle, long currentRevision) {
        this.currentRevision = currentRevision;
        this.consumerUrl = consumerUrl;
        this.title = pageTitle;
    }

    @Override
    public Boolean query(Root root, Date date) throws Exception {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        if (wiki == null) {
            return true;
        }
        Page page = wiki.getPagesByTitle().get(title);
        return page == null
                || page.getTimestampSegmented() == null
                || page.getRevisionAtSegmentation() < currentRevision
                // If previously segmented too long ago, then requeue segments again.
                // Perhaps the speech synthesis has been updated since then.
                || page.getTimestampSegmented().plus(wiki.getMaximumSynthesizedVoiceAge()).isBefore(Prevalence.toLocalDateTime(date));
    }
}

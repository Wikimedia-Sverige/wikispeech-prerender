package se.wikimedia.wikispeech.prerender.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SegmentedPage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class PageNeedsToBeSegmented implements Query<Root, Boolean> {

    private Duration maximumSynthesizedVoiceAge;
    private long currentRevision;
    private String consumerUrl;
    private String title;

    public PageNeedsToBeSegmented(Duration maximumSynthesizedVoiceAge, long currentRevision, String consumerUrl, String pageTitle) {
        this.maximumSynthesizedVoiceAge = maximumSynthesizedVoiceAge;
        this.currentRevision = currentRevision;
        this.consumerUrl = consumerUrl;
        this.title = pageTitle;
    }

    @Override
    public Boolean query(Root root, Date date) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(consumerUrl);
        if (remoteSite == null) {
            return true;
        }
        SegmentedPage segmentedPage = remoteSite.getPagesByTitle().get(title);
        return segmentedPage == null
            || segmentedPage.getLastSegmentedRevision() == null
                || segmentedPage.getLastSegmentedRevision() < currentRevision
                // If previously segmented too long ago, then requeue segments again.
                // Perhaps the speech synthesis has been updated since then.
                || segmentedPage.getLastSegmented().plus(maximumSynthesizedVoiceAge).isBefore(Prevalence.toLocalDateTime(date));
    }
}

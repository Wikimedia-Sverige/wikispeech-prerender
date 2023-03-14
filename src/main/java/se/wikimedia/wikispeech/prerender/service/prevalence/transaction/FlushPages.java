package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;

@Data
public class FlushPages implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private LocalDateTime earliestAllowedTimestampSegmented;

    public FlushPages() {
    }

    public FlushPages(LocalDateTime earliestAllowedTimestampSegmented) {
        this.earliestAllowedTimestampSegmented = earliestAllowedTimestampSegmented;
    }

    @Override
    public void executeOn(Root root, Date executionTime) {
        for (Wiki wiki : root.getWikiByConsumerUrl().values()) {
            for (Iterator<Page> pages = wiki.getPagesByTitle().values().iterator(); pages.hasNext(); ) {
                Page page = pages.next();
                if (page.getTimestampSegmented() != null
                        && page.getTimestampSegmented().isBefore(earliestAllowedTimestampSegmented)) {

                    boolean pageShouldBeRemoved = true;
                    if (page.getSegments() != null && !page.getSegments().isEmpty()) {
                        for (PageSegment segment : page.getSegments()) {
                            if (segment.getSynthesizedVoices() != null && !segment.getSynthesizedVoices().isEmpty()) {
                                for (PageSegmentVoice voice : segment.getSynthesizedVoices()) {
                                    if (voice.getFailedAttempts() == null || voice.getFailedAttempts().isEmpty()) {
                                        pageShouldBeRemoved = false;
                                        break;
                                    }
                                }
                            }
                            if (!pageShouldBeRemoved) break;
                        }
                    }

                    if (pageShouldBeRemoved)
                        pages.remove();

                }

            }
        }
    }
}

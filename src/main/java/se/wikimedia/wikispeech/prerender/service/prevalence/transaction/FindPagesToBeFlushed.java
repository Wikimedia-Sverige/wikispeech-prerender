package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class FindPagesToBeFlushed implements Query<Root, Map<String, Collection<String>>> {

    private static final long serialVersionUID = 1L;

    private Duration pageMustBeThisOldToBeConsideredForFlushing;
    private Duration flushPageAfterThisMuchTimeSinceSegmentation;

    public FindPagesToBeFlushed() {
    }

    public FindPagesToBeFlushed(
            Duration pageMustBeThisOldToBeConsideredForFlushing,
            Duration flushPageAfterThisMuchTimeSinceSegmentation
    ) {
        this.pageMustBeThisOldToBeConsideredForFlushing = pageMustBeThisOldToBeConsideredForFlushing;
        this.flushPageAfterThisMuchTimeSinceSegmentation = flushPageAfterThisMuchTimeSinceSegmentation;
    }

    @Override
    public Map<String, Collection<String>> query(Root root, Date executionTime) throws Exception {

        LocalDateTime now = Prevalence.toLocalDateTime(executionTime);

        Map<String, Collection<String>> pageTitlesByWikiConsumerUrl = new HashMap<>();

        LocalDateTime ignoreIfPageWasSegmentedAfter = LocalDateTime.now().minus(pageMustBeThisOldToBeConsideredForFlushing);
        LocalDateTime flushIfPageWasSegmentedBefore = LocalDateTime.now().minus(flushPageAfterThisMuchTimeSinceSegmentation);

        for (Wiki wiki : root.getWikiByConsumerUrl().values()) {
            Set<String> pageTitlesToBeFlushed = new HashSet<>();
            for (Page page : wiki.getPagesByTitle().values()) {
                if (page.getTimestampSegmented() == null) {
                    // page has not been segmented
                } else if (page.getTimestampDontFlushUntil() != null && page.getTimestampDontFlushUntil().isAfter(now)) {
                    // page is marked as not being flushed
                } else if (page.getTimestampSegmented().isAfter(ignoreIfPageWasSegmentedAfter)) {
                    // page is too young.
                    // todo: the idea is to avoid looping back to re-segmenting the same page over and over,
                    // todo: especially if it's the main page or linked from main page.
                    // todo: there must be a better way to flush out other pages without touching the above mentioned.
                } else if (page.getTimestampSegmented().isBefore(flushIfPageWasSegmentedBefore)) {
                    // segmentation is too old
                    pageTitlesToBeFlushed.add(page.getTitle());
                } else {
                    // check if all segments have been synthesized for all voices in wiki for the language of the page.
                    boolean allSegmentsHaveBeenSynthesizedForAllVoices = true;
                    for (PageSegment segment : page.getSegments()) {
                        Set<String> voicesExpected = new HashSet<>(wiki.getVoicesPerLanguage().get(page.getLanguageAtSegmentation()));
                        if (segment.getSynthesizedVoices() != null) {
                            for (PageSegmentVoice voice : segment.getSynthesizedVoices()) {
                                if (voice.getTimestampSynthesized() != null) {
                                    voicesExpected.remove(voice.getVoice());
                                }
                            }
                        }
                        if (!voicesExpected.isEmpty()) {
                            allSegmentsHaveBeenSynthesizedForAllVoices = false;
                            break;
                        }
                    }
                    if (allSegmentsHaveBeenSynthesizedForAllVoices) {
                        pageTitlesToBeFlushed.add(page.getTitle());
                    }
                }
            }
            if (!pageTitlesToBeFlushed.isEmpty()) {
                pageTitlesByWikiConsumerUrl.put(wiki.getConsumerUrl(), pageTitlesToBeFlushed);
            }
        }
        return pageTitlesByWikiConsumerUrl;
    }
}

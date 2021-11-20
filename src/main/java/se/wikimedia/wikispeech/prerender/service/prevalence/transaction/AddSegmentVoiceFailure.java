package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;

@Data
public class AddSegmentVoiceFailure implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private byte[] hash;
    private String voice;
    private String stacktrace;

    public AddSegmentVoiceFailure() {
    }

    public AddSegmentVoiceFailure(String consumerUrl, String title, byte[] hash, String voice, String stacktrace) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.hash = hash;
        this.voice = voice;
    }

    @Override
    public void executeOn(Root root, Date date) {
        Page page = root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title);
        for (PageSegment pageSegment : page.getSegments()) {
            if (Arrays.equals(hash, pageSegment.getHash())) {
                PageSegmentVoice pageSegmentVoice = null;
                if (pageSegment.getSynthesizedVoices() != null && !pageSegment.getSynthesizedVoices().isEmpty()) {
                    for (PageSegmentVoice existingPageSegmentVoice : pageSegment.getSynthesizedVoices()) {
                        if (voice.equals(existingPageSegmentVoice.getVoice())) {
                            pageSegmentVoice = existingPageSegmentVoice;
                            break;
                        }
                    }
                }
                if (pageSegmentVoice == null) {
                    pageSegmentVoice = new PageSegmentVoice();
                    pageSegmentVoice.setVoice(voice);
                    pageSegmentVoice.setTimestampSynthesized(null);
                    pageSegmentVoice.setSynthesizedRevision(null);
                    pageSegment.getSynthesizedVoices().add(pageSegmentVoice);
                }
                if (pageSegmentVoice.getFailedAttempts() == null) {
                    pageSegmentVoice.setFailedAttempts(new LinkedHashMap<>());
                }
                pageSegmentVoice.getFailedAttempts().put(Prevalence.toLocalDateTime(date), stacktrace);
            }
        }
        throw new RuntimeException();
    }
}

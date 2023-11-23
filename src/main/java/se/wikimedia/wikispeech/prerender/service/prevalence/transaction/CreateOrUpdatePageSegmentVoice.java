package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;

import java.util.Arrays;
import java.util.Date;

@Data
public class CreateOrUpdatePageSegmentVoice implements TransactionWithQuery<Root, PageSegmentVoice> {

    private static final long serialVersionUID = 1L;


    private String consumerUrl;
    private String title;
    private byte[] hash;
    private long synthesizedRevision;
    private String voice;

    public CreateOrUpdatePageSegmentVoice() {
    }

    public CreateOrUpdatePageSegmentVoice(
            String consumerUrl,
            String title,
            byte[] hash,
            long synthesizedRevision,
            String voice
    ) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.hash = hash;
        this.synthesizedRevision = synthesizedRevision;
        this.voice = voice;
    }

    @Override
    public PageSegmentVoice executeAndQuery(Root root, Date date) throws Exception {
        for (PageSegment pageSegment : root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title).getSegments()) {
            if (Arrays.equals(hash, pageSegment.getHash())) {
                for (PageSegmentVoice pageSegmentVoice : pageSegment.getSynthesizedVoices()) {
                    if (pageSegmentVoice.getVoice().equals(voice)) {
                        pageSegmentVoice.setTimestampSynthesized(Prevalence.toLocalDateTime(date));
                        pageSegmentVoice.setSynthesizedRevision(synthesizedRevision);
                        pageSegmentVoice.setFailedAttempts(null);
                        return pageSegmentVoice;
                    }
                }

                PageSegmentVoice pageSegmentVoice = new PageSegmentVoice();
                pageSegmentVoice.setTimestampSynthesized(Prevalence.toLocalDateTime(date));
                pageSegmentVoice.setSynthesizedRevision(synthesizedRevision);
                pageSegmentVoice.setVoice(root.getInternedVoices().intern(voice));
                pageSegmentVoice.setFailedAttempts(null);
                pageSegment.getSynthesizedVoices().add(pageSegmentVoice);
                return pageSegmentVoice;
            }
        }
        throw new RuntimeException("Segment does not exists.");
    }
}

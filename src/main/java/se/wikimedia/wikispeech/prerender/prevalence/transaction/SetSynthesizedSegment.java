package se.wikimedia.wikispeech.prerender.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.SegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.SynthesizedSegment;

import java.util.Arrays;
import java.util.Date;

@Data
public class SetSynthesizedSegment implements TransactionWithQuery<Root, SynthesizedSegment> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private byte[] hash;
    private String language;
    private String voice;
    private Long revision;


    public SetSynthesizedSegment() {
    }

    public SetSynthesizedSegment(String remoteSiteConsumerUrl, String title, byte[] hash, String language, String voice, Long revision) {
        this.consumerUrl = remoteSiteConsumerUrl;
        this.title = title;
        this.hash = hash;
        this.language = language;
        this.voice = voice;
        this.revision = revision;
    }

    @Override
    public SynthesizedSegment executeAndQuery(Root root, Date executionTime) throws Exception {
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
        for (SynthesizedSegment synthesizedSegment : segmentedPage.getRenderedSynthesizedSegments()) {
            if (Arrays.equals(hash, synthesizedSegment.getHash())
                    && language.equals(synthesizedSegment.getLanguage())
                    && ((voice == null && synthesizedSegment.getVoice() == null) || (voice != null && voice.equals(synthesizedSegment.getVoice())))
            ) {
                synthesizedSegment.setTimestampSynthesized(Prevalence.toLocalDateTime(executionTime));
                synthesizedSegment.setRevision(revision);
                return synthesizedSegment;
            }
        }
        SynthesizedSegment synthesizedSegment = new SynthesizedSegment();
        synthesizedSegment.setHash(hash);
        synthesizedSegment.setLanguage(language);
        synthesizedSegment.setTimestampSynthesized(Prevalence.toLocalDateTime(executionTime));
        synthesizedSegment.setVoice(voice);
        synthesizedSegment.setRevision(revision);
        segmentedPage.getRenderedSynthesizedSegments().add(synthesizedSegment);
        return synthesizedSegment;
    }
}

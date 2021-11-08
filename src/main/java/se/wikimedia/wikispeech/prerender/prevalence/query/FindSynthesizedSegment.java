package se.wikimedia.wikispeech.prerender.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.domain.SegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.SynthesizedSegment;

import java.util.Arrays;
import java.util.Date;

@Data
public class FindSynthesizedSegment implements Query<Root, SynthesizedSegment> {

    private String consumerUrl;
    private String title;
    private byte[] hash;
    private String language;
    private String voice;


    public FindSynthesizedSegment(String remoteSiteConsumerUrl, String pageTitle, byte[] segmentHash, String language, String voice) {
        this.consumerUrl = remoteSiteConsumerUrl;
        this.title = pageTitle;
        this.hash = segmentHash;
        this.language = language;
        this.voice = voice;
    }

    @Override
    public SynthesizedSegment query(Root root, Date date) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(consumerUrl);
        if (remoteSite != null) {
            SegmentedPage segmentedPage = remoteSite.getPagesByTitle().get(title);
            if (segmentedPage != null) {
                for (SynthesizedSegment synthesizedSegment : segmentedPage.getRenderedSynthesizedSegments()) {
                    if (Arrays.equals(hash, synthesizedSegment.getHash())
                            && language.equals(synthesizedSegment.getLanguage())
                            && ((voice == null && synthesizedSegment.getVoice() == null) || (voice != null && voice.equals(synthesizedSegment.getVoice())))
                    ) {
                        return synthesizedSegment;
                    }
                }
            }
        }
        return null;
    }
}

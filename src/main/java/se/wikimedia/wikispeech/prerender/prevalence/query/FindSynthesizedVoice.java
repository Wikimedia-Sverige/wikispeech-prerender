package se.wikimedia.wikispeech.prerender.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SynthesizedSegment;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SynthesizedVoice;

import java.util.Arrays;
import java.util.Date;

@Data
public class FindSynthesizedVoice implements Query<Root, SynthesizedVoice> {

    private String consumerUrl;
    private String title;
    private byte[] hash;
    private String language;
    private String voice;


    public FindSynthesizedVoice(String remoteSiteConsumerUrl, String pageTitle, byte[] segmentHash, String language, String voice) {
        this.consumerUrl = remoteSiteConsumerUrl;
        this.title = pageTitle;
        this.hash = segmentHash;
        this.language = language;
        this.voice = voice;
    }

    @Override
    public SynthesizedVoice query(Root root, Date date) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(consumerUrl);
        if (remoteSite != null) {
            SegmentedPage segmentedPage = remoteSite.getPagesByTitle().get(title);
            if (segmentedPage != null) {
                for (SynthesizedSegment synthesizedSegment : segmentedPage.getRenderedSynthesizedSegments()) {
                    if (Arrays.equals(hash, synthesizedSegment.getHash())
                            && language.equals(synthesizedSegment.getLanguage())
                    ) {
                        for (SynthesizedVoice synthesizedVoice : synthesizedSegment.getSynthesizedVoices()) {
                            if ((voice == null && synthesizedVoice.getVoice() == null) || (voice != null && voice.equals(synthesizedVoice.getVoice()))) {
                                return synthesizedVoice;
                            }
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }
}

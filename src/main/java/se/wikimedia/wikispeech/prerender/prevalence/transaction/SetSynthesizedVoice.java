package se.wikimedia.wikispeech.prerender.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SynthesizedSegment;
import se.wikimedia.wikispeech.prerender.prevalence.domain.state.SynthesizedVoice;

import java.util.Arrays;
import java.util.Date;

@Data
public class SetSynthesizedVoice implements TransactionWithQuery<Root, SynthesizedVoice> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private byte[] hash;
    private String language;
    private String voice;
    private Long revision;


    public SetSynthesizedVoice() {
    }

    public SetSynthesizedVoice(String remoteSiteConsumerUrl, String title, byte[] hash, String language, String voice, Long revision) {
        this.consumerUrl = remoteSiteConsumerUrl;
        this.title = title;
        this.hash = hash;
        this.language = language;
        this.voice = voice;
        this.revision = revision;
    }

    @Override
    public SynthesizedVoice executeAndQuery(Root root, Date executionTime) throws Exception {
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
            ) {
                SynthesizedVoice foundVoice = null;
                for (SynthesizedVoice synthesizedVoice : synthesizedSegment.getSynthesizedVoices()) {
                    if ((voice == null && synthesizedVoice.getVoice() == null) || (voice != null && voice.equals(synthesizedVoice.getVoice()))) {
                        foundVoice = synthesizedVoice;
                        break;
                    }
                }

                if (foundVoice == null) {
                    foundVoice = new SynthesizedVoice();
                    foundVoice.setVoice(voice);
                    synthesizedSegment.getSynthesizedVoices().add(foundVoice);
                }

                foundVoice.setRevision(revision);
                foundVoice.setTimestampSynthesized(Prevalence.toLocalDateTime(executionTime));

                return foundVoice;
            }
        }
        SynthesizedSegment synthesizedSegment = new SynthesizedSegment();
        synthesizedSegment.setHash(hash);
        synthesizedSegment.setLanguage(language);

        SynthesizedVoice foundVoice = new SynthesizedVoice();
        foundVoice.setVoice(voice);
        foundVoice.setRevision(revision);
        foundVoice.setTimestampSynthesized(Prevalence.toLocalDateTime(executionTime));
        synthesizedSegment.getSynthesizedVoices().add(foundVoice);

        segmentedPage.getRenderedSynthesizedSegments().add(synthesizedSegment);
        return foundVoice;
    }
}

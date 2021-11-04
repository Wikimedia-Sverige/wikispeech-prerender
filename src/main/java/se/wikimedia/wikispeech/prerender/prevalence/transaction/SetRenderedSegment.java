package se.wikimedia.wikispeech.prerender.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Page;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Segment;

import java.util.Arrays;
import java.util.Date;

@Data
public class SetRenderedSegment implements TransactionWithQuery<Root, Segment> {

    private static final long serialVersionUID = 1L;

    private String remoteSiteConsumerUrl;
    private String pageTitle;
    private byte[] segmentHash;
    private String language;
    private String voice;


    public SetRenderedSegment() {
    }

    public SetRenderedSegment(String remoteSiteConsumerUrl, String pageTitle, byte[] segmentHash, String language, String voice) {
        this.remoteSiteConsumerUrl = remoteSiteConsumerUrl;
        this.pageTitle = pageTitle;
        this.segmentHash = segmentHash;
        this.language = language;
        this.voice = voice;
    }

    @Override
    public Segment executeAndQuery(Root root, Date executionTime) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(remoteSiteConsumerUrl);
        if (remoteSite == null) {
            remoteSite = new RemoteSite();
            remoteSite.setConsumerUrl(remoteSiteConsumerUrl);
            root.getRemoteSiteByConsumerUrl().put(remoteSiteConsumerUrl, remoteSite);
        }
        Page page = remoteSite.getPagesByTitle().get(pageTitle);
        if (page == null) {
            page = new Page();
            page.setTitle(pageTitle);
            remoteSite.getPagesByTitle().put(pageTitle, page);
        }
        for (Segment segment : page.getRenderedSegments()) {
            if (Arrays.equals(segmentHash, segment.getHash())
                    && language.equals(segment.getLanguage())
                    && ((voice == null && segment.getVoice() == null) || (voice != null && voice.equals(segment.getVoice())))
            ) {
                segment.setTimestampRendered(Prevalence.toLocalDateTime(executionTime));
                return segment;
            }
        }
        Segment segment = new Segment();
        segment.setHash(segmentHash);
        segment.setLanguage(language);
        segment.setTimestampRendered(Prevalence.toLocalDateTime(executionTime));
        segment.setVoice(voice);
        page.getRenderedSegments().add(segment);
        return segment;
    }
}

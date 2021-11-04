package se.wikimedia.wikispeech.prerender.prevalence.query;

import lombok.Data;
import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Page;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RemoteSite;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Segment;

import java.util.Arrays;
import java.util.Date;

@Data
public class FindRenderedSegment implements Query<Root, Segment> {

    private String remoteSiteConsumerUrl;
    private String pageTitle;
    private byte[] segmentHash;
    private String language;
    private String voice;


    public FindRenderedSegment(String remoteSiteConsumerUrl, String pageTitle, byte[] segmentHash, String language, String voice) {
        this.remoteSiteConsumerUrl = remoteSiteConsumerUrl;
        this.pageTitle = pageTitle;
        this.segmentHash = segmentHash;
        this.language = language;
        this.voice = voice;
    }

    @Override
    public Segment query(Root root, Date date) throws Exception {
        RemoteSite remoteSite = root.getRemoteSiteByConsumerUrl().get(remoteSiteConsumerUrl);
        if (remoteSite != null) {
            Page page = remoteSite.getPagesByTitle().get(pageTitle);
            if (page != null) {
                for (Segment segment : page.getRenderedSegments()) {
                    if (Arrays.equals(segmentHash, segment.getHash())
                            && language.equals(segment.getLanguage())
                            && ((voice == null && segment.getVoice() == null) || (voice != null && voice.equals(segment.getVoice())))
                    ) {
                        return segment;
                    }
                }
            }
        }
        return null;
    }
}

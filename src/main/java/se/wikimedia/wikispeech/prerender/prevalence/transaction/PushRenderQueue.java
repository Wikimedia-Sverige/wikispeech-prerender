package se.wikimedia.wikispeech.prerender.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RenderQueueItem;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;

import java.util.Date;

@Data
public class PushRenderQueue implements TransactionWithQuery<Root, RenderQueueItem> {

    private static final long serialVersionUID = 1L;

    private String remoteSiteConsumerUrl;
    private String pageTitle;
    private byte[] segmentHash;
    private long pageRevision;
    private String language;
    private String voice;


    public PushRenderQueue() {
    }

    public PushRenderQueue(String remoteSiteConsumerUrl, String pageTitle, byte[] segmentHash, long pageRevision, String language, String voice) {
        this.remoteSiteConsumerUrl = remoteSiteConsumerUrl;
        this.pageTitle = pageTitle;
        this.segmentHash = segmentHash;
        this.pageRevision = pageRevision;
        this.language = language;
        this.voice = voice;
    }

    @Override
    public RenderQueueItem executeAndQuery(Root root, Date date) throws Exception {
        RenderQueueItem item = new RenderQueueItem();
        item.setRemoteSiteConsumerUrl(remoteSiteConsumerUrl);
        item.setPageTitle(pageTitle);
        item.setSegmentHash(segmentHash);
        item.setPageRevision(pageRevision);
        item.setLanguage(language);
        item.setVoice(voice);
        item.setQueued(Prevalence.toLocalDateTime(date));
        root.getRenderQueue().add(item);
        return item;
    }
}

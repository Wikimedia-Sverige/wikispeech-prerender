package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;

import java.util.Date;
import java.util.Set;

@Data
public class FinalizedPageSegmented implements TransactionWithQuery<Root, Page> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private String language;
    private long revisionAtSegmentation;
    private Set<byte[]> deletedSegmentHashes;

    public FinalizedPageSegmented() {
    }

    public FinalizedPageSegmented(String consumerUrl, String title, String language, long revisionAtSegmentation, Set<byte[]> deletedSegmentHashes) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.language = language;
        this.revisionAtSegmentation = revisionAtSegmentation;
        this.deletedSegmentHashes = deletedSegmentHashes;
    }

    @Override
    public Page executeAndQuery(Root root, Date date) throws Exception {
        Page page = root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title);
        page.setTimestampSegmented(Prevalence.toLocalDateTime(date));
        page.setRevisionAtSegmentation(revisionAtSegmentation);
        page.setLanguageAtSegmentation(root.getInternedLanguages().intern(language));
        if (deletedSegmentHashes != null && !deletedSegmentHashes.isEmpty()) {
            page.getSegments().removeIf(segment -> deletedSegmentHashes.contains(segment.getHash()));
        }
        return page;
    }
}

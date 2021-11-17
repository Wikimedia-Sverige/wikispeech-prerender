package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.util.Date;

@Data
public class CreateSegmentedPage implements TransactionWithQuery<Root, Page> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private String languageAtSegmentation;
    private long revisionAtSegmentation;

    public CreateSegmentedPage() {
    }

    public CreateSegmentedPage(String consumerUrl, String title, String languageAtSegmentation, long revisionAtSegmentation) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.languageAtSegmentation = languageAtSegmentation;
        this.revisionAtSegmentation = revisionAtSegmentation;
    }

    @Override
    public Page executeAndQuery(Root root, Date date) throws Exception {
        Wiki wiki = root.getWikiByConsumerUrl().get(consumerUrl);
        if (wiki.getPagesByTitle().containsKey(title)) {
            throw new RuntimeException();
        }
        Page page = new Page();
        page.setTitle(title);
        page.setRevisionAtSegmentation(revisionAtSegmentation);
        page.setLanguageAtSegmentation(root.getInternedLanguages().intern(languageAtSegmentation));
        page.setTimestampSegmented(Prevalence.toLocalDateTime(date));
        wiki.getPagesByTitle().put(title, page);
        return page;
    }
}

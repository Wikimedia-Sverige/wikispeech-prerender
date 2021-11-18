package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;

import java.util.Date;

@Data
public class SetPagePriority implements Transaction<Root> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private float priority;

    public SetPagePriority() {
    }

    public SetPagePriority(String consumerUrl, String title, float priority) {
        this.consumerUrl = consumerUrl;
        this.title = title;
        this.priority = priority;
    }

    @Override
    public void executeOn(Root root, Date date) {
        root.getWikiByConsumerUrl().get(consumerUrl).getPagesByTitle().get(title).setPriority(priority);
    }
}

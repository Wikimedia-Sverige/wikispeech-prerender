package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation;

import java.time.LocalDateTime;
import java.util.Collection;

@Data
public class PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation extends PushToCommandQueue<ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private Collection<String> voices;

    private String linksExpression = "//*[@id='bodyContent']//A[starts-with(@href, '/wiki/')]";
    private String allowedHrefPattern = "/wiki/[^:]+";

    @Override
    public ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation commandFactory(LocalDateTime executionTime) {
        ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation command = new ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation();
        command.setCreated(executionTime);
        command.setConsumerUrl(this.consumerUrl);
        command.setTitle(this.title);
        command.setLinksExpression(this.linksExpression);
        command.setAllowedHrefPattern(this.allowedHrefPattern);
        command.setLanguage(this.language);
        command.setVoices(this.voices);
        return command;
    }

    public static PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation factory(
             String consumerUrl,
             String title,

             String language,
             Collection<String> voices,

             String linksExpression,
             String allowedHrefPattern
    ) {
        PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation push = new PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation();
        push.setConsumerUrl(consumerUrl);
        push.setTitle(title);
        push.setLanguage(language);
        push.setVoices(voices);
        push.setLinksExpression(linksExpression);
        push.setAllowedHrefPattern(allowedHrefPattern);
        return push;
    }
}

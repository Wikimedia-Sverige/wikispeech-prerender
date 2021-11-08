package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation;

import java.time.LocalDateTime;

@Data
public class PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation extends PushToCommandQueue<ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private String voice;

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
        command.setVoice(this.voice);
        return command;
    }

    public static PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation factory(
             String consumerUrl,
             String title,

             String language,
             String voice,

             String linksExpression,
             String allowedHrefPattern
    ) {
        PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation push = new PushScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation();
        push.setConsumerUrl(consumerUrl);
        push.setTitle(title);
        push.setLanguage(language);
        push.setVoice(voice);
        push.setLinksExpression(linksExpression);
        push.setAllowedHrefPattern(allowedHrefPattern);
        return push;
    }
}

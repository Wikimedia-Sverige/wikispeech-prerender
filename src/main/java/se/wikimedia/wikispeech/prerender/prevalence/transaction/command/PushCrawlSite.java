package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.CrawlSite;

import java.time.LocalDateTime;
import java.util.Collection;

@Data
public class PushCrawlSite extends PushToCommandQueue<CrawlSite> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String startingPointTitle;
    private Integer maximumDepth;

    private String language;
    private Collection<String> voices;

    private String linksExpression;
    private String allowedHrefPattern;


    @Override
    public CrawlSite commandFactory(LocalDateTime executionTime) {
        CrawlSite command = new CrawlSite();
        command.setCreated(executionTime);
        command.setConsumerUrl(this.consumerUrl);
        command.setStartingPointTitle(this.startingPointTitle);
        command.setMaximumDepth(this.maximumDepth);
        command.setLanguage(language);
        command.setVoices(voices);
        command.setLinksExpression(this.linksExpression);
        command.setAllowedHrefPattern(this.allowedHrefPattern);
        return command;
    }

    public static PushCrawlSite factory(String consumerUrl, String startingPointTitle, int maximumDepth, String language, Collection<String> voices, String linksExpression, String allowedHrefPattern) {
        PushCrawlSite push = new PushCrawlSite();
        push.setConsumerUrl(consumerUrl);
        push.setStartingPointTitle(startingPointTitle);
        push.setLanguage(language);
        push.setVoices(voices);
        push.setMaximumDepth(maximumDepth);
        push.setLinksExpression(linksExpression);
        push.setAllowedHrefPattern(allowedHrefPattern);
        return push;
    }
}

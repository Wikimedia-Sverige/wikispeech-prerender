package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.PollRecentChanges;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;

@Data
public class PushPollRecentChanges extends PushToCommandQueue<PollRecentChanges> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private ZonedDateTime startTimestamp;

    private String language;
    private Collection<String> voices;

    private String linksExpression;
    private String allowedHrefPattern;


    @Override
    public PollRecentChanges commandFactory(LocalDateTime executionTime) {
        PollRecentChanges command = new PollRecentChanges();
        command.setCreated(executionTime);
        command.setConsumerUrl(this.consumerUrl);
        command.setStartTimestamp(this.startTimestamp);
        command.setLanguage(language);
        command.setVoices(voices);
        command.setLinksExpression(this.linksExpression);
        command.setAllowedHrefPattern(this.allowedHrefPattern);
        return command;
    }

    public static PushPollRecentChanges factory(String consumerUrl, ZonedDateTime startTimestamp, String language, Collection<String> voices, String linksExpression, String allowedHrefPattern) {
        PushPollRecentChanges push = new PushPollRecentChanges();
        push.setConsumerUrl(consumerUrl);
        push.setStartTimestamp(startTimestamp);
        push.setLanguage(language);
        push.setVoices(voices);
        push.setLinksExpression(linksExpression);
        push.setAllowedHrefPattern(allowedHrefPattern);
        return push;
    }
}

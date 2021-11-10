package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.SegmentPageAndQueueForSynthesis;

import java.time.LocalDateTime;
import java.util.Collection;

@Data
public class PushSegmentPageAndQueueForSynthesis extends PushToCommandQueue<SegmentPageAndQueueForSynthesis> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private Collection<String> voices;


    @Override
    public SegmentPageAndQueueForSynthesis commandFactory(LocalDateTime executionTime) {
        SegmentPageAndQueueForSynthesis command = new SegmentPageAndQueueForSynthesis();
        command.setCreated(executionTime);
        command.setConsumerUrl(this.consumerUrl);
        command.setTitle(this.title);
        command.setLanguage(this.language);
        command.setVoices(this.voices);
        return command;
    }

    public static PushSegmentPageAndQueueForSynthesis factory(String consumerUrl, String title, String language, Collection<String> voices) {
        PushSegmentPageAndQueueForSynthesis push = new PushSegmentPageAndQueueForSynthesis();
        push.setConsumerUrl(consumerUrl);
        push.setTitle(title);
        push.setLanguage(language);
        push.setVoices(voices);
        return push;
    }
}

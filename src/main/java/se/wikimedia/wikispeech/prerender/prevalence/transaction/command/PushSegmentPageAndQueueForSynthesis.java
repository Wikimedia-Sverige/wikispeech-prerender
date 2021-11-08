package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.SegmentPageAndQueueForSynthesis;

import java.time.LocalDateTime;

@Data
public class PushSegmentPageAndQueueForSynthesis extends PushToCommandQueue<SegmentPageAndQueueForSynthesis> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private String voice;


    @Override
    public SegmentPageAndQueueForSynthesis commandFactory(LocalDateTime executionTime) {
        SegmentPageAndQueueForSynthesis command = new SegmentPageAndQueueForSynthesis();
        command.setCreated(executionTime);
        command.setConsumerUrl(this.consumerUrl);
        command.setTitle(this.title);
        command.setLanguage(this.language);
        command.setVoice(this.voice);
        return command;
    }

    public static PushSegmentPageAndQueueForSynthesis factory(String consumerUrl, String title, String language, String voice) {
        PushSegmentPageAndQueueForSynthesis push = new PushSegmentPageAndQueueForSynthesis();
        push.setConsumerUrl(consumerUrl);
        push.setTitle(title);
        push.setLanguage(language);
        push.setVoice(voice);
        return push;
    }
}

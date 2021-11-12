package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.SynthesizeSegment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PushSynthesizeSegmentToCommandQueue implements TransactionWithQuery<Root, SynthesizeSegment> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private String voice;

    private Long currentRevisionAtSegmentation;

    private byte[] hash;
    private int contentStartOffset;
    private int contentEndOffset;
    private List<String> contentXPathExpressions;
    private List<String> contentTexts;

    private List<LocalDateTime> failedAttempts;

    @Override
    public SynthesizeSegment executeAndQuery(Root root, Date date) throws Exception {
        SynthesizeSegment command = commandFactory(Prevalence.toLocalDateTime(date));
        root.getSynthesizeSegmentsQueue().add(command);
        return command;
    }

    public SynthesizeSegment commandFactory(LocalDateTime executionTime) {
        SynthesizeSegment command = new SynthesizeSegment();
        command.setCreated(executionTime);
        command.setConsumerUrl(this.consumerUrl);
        command.setTitle(this.title);
        command.setCurrentRevisionAtSegmentation(this.currentRevisionAtSegmentation);
        command.setHash(this.hash);
        command.setContentStartOffset(this.contentStartOffset);
        command.setContentEndOffset(this.contentEndOffset);
        command.setContentXPathExpressions(this.contentXPathExpressions);
        command.setContentTexts(this.contentTexts);
        command.setLanguage(this.language);
        command.setVoice(this.voice);
        command.setFailedAttempts(this.failedAttempts);
        return command;
    }

    public static PushSynthesizeSegmentToCommandQueue requeueFactory(SynthesizeSegment command) {
        PushSynthesizeSegmentToCommandQueue push = new PushSynthesizeSegmentToCommandQueue();
        push.setConsumerUrl(command.getConsumerUrl());
        push.setTitle(command.getTitle());
        push.setCurrentRevisionAtSegmentation(command.getCurrentRevisionAtSegmentation());
        push.setHash(command.getHash());
        push.setContentStartOffset(command.getContentStartOffset());
        push.setContentEndOffset(command.getContentEndOffset());
        push.setContentXPathExpressions(command.getContentXPathExpressions());
        push.setContentTexts(command.getContentTexts());
        push.setLanguage(command.getLanguage());
        push.setVoice(command.getVoice());
        if (command.getFailedAttempts() == null) {
            push.setFailedAttempts(new ArrayList<>());
        } else {
            push.setFailedAttempts(new ArrayList<>(command.getFailedAttempts()));
        }
        push.getFailedAttempts().add(LocalDateTime.now());

        return push;
    }
}

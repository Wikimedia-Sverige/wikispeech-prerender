package se.wikimedia.wikispeech.prerender.prevalence.domain.command;

import lombok.Data;

@Data
public class SegmentPageAndQueueForSynthesis extends Command {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private String voice;

    @Override
    public <R> R accept(CommandVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

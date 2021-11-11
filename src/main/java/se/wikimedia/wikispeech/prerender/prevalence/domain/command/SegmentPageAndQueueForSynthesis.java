package se.wikimedia.wikispeech.prerender.prevalence.domain.command;

import lombok.Data;

import java.util.Collection;

@Data
public class SegmentPageAndQueueForSynthesis extends Command {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private Collection<String> voices;

    /** If not null, only the first n segments of the page will be synthesized */
    private Integer nFirstSegmentsLimit;

    @Override
    public <R> R accept(CommandVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

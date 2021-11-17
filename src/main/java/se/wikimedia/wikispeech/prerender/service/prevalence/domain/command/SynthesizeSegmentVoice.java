package se.wikimedia.wikispeech.prerender.service.prevalence.domain.command;

import lombok.Data;

import java.util.List;

@Data
public class SynthesizeSegmentVoice extends Command {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;
    private byte[] hash;
    private String voice;

    private int contentStartOffset;
    private int contentEndOffset;
    private List<String> contentXPathExpressions;
    private List<String> contentTexts;

    @Override
    public <R> R accept(CommandVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

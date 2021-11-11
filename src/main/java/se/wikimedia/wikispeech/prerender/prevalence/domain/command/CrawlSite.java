package se.wikimedia.wikispeech.prerender.prevalence.domain.command;

import lombok.Data;

import java.util.Collection;

@Data
public class CrawlSite extends Command {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String startingPointTitle;
    private int maximumDepth;

    private String language;
    private Collection<String> voices;

    /** If not null, only the first n segments of the page will be synthesized */
    private Integer nFirstSegmentsLimit;

    private String linksExpression = "//*[@id='bodyContent']//A[starts-with(@href, '/wiki/')]";
    private String allowedHrefPattern = "/wiki/[^:]+";


    @Override
    public <R> R accept(CommandVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

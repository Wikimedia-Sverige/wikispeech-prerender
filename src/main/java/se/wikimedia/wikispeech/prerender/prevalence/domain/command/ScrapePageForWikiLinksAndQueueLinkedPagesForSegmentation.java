package se.wikimedia.wikispeech.prerender.prevalence.domain.command;

import lombok.Data;

@Data
public class ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation extends Command {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String title;

    private String language;
    private String voice;

    private String linksExpression = "//*[@id='bodyContent']//A[starts-with(@href, '/wiki/')]";
    private String allowedHrefPattern = "/wiki/[^:]+";

    @Override
    public <R> R accept(CommandVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

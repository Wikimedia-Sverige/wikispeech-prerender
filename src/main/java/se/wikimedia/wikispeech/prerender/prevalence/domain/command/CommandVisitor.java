package se.wikimedia.wikispeech.prerender.prevalence.domain.command;

public interface CommandVisitor<R> {

    public R visit(SegmentPageAndQueueForSynthesis command);
    public R visit(SynthesizeSegment command);
    public R visit(ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation command);

}

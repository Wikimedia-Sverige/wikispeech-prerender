package se.wikimedia.wikispeech.prerender.service.prevalence.domain.command;

public interface CommandVisitor<R> {

    public R visit(SegmentPage command);
    public R visit(SynthesizeSegmentVoice command);
    public R visit(ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation command);
    public R visit(CrawlSite command);
    public R visit(PollRecentChanges command);

}

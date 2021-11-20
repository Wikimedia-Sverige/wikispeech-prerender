package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

@Data
public class PageSegmentVoiceReference {
    private Wiki wiki;
    private Page page;
    private PageSegment pageSegment;
    private PageSegmentVoice pageSegmentVoice;

    public PageSegmentVoiceReference(Wiki wiki, Page page, PageSegment pageSegment, PageSegmentVoice pageSegmentVoice) {
        this.wiki = wiki;
        this.page = page;
        this.pageSegment = pageSegment;
        this.pageSegmentVoice = pageSegmentVoice;
    }
}

package se.wikimedia.wikispeech.prerender.service.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Page implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private List<PageSegment> segments = new ArrayList<>();

    private float priority = 1F;

    /** null means never segmented */
    private LocalDateTime timestampSegmented;
    private Long revisionAtSegmentation;
    private String languageAtSegmentation;

    /**
     * Longevity of this page.
     * E.g. main page should never be flushed out,
     * pages linked from main page lives for quite some time (they are often large and have a lot of segments we don't want to rerender if we can avoid it)
     * while most pages use system fallback (setting this value to null)
     */
    private LocalDateTime timestampDontFlushUntil;


    @Override
    public String toString() {
        return "Page{" +
                "title='" + title + '\'' +
                ", priority=" + priority +
                ", timestampSegmented=" + timestampSegmented +
                ", revisionAtSegmentation=" + revisionAtSegmentation +
                ", languageAtSegmentation='" + languageAtSegmentation + '\'' +
                '}';
    }
}

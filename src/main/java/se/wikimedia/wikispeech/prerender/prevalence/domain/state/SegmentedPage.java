package se.wikimedia.wikispeech.prerender.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SegmentedPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private List<SynthesizedSegment> renderedSynthesizedSegments = new ArrayList<>();

    private LocalDateTime lastSegmented;
    private Long lastSegmentedRevision;

}
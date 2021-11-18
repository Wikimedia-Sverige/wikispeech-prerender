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

}

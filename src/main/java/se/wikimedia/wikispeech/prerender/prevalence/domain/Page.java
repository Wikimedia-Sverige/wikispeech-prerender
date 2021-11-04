package se.wikimedia.wikispeech.prerender.prevalence.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Page implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private List<Segment> renderedSegments = new ArrayList<>();

}

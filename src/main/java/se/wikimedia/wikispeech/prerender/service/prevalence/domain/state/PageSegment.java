package se.wikimedia.wikispeech.prerender.service.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class PageSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] hash;
    /** The same segment can occur multiple times in a page. This is the lowest index it was found. */
    private int lowestIndexAtSegmentation;
    private List<PageSegmentVoice> synthesizedVoices = new ArrayList<>();

}

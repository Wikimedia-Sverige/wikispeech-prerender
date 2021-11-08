package se.wikimedia.wikispeech.prerender.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SynthesizedSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] hash;
    private String language;
    private List<SynthesizedVoice> synthesizedVoices = new ArrayList<>();

}

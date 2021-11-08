package se.wikimedia.wikispeech.prerender.prevalence.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SynthesizedSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime timestampSynthesized;

    private byte[] hash;
    private String language;
    private String voice;
    private Long revision;

    // todo revision, voice and language should be in a SynthesizedVoice, a List<> in this class..


}

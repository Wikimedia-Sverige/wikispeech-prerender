package se.wikimedia.wikispeech.prerender.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SynthesizedVoice implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime timestampSynthesized;

    private String voice;
    private Long revision;

}

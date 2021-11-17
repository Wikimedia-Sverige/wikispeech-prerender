package se.wikimedia.wikispeech.prerender.service.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

@Data
public class PageSegmentVoice implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voice;
    private LinkedHashMap<LocalDateTime, String> failedAttempts;
    private LocalDateTime timestampSynthesized;
    private Long synthesizedRevision;

}

package se.wikimedia.wikispeech.prerender.prevalence.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Segment implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime timestampRendered;

    private byte[] hash;
    private String language;
    private String voice;

}

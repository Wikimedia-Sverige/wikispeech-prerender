package se.wikimedia.wikispeech.prerender.prevalence.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RenderQueueItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String remoteSiteConsumerUrl;
    private String pageTitle;
    private byte[] segmentHash;
    private long pageRevision;
    private String language;
    private String voice;

    private LocalDateTime queued;

}

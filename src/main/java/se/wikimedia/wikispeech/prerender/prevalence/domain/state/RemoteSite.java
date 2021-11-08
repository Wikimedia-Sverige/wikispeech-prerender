package se.wikimedia.wikispeech.prerender.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class RemoteSite implements Serializable {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private Map<String, SegmentedPage> pagesByTitle = new HashMap<>();

}

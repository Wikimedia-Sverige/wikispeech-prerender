package se.wikimedia.wikispeech.prerender.service.prevalence.domain.state;

import lombok.Data;

import java.io.Serializable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Data
public class Wiki implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String consumerUrl;

    private Page mainPage;
    private Map<String, Page> pagesByTitle = new HashMap<>();

    private Duration maximumSynthesizedVoiceAge = Duration.ofDays(30);

    private String defaultLanguage = null;
    private Map<String, Set<String>> voicesPerLanguage = new HashMap<>();

    private OffsetDateTime timestampOfLastRecentChangesItemProcessed;
    private List<Integer> pollRecentChangesNamespaces = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerUrl);
    }

    @Override
    public String toString() {
        return "Wiki{" +
                "name='" + name + '\'' +
                ", consumerUrl='" + consumerUrl + '\'' +
                ", maximumSynthesizedVoiceAge=" + maximumSynthesizedVoiceAge +
                ", defaultLanguage='" + defaultLanguage + '\'' +
                ", voicesPerLanguage=" + voicesPerLanguage +
                ", timestampOfLastRecentChangesItemProcessed=" + timestampOfLastRecentChangesItemProcessed +
                ", pollRecentChangesNamespaces=" + pollRecentChangesNamespaces +
                '}';
    }
}

package se.wikimedia.wikispeech.prerender.service.prevalence.domain;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class Root implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Wiki> wikiByConsumerUrl = new HashMap<>();

    private Intern<String> internedVoices = new Intern<>();
    private Intern<String> internedLanguages = new Intern<>();

    @Override
    public String toString() {
        return "Root{" +
                "internedVoices=" + internedVoices +
                ", internedLanguages=" + internedLanguages +
                '}';
    }
}

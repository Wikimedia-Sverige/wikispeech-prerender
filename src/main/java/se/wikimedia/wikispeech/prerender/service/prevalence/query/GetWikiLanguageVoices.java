package se.wikimedia.wikispeech.prerender.service.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GetWikiLanguageVoices implements Query<Root, Set<String>> {

    private String consumerUrl;
    private String language;

    public GetWikiLanguageVoices(String consumerUrl, String language) {
        this.consumerUrl = consumerUrl;
        this.language = language;
    }

    @Override
    public Set<String> query(Root root, Date date) throws Exception {
        Set<String> set = root.getWikiByConsumerUrl().get(consumerUrl).getVoicesPerLanguage().get(language);
        if (set == null || set.isEmpty()) {
            set = new HashSet<>();
            set.add(null);
        } else {
            set = new HashSet<>(set);
        }
        return set;
    }
}

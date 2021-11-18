package se.wikimedia.wikispeech.prerender.service.prevalence.transaction;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Data
public class CreateWiki implements TransactionWithQuery<Root, Wiki> {

    private static final long serialVersionUID = 1L;

    private String consumerUrl;
    private String name;
    private Duration maximumSynthesizedVoiceAge;

    private String defaultLanguage;
    private Map<String, Set<String>> voicesPerLanguage = new HashMap<>();

    private List<Integer> pollRecentChangesNamespaces = new ArrayList<>();
    private OffsetDateTime timestampOfLastRecentChangesItemProcessed;

    public CreateWiki() {
    }

    @Override
    public Wiki executeAndQuery(Root root, Date date) throws Exception {
        Wiki wiki = new Wiki();
        wiki.setConsumerUrl(consumerUrl);
        wiki.setName(name);
        wiki.setMaximumSynthesizedVoiceAge(maximumSynthesizedVoiceAge);
        wiki.setDefaultLanguage(defaultLanguage);

        if (voicesPerLanguage != null) {
            wiki.setVoicesPerLanguage(new HashMap<>(voicesPerLanguage.size()));
            for (Map.Entry<String, Set<String>> entry : voicesPerLanguage.entrySet()) {
                Set<String> voices = new HashSet<>(entry.getValue().size());
                wiki.getVoicesPerLanguage().put(root.getInternedLanguages().intern(entry.getKey()), voices);
                for (String voice : entry.getValue()) {
                    voices.add(root.getInternedVoices().intern(voice));
                }
            }
        }

        wiki.setPollRecentChangesNamespaces(pollRecentChangesNamespaces);
        if (timestampOfLastRecentChangesItemProcessed != null) {
            wiki.setTimestampOfLastRecentChangesItemProcessed(timestampOfLastRecentChangesItemProcessed);
        } else {
            wiki.setTimestampOfLastRecentChangesItemProcessed(Prevalence.toLocalDateTime(date).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC).minusDays(1));
        }
        root.getWikiByConsumerUrl().put(consumerUrl, wiki);
        return wiki;
    }

}

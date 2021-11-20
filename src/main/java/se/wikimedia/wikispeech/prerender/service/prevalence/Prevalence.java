package se.wikimedia.wikispeech.prerender.service.prevalence;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.*;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.service.AbstractLifecycle;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateWiki;
import se.wikimedia.wikispeech.prerender.site.SwedishWikipedia;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class Prevalence extends AbstractLifecycle implements SmartLifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    @Setter
    private Prevayler<Root> prevalyer;

    @Override
    public void doStart() {
        File prevalenceBase = new File("./prevalence");
        try {
            prevalyer = PrevaylerFactory.createPrevayler(new Root(), prevalenceBase.getAbsolutePath());

            if (execute(new Query<Root, Boolean>() {
                @Override
                public Boolean query(Root root, Date date) throws Exception {
                    return root.getWikiByConsumerUrl().isEmpty();
                }
            })) {
                initializePrevalence();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void doStop() {
        try {
            prevalyer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(Transaction<Root> transaction) {
//        if (log.isDebugEnabled()) {
//            log.debug("Executing transaction {}", transaction);
//        }
        prevalyer.execute(transaction);
    }

    public <R> R execute(TransactionWithQuery<Root, R> transaction) throws Exception {
//        if (log.isDebugEnabled()) {
//            log.debug("Executing transaction {}", transaction);
//        }
        return prevalyer.execute(transaction);
    }

    public <R> R execute(Query<Root, R> query) throws Exception {
//        if (log.isTraceEnabled()) {
//            log.trace("Executing query {}", query);
//        }
        return prevalyer.execute(query);
    }

    public static LocalDateTime toLocalDateTime(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private void initializePrevalence() throws Exception {
//        CreateWiki createWiki = new CreateWiki(
//                SwedishWikipedia.CONSUMER_URL_SV_WIKIPEDIA,
//                "Svenska Wikipedia",
//                Duration.ofDays(30),
//                "sv"
//        );
//        createWiki.addLanguageVoice("sv", "stts_sv_nst-hsmm");
//        createWiki.getPollRecentChangesNamespaces().add(0);
//        createWiki.getPollRecentChangesNamespaces().add(100);
//        execute(createWiki);
    }


}

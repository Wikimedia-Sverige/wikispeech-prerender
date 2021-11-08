package se.wikimedia.wikispeech.prerender.prevalence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;
import org.prevayler.Query;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class Prevalence {

    private static Prevalence instance = new Prevalence();

    public static Prevalence getInstance() {
        return instance;
    }

    private Prevalence() {
    }

    private Logger log = LogManager.getLogger();

    private Prevayler<Root> prevalyer;

    public void open() throws Exception {
        log.info("Setting up prevalence...");
        File prevalenceBase = new File("./prevalence");
        prevalyer = PrevaylerFactory.createPrevayler(new Root(), prevalenceBase.getAbsolutePath());
        log.info("Prevalence as been started.");
    }

    public void close() throws IOException {
        prevalyer.close();
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


}

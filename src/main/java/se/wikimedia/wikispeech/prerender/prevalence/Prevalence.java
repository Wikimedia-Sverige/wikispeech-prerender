package se.wikimedia.wikispeech.prerender.prevalence;

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

    private Prevayler<Root> prevalyer;

    public void open() throws Exception {
        File prevalenceBase = new File("./prevalence");
        prevalyer = PrevaylerFactory.createPrevayler(new Root(), prevalenceBase.getAbsolutePath());
    }

    public void close() throws IOException {
        prevalyer.close();
    }

    public <R> R execute(TransactionWithQuery<Root, R> transaction) throws Exception {
        return prevalyer.execute(transaction);
    }

    public <R> R execute(Query<Root, R> query) throws Exception {
        return prevalyer.execute(query);
    }


    public static LocalDateTime toLocalDateTime(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }


}

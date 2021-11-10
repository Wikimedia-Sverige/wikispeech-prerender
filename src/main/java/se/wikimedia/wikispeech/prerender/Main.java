package se.wikimedia.wikispeech.prerender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.query.command.PeekCommandQueue;
import se.wikimedia.wikispeech.prerender.prevalence.query.command.PeekSynthesizeSegmentCommandQueue;
import se.wikimedia.wikispeech.prerender.site.EnglishWikipedia;
import se.wikimedia.wikispeech.prerender.site.RecentChangesPoller;
import se.wikimedia.wikispeech.prerender.site.RemoteSite;
import se.wikimedia.wikispeech.prerender.site.SwedishWikipedia;

import java.time.Duration;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    private Logger log = LogManager.getLogger();

    public void run() throws Exception {
        CommandQueue.getInstance().setNumberOfWorkerThreads(10);
        CommandQueue.getInstance().setNumberOfSynthesizeWorkerThreads(2);
        CommandQueue.getInstance().setMaximumSynthesizedVoiceAge(Duration.ofDays(30));

        RemoteSite[] remoteSites = new RemoteSite[]{
                new SwedishWikipedia(),
//                new EnglishWikipedia(),
        };

        Prevalence.getInstance().open();
        CommandQueue.getInstance().start();

        for (RemoteSite remoteSite : remoteSites) {
            remoteSite.start();
        }

        try {
            if (Prevalence.getInstance().execute(new PeekCommandQueue()) == null
                    && Prevalence.getInstance().execute(new PeekSynthesizeSegmentCommandQueue()) == null) {
            }

            while (true) {
                while (Prevalence.getInstance().execute(new PeekCommandQueue()) != null
                        || Prevalence.getInstance().execute(new PeekSynthesizeSegmentCommandQueue()) != null) {
                    Thread.sleep(Duration.ofMinutes(1).toMillis());
                }
                log.info("Queue is empty.");
                Thread.sleep(Duration.ofMinutes(1).toMillis());
            }

        } finally {
            CommandQueue.getInstance().stop();

            for (RemoteSite remoteSite : remoteSites) {
                remoteSite.stop();
            }

            Prevalence.getInstance().close();
        }
    }


}

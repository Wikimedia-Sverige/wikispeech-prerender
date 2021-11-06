package se.wikimedia.wikispeech.prerender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.query.PeekRenderQueue;
import se.wikimedia.wikispeech.prerender.site.EnglishWikipedia;
import se.wikimedia.wikispeech.prerender.site.RemoteSite;
import se.wikimedia.wikispeech.prerender.site.SwedishWikipedia;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    private Logger log = LogManager.getLogger();

    public void run() throws Exception {
        RenderQueue.getInstance().setNumberOfWorkerThreads(1);

        RemoteSite[] remoteSites = new RemoteSite[]{
                new SwedishWikipedia(),
                new EnglishWikipedia(),
        };

        Prevalence.getInstance().open();
        RenderQueue.getInstance().start();

        try {
            while (true) {
                while (Prevalence.getInstance().execute(new PeekRenderQueue()) != null) {
                    Thread.sleep(1000);
                }
                log.info("Queue is empty.");
                // todo consider waiting for a while before repopulating.
                Thread.sleep(1);
                populate(remoteSites);
            }
        } finally {
            RenderQueue.getInstance().stop();
            Prevalence.getInstance().close();
        }
    }

    private void populate(RemoteSite[] remoteSites) {
        for (RemoteSite remoteSite : remoteSites) {
            log.info("Collecting titles from {}...", remoteSite.getName());
            remoteSite.collectTitles((title, previousMustHaveBeenRenderedBefore) -> {
                for (String voice : remoteSite.getVoices()) {
                    try {
                        RenderQueue.getInstance().queuePage(remoteSite.getConsumerUrl(), title, remoteSite.getLanguage(), voice, previousMustHaveBeenRenderedBefore);
                    } catch (Exception e) {
                        log.error("Failed to queue title {} from {}", title, remoteSite.getName());
                    }
                }
            });
        }
        log.info("Populated data from all remote sites.");
    }

}

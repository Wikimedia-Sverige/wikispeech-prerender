package se.wikimedia.wikispeech.prerender.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.Lifecycle;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLifecycle implements Lifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    private final AtomicBoolean running = new AtomicBoolean(false);

    protected abstract void doStart();
    protected abstract void doStop();

    @Override
    public final void start() {
        if (running.compareAndSet(false, true)) {
            log.info("Starting up...");
            doStart();
            log.info("Started.");
        } else {
            throw new RuntimeException("Already running!");
        }
    }

    @Override
    public final void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                log.info("Stopping...");
                doStop();
                log.info("Stopped.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Not running!");
        }
    }

    @Override
    public final boolean isRunning() {
        return running.get();
    }

}

package se.wikimedia.wikispeech.prerender.service;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;

public abstract class ExecutorService extends AbstractLifecycle {

    private final Logger log = LogManager.getLogger(getClass());

    @Setter
    private int numberOfWorkerThreads = 1;

    private CountDownLatch stoppedLatch;

    protected abstract void execute();

    @Override
    public void doStart() {
        stoppedLatch = new CountDownLatch(numberOfWorkerThreads);
        for (int i = 0; i < numberOfWorkerThreads; i++) {
            new Thread(() -> {
                try {
                    while (isRunning()) {
                        try {
                            ExecutorService.this.execute();
                        } catch (Exception e) {
                            log.fatal("Fatal exception, thread stops!", e);
                            break;
                        }
                    }
                } finally {
                    log.info("Thread stops.");
                    stoppedLatch.countDown();
                }
            }).start();
        }
    }

    @Override
    public void doStop() {
        try {
            stoppedLatch.await();
            stoppedLatch = null;
        } catch (InterruptedException ie) {
            log.error("Interrupted while awaiting threads to close down.", ie);
        }
    }

}

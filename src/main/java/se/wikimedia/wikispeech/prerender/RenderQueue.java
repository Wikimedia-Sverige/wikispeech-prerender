package se.wikimedia.wikispeech.prerender;

import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RenderQueueItem;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Segment;
import se.wikimedia.wikispeech.prerender.prevalence.query.FindRenderedSegment;
import se.wikimedia.wikispeech.prerender.prevalence.query.PeekRenderQueue;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.PollRenderQueue;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.PushRenderQueue;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.SetRenderedSegment;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenderQueue {

    private static RenderQueue instance = new RenderQueue();

    public static RenderQueue getInstance() {
        return instance;
    }

    private RenderQueue() {

    }

    private Logger log = LogManager.getLogger();

    private WikispeechApi wikispeech;

    @Setter
    private int numberOfWorkerThreads = 1;
    private AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch stoppedLatch;

    public void start() {
        if (running.get()) {
            throw new RuntimeException("Already running!");
        }
        running.set(true);

        wikispeech = new WikispeechApi();
        wikispeech.open();

        CountDownLatch stoppedLatch = new CountDownLatch(numberOfWorkerThreads);
        for (int i = 0; i < numberOfWorkerThreads; i++) {
            new Thread(() -> {
                try {
                    while (running.get()) {
                        try {
                            RenderQueueItem item = Prevalence.getInstance().execute(new PeekRenderQueue());
                            if (item != null) {
                                item = Prevalence.getInstance().execute(new PollRenderQueue());
                                if (item != null) {
                                    log.debug("Rendering segment from {}/{} with hash {}", item.getRemoteSiteConsumerUrl(), item.getPageTitle(), Hex.encodeHexString(item.getSegmentHash()));
                                    WikispeechApi.ListenResponse listenResponse = null;
                                    try {
                                        listenResponse = wikispeech.listen(
                                                item.getRemoteSiteConsumerUrl(),
                                                Hex.encodeHexString(item.getSegmentHash()),
                                                item.getPageRevision(),
                                                item.getLanguage()
                                        );
                                    } catch (SocketTimeoutException timeoutException) {
                                        log.warn("Timeout while rendering. Requeuing {}", item, timeoutException);
                                        Prevalence.getInstance().execute(new PushRenderQueue(
                                                item.getRemoteSiteConsumerUrl(),
                                                item.getPageTitle(),
                                                item.getSegmentHash(),
                                                item.getPageRevision(),
                                                item.getLanguage(),
                                                item.getVoice()
                                        ));
                                    } catch (Exception exception) {
                                        log.error("Failed to render {}", item, exception);
                                        continue;
                                    }
                                    if (listenResponse != null) {
                                        Segment segment = Prevalence.getInstance().execute(
                                                new SetRenderedSegment(
                                                        item.getRemoteSiteConsumerUrl(),
                                                        item.getPageTitle(),
                                                        item.getSegmentHash(),
                                                        item.getLanguage(),
                                                        item.getVoice()
                                                )
                                        );
                                    }
                                }
                            }
                            if (item == null) {
                                log.trace("Queue is empty. Awaiting more items.");
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            log.fatal("Fatal exception! Thread will stop!", e);
                            break;
                        }
                    }
                } finally {
                    log.info("Thread stops.");
                    stoppedLatch.countDown();
                }
            }).start();
        }
        this.stoppedLatch = stoppedLatch;
    }

    public void stop() {
        if (!running.get()) {
            throw new RuntimeException("Already stopped!");
        }
        running.set(false);
        try {
            stoppedLatch.await();
        } catch (InterruptedException ie) {
            log.error("Interrupted while awaiting threads to close down.", ie);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void queuePage(String consumerUrl, String title, String language, String voice, LocalDateTime existingMustBeRenderedBefore) throws Exception {
        long currentRevision = wikispeech.getCurrentRevision(consumerUrl, title);
        List<WikispeechApi.Segment> segments = wikispeech.segment(consumerUrl, title);
        log.info("Populating queue with {} segments from {} in {}", segments.size(), title, consumerUrl);
        for (WikispeechApi.Segment segment : segments) {
            if (!segment.getHash().equals(Hex.encodeHexString(Hex.decodeHex(segment.getHash())))) {
                System.currentTimeMillis();
            }
            Segment renderedSegment = Prevalence.getInstance().execute(new FindRenderedSegment(
                    consumerUrl,
                    title,
                    Hex.decodeHex(segment.getHash()),
                    language,
                    voice
            ));
            if (renderedSegment != null) {
                if (renderedSegment.getTimestampRendered().isAfter(existingMustBeRenderedBefore)) {
                    continue;
                }
            }
            Prevalence.getInstance().execute(new PushRenderQueue(
                    consumerUrl,
                    title,
                    Hex.decodeHex(segment.getHash()),
                    currentRevision,
                    language,
                    voice
            ));
        }

    }

}

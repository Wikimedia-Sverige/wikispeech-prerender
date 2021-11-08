package se.wikimedia.wikispeech.prerender;

import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.SegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.domain.SynthesizedSegment;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.*;
import se.wikimedia.wikispeech.prerender.prevalence.query.FindSegmentedPage;
import se.wikimedia.wikispeech.prerender.prevalence.query.FindSynthesizedSegment;
import se.wikimedia.wikispeech.prerender.prevalence.query.command.PeekCommandQueue;
import se.wikimedia.wikispeech.prerender.prevalence.query.command.PeekSynthesizeSegmentCommandQueue;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.SetPageLastSegmented;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.SetSynthesizedSegment;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PollCommandQueue;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PollSynthesizeSegmentCommandQueue;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushSegmentPageAndQueueForSynthesis;
import se.wikimedia.wikispeech.prerender.prevalence.transaction.command.PushSynthesizeSegmentToCommandQueue;
import se.wikimedia.wikispeech.prerender.site.ScrapePageForWikiLinks;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandQueue {

    private Logger log = LogManager.getLogger();

    private static CommandQueue instance = new CommandQueue();

    public static CommandQueue getInstance() {
        return instance;
    }

    @Setter
    private int numberOfWorkerThreads = 1;
    @Setter
    private int numberOfSynthesizeWorkerThreads = 1;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch stoppedLatch;

    private CommandQueue() {

    }

    public void start() {
        if (running.get()) {
            throw new RuntimeException("Already running!");
        }
        running.set(true);

        CountDownLatch stoppedLatch = new CountDownLatch(numberOfWorkerThreads + numberOfSynthesizeWorkerThreads);

        for (int i = 0; i < numberOfWorkerThreads; i++) {
            new Thread(() -> {
                try {
                    while (running.get()) {
                        try {
                            Command command = Prevalence.getInstance().execute(new PeekCommandQueue());
                            if (command != null) {
                                command = Prevalence.getInstance().execute(new PollCommandQueue());
                                if (command != null) {
                                    log.debug("Processing {}", command);
                                    command.accept(processCommandVisitor);
                                }
                            }
                            if (command == null) {
                                log.trace("Queue is empty. Awaiting more items...");
                                Thread.sleep(1000);
                            }
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

        for (int i = 0; i < numberOfSynthesizeWorkerThreads; i++) {
            new Thread(() -> {
                try {
                    while (running.get()) {
                        try {
                            Command command = Prevalence.getInstance().execute(new PeekSynthesizeSegmentCommandQueue());
                            if (command != null) {
                                command = Prevalence.getInstance().execute(new PollSynthesizeSegmentCommandQueue());
                                if (command != null) {
                                    log.debug("Processing {}", command);
                                    command.accept(processCommandVisitor);
                                }
                            }
                            if (command == null) {
                                log.trace("Queue is empty. Awaiting more items...");
                                Thread.sleep(1000);
                            }
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
        this.stoppedLatch = stoppedLatch;
    }

    public void stop() {
        if (!running.get()) {
            throw new RuntimeException("Already stopped!");
        }
        running.set(false);
        try {
            stoppedLatch.await();
            stoppedLatch = null;
        } catch (InterruptedException ie) {
            log.error("Interrupted while awaiting threads to close down.", ie);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private CommandVisitor<Void> processCommandVisitor = new CommandVisitor<Void>() {
        @Override
        public Void visit(SegmentPageAndQueueForSynthesis command) {
            try {
                WikispeechApi api = new WikispeechApi();
                api.open();
                long currentRevision = api.getCurrentRevision(command.getConsumerUrl(), command.getTitle());

                // if revision haven't changed since last visit, then no need to process.
                SegmentedPage segmentedPage = Prevalence.getInstance().execute(new FindSegmentedPage(command.getConsumerUrl(), command.getTitle()));
                if (segmentedPage != null
                        && segmentedPage.getLastSegmentedRevision() != null
                        && segmentedPage.getLastSegmentedRevision() == currentRevision) {
                    return null;
                    // no need
                }
                api.segment(command.getConsumerUrl(), command.getTitle(), new Collector<WikispeechApi.Segment>() {
                    @Override
                    public void collect(WikispeechApi.Segment segment) {
                        try {
                            // segment might have previously been synthesized with a greater revision than the revision we asked for when adding it to the queue.
                            SynthesizedSegment synthesizedSegment = Prevalence.getInstance().execute(
                                    new FindSynthesizedSegment(
                                            command.getConsumerUrl(),
                                            command.getTitle(),
                                            Hex.decodeHex(segment.getHash()),
                                            command.getLanguage(),
                                            command.getVoice()
                                    )
                            );
                            if (synthesizedSegment != null
                                    && currentRevision <= synthesizedSegment.getRevision()) {
                                // No need
                                return;
                            }
                            PushSynthesizeSegmentToCommandQueue push = new PushSynthesizeSegmentToCommandQueue();
                            push.setCurrentRevisionAtSegmentation(currentRevision);
                            push.setConsumerUrl(command.getConsumerUrl());
                            push.setTitle(command.getTitle());
                            push.setHash(Hex.decodeHex(segment.getHash()));
                            push.setContentStartOffset(segment.getStartOffset());
                            push.setContentEndOffset(segment.getEndOffset());
                            push.setLanguage(command.getLanguage());
                            push.setVoice(command.getVoice());
                            if (segment.getContent() != null) {
                                push.setContentXPathExpressions(new ArrayList<>(segment.getContent().length));
                                push.setContentTexts(new ArrayList<>(segment.getContent().length));
                                for (WikispeechApi.SegmentContent content : segment.getContent()) {
                                    push.getContentXPathExpressions().add(content.getPath());
                                    push.getContentTexts().add(content.getString());
                                }
                            }
                            Prevalence.getInstance().execute(push);
                        } catch (Exception e) {
                            log.error("Failed to queue synthesize segment {} via {}", segment, command, e);
                        }
                    }
                });
                Prevalence.getInstance().execute(new SetPageLastSegmented(command.getConsumerUrl(), command.getTitle(), currentRevision));
            } catch (Exception e) {
                log.error("Failed to process {}", command, e);
            }
            return null;
        }

        @Override
        public Void visit(SynthesizeSegment command) {
            try {
                SynthesizedSegment synthesizedSegment = Prevalence.getInstance().execute(
                        new FindSynthesizedSegment(
                                command.getConsumerUrl(),
                                command.getTitle(),
                                command.getHash(),
                                command.getLanguage(),
                                command.getVoice()
                        )
                );

                if (synthesizedSegment != null
                        && synthesizedSegment.getRevision() >= command.getCurrentRevisionAtSegmentation()) {
                    // no need
                    return null;
                }

                WikispeechApi api = new WikispeechApi();
                api.open();
                try {
                    WikispeechApi.ListenResponseEnvelope listenResponse = api.listen(
                            command.getConsumerUrl(),
                            command.getTitle(),
                            Hex.encodeHexString(command.getHash()),
                            command.getCurrentRevisionAtSegmentation(),
                            command.getLanguage()
                    );
                    Prevalence.getInstance().execute(
                            new SetSynthesizedSegment(
                                    command.getConsumerUrl(),
                                    command.getTitle(),
                                    command.getHash(),
                                    command.getLanguage(),
                                    command.getVoice(),
                                    listenResponse.getRevision()
                            )
                    );
                } catch (SocketTimeoutException timeoutException) {
                    log.warn("Timeout while rendering. Requeuing {}", command, timeoutException);
                    Prevalence.getInstance().execute(PushSynthesizeSegmentToCommandQueue.factory(command));
                } catch (Exception exception) {
                    log.error("Failed to process {}", command, exception);
                }
            } catch (Exception e) {
                log.error("Failed to process {}", command, e);
            }
            return null;
        }

        @Override
        public Void visit(ScrapePageForWikiLinksAndQueueLinkedPagesForSegmentation command) {
            try {
                ScrapePageForWikiLinks scraper = new ScrapePageForWikiLinks();
                scraper.setConsumerUrl(command.getConsumerUrl());
                scraper.setTitle(command.getTitle());
                scraper.setLinksExpression(command.getLinksExpression());
                scraper.setAllowedHrefPattern(command.getAllowedHrefPattern());
                scraper.setCollector(new Collector<String>() {
                    @Override
                    public void collect(String title) {
                        PushSegmentPageAndQueueForSynthesis push = new PushSegmentPageAndQueueForSynthesis();
                        push.setConsumerUrl(command.getConsumerUrl());
                        push.setTitle(title);
                        push.setLanguage(command.getLanguage());
                        push.setVoice(command.getVoice());
                        try {
                            Prevalence.getInstance().execute(push);
                        } catch (Exception e) {
                            log.error("Failed to execute {} via {}", push, command, e);
                        }
                    }
                });
                scraper.execute();
            } catch (Exception e) {
                log.error("Failed to process {}", command, e);
            }
            return null;
        }
    };

}

package se.wikimedia.wikispeech.prerender.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prevayler.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PageCleanupService {

    private final Logger log = LogManager.getLogger(getClass());

    private final Prevalence prevalence;

    private final Duration maximumPageChangeAge = Duration.ofDays(7);

    @Autowired
    public PageCleanupService(Prevalence prevalence) {
        this.prevalence = prevalence;
    }

    @Scheduled(initialDelay = 10, fixedDelay = 60 * 24, timeUnit = TimeUnit.MINUTES)
    public void cleanup() throws Exception {

    }

}

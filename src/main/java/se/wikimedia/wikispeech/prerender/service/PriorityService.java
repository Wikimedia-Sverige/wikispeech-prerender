package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class PriorityService {

    private final Logger log = LogManager.getLogger(getClass());

    private final Map<Object, PrioritySetting> settings = new ConcurrentHashMap<>();

    public void put(Object key, PrioritySetting prioritySetting) {
        settings.put(key, prioritySetting);
    }

    public float getMultiplier(
            Wiki wiki,
            Page page,
            PageSegment pageSegment,
            PageSegmentVoice pageSegmentVoice,
            String voice
    ) {
        LocalDateTime now = LocalDateTime.now();
        float greatestMultiplier = 1F;
        PrioritySettingVisitor<Boolean> visitor = new PrioritySettingVisitor<Boolean>() {
            @Override
            public Boolean visit(PagePrioritySetting setting) {
                return setting.getConsumerUrl().equals(wiki.getConsumerUrl())
                        && setting.getTitle().equals(page.getTitle());
            }
        };
        for (PrioritySetting prioritySetting : new HashSet<>(settings.values())) {
            if (prioritySetting.getFrom().isBefore(now)
                    && prioritySetting.getTo().isAfter(now)
                    && prioritySetting.accept(visitor)) {
                if (prioritySetting.getMultiplier() > greatestMultiplier) {
                    greatestMultiplier = prioritySetting.getMultiplier();
                }
            }
        }
        return greatestMultiplier;
    }


    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void expunge() {
        log.info("Expunging...");
        for (Map.Entry<Object, PrioritySetting> entry : new HashSet<>(settings.entrySet())) {
            if (entry.getValue().getTo().isBefore(LocalDateTime.now())) {
                settings.remove(entry.getKey());
            }
        }
    }

    public static interface PrioritySettingVisitor<R> {
        public abstract R visit(PagePrioritySetting setting);
    }

    @Data
    public abstract static class PrioritySetting {
        private LocalDateTime from;
        private LocalDateTime to;
        private float multiplier;

        public PrioritySetting() {
        }

        public PrioritySetting(LocalDateTime from, LocalDateTime to, float multiplier) {
            this.from = from;
            this.to = to;
            this.multiplier = multiplier;
        }

        public abstract <R> R accept(PrioritySettingVisitor<R> visitor);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PagePrioritySetting extends PrioritySetting {
        private String consumerUrl;
        private String title;

        public PagePrioritySetting() {
        }

        public PagePrioritySetting(LocalDateTime from, LocalDateTime to, float multiplier, String consumerUrl, String title) {
            super(from, to, multiplier);
            this.consumerUrl = consumerUrl;
            this.title = title;
        }

        @Override
        public <R> R accept(PrioritySettingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

}

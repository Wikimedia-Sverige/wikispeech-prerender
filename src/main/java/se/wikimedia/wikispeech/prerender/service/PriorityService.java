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

    public PrioritySetting getMultiplier(
            Wiki wiki,
            Page page,
            PageSegment pageSegment,
            PageSegmentVoice pageSegmentVoice,
            String voice
    ) {
        LocalDateTime now = LocalDateTime.now();
        PrioritySetting greatestMultiplier = null;
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
                if (greatestMultiplier == null || prioritySetting.getMultiplier() > greatestMultiplier.getMultiplier()) {
                    greatestMultiplier = prioritySetting;
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
        private double multiplier;

        public PrioritySetting() {
        }

        public PrioritySetting(LocalDateTime from, LocalDateTime to, double multiplier) {
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

    @Data
    public static class CalculatedPriority {
        private double value;
        private List<Explanation> explanations;
     }

    @Data
    public static class Explanation {
        private double value;
        private String expression;

        public Explanation(double value, String expression) {
            this.value = value;
            this.expression = expression;
        }
    }

    public CalculatedPriority calculatePriority(SynthesizeService.CandidateToBeSynthesized candidateToBeSynthesized, boolean explain) {

        double value = 1D;

        CalculatedPriority priority = new CalculatedPriority();
        if (explain) {
            priority.setExplanations(new ArrayList<>());
            priority.getExplanations().add(new Explanation(1D, "Starting value"));
        }

        value *= candidateToBeSynthesized.getPage().getPriority();
        if (explain) {
            priority.getExplanations().add(new Explanation(value, "multiplied with page priority " + candidateToBeSynthesized.getPage().getPriority()));
        }

        if (candidateToBeSynthesized.getPageSegmentVoice() != null
                && candidateToBeSynthesized.getPageSegmentVoice().getFailedAttempts() != null
                && !candidateToBeSynthesized.getPageSegmentVoice().getFailedAttempts().isEmpty()) {
            value /= candidateToBeSynthesized.getPageSegmentVoice().getFailedAttempts().size() + 1D;
            if (explain) {
                priority.getExplanations().add(new Explanation(value, "Divided with number of failures " + candidateToBeSynthesized.getPageSegmentVoice().getFailedAttempts().size()));
            }
        }

        // lower segment index in page is slightly more prioritized
        value += 1D - Math.min(1000, candidateToBeSynthesized.getPageSegment().getLowestIndexAtSegmentation()) / 1000D;
        if (explain) {
            priority.getExplanations().add(new Explanation(value, "Added priority for page segment index " + candidateToBeSynthesized.getPageSegment().getLowestIndexAtSegmentation()));
        }

        // no synthesized voice at all is slightly more prioritized
        if (candidateToBeSynthesized.getPageSegmentVoice() == null) {
            value += 0.001D;
            if (explain) {
                priority.getExplanations().add(new Explanation(value, "Added never previously synthesized priority."));
            }
        }

        PrioritySetting multiplier = getMultiplier(
                candidateToBeSynthesized.getWiki(),
                candidateToBeSynthesized.getPage(),
                candidateToBeSynthesized.getPageSegment(),
                candidateToBeSynthesized.getPageSegmentVoice(),
                candidateToBeSynthesized.getVoice()
        );
        if (multiplier != null) {
            value *= multiplier.getMultiplier();
            if (explain) {
                priority.getExplanations().add(new Explanation(value, "Multiplied with " + multiplier));
            }
        }

        priority.setValue(value);
        return priority;
    }


}

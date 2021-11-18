package se.wikimedia.wikispeech.prerender.service;

import lombok.Data;
import org.springframework.stereotype.Service;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class PriorityService {

    private Set<PrioritySetting> settings = new HashSet<>();

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
        for (PrioritySetting prioritySetting : settings) {
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


    public void expunge() {
        settings.removeIf(prioritySetting -> prioritySetting.getTo().isBefore(LocalDateTime.now()));
    }

    public static interface PrioritySettingVisitor<R> {
        public abstract R visit(PagePrioritySetting setting);
    }

    @Data
    public abstract static class PrioritySetting {
        private LocalDateTime from;
        private LocalDateTime to;
        private float multiplier;

        public abstract <R> R accept(PrioritySettingVisitor<R> visitor);
    }

    @Data
    public static class PagePrioritySetting extends PrioritySetting {
        private String consumerUrl;
        private String title;

        @Override
        public <R> R accept(PrioritySettingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

}

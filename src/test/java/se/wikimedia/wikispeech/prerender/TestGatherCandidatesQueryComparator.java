package se.wikimedia.wikispeech.prerender;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import se.wikimedia.wikispeech.prerender.service.SynthesizeService;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import se.wikimedia.wikispeech.prerender.service.SynthesizeService.GatherCandidatesQuery.SegmentVoiceToBeSynthesized;
import se.wikimedia.wikispeech.prerender.service.SynthesizeService.GatherCandidatesQuery.SegmentVoiceToBeSynthesizedComparator;

import java.time.LocalDateTime;
import java.util.*;

public class TestGatherCandidatesQueryComparator {

    private Comparator<SegmentVoiceToBeSynthesized> comparator = new SegmentVoiceToBeSynthesizedComparator();

    private Random random;

    @Before
    public void setup() {
        long seed = System.currentTimeMillis();
        random = new Random(seed);
        System.out.println("Using random seed " + seed);
    }

    @Test
    public void test() {
        List<SegmentVoiceToBeSynthesized> list = new ArrayList<>();

        String language = null;
        String voice = null;

        Wiki wiki = new Wiki();
        wiki.setConsumerUrl("https://wiki/w");

        {
            Page page = new Page();
            page.setTitle("Title 1");
            page.setTimestampSegmented(LocalDateTime.parse("2021-11-13T10:00:00"));

            PageSegment segment = new PageSegment();
            segment.setHash(new byte[]{1});

            list.add(new SegmentVoiceToBeSynthesized(
                    wiki, page, segment, null, language, voice
            ));
        }
        {
            Page page = new Page();
            page.setTitle("Title 2");
            page.setTimestampSegmented(LocalDateTime.parse("2021-11-13T11:00:00"));

            PageSegment segment = new PageSegment();
            segment.setHash(new byte[]{2});

            list.add(new SegmentVoiceToBeSynthesized(
                    wiki, page, segment, null, language, voice
            ));
        }
        {
            Page page = new Page();
            page.setTitle("Title 3");
            page.setTimestampSegmented(LocalDateTime.parse("2021-11-13T11:00:00"));

            PageSegment segment = new PageSegment();
            segment.setHash(new byte[]{3});

            list.add(new SegmentVoiceToBeSynthesized(
                    wiki, page, segment, new PageSegmentVoice(), language, voice
            ));
        }
        // todo test query
//        {
//            Page page = new Page();
//            page.setTitle("Title 4 should be ignored due to recent synthesis");
//            page.setTimestampSegmented(LocalDateTime.now());
//
//            PageSegment segment = new PageSegment();
//            segment.setHash(new byte[]{4});
//
//            list.add(new SegmentVoiceToBeSynthesized(
//                    wiki, page, segment, new PageSegmentVoice(), language, voice
//            ));
//        }

        for (int i = 0; i<10; i++) {
            Collections.shuffle(list, random);
            list.sort(comparator);
            Assert.assertEquals("Title 1", list.get(0).getPage().getTitle());
            Assert.assertEquals("Title 2", list.get(1).getPage().getTitle());
            Assert.assertEquals("Title 3", list.get(2).getPage().getTitle());
        }
    }

}

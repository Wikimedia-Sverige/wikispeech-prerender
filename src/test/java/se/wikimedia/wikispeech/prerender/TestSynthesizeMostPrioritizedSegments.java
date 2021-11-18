package se.wikimedia.wikispeech.prerender;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.prevayler.PrevaylerFactory;
import se.wikimedia.wikispeech.prerender.service.PriorityService;
import se.wikimedia.wikispeech.prerender.service.SynthesizeService;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegment;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.PageSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class TestSynthesizeMostPrioritizedSegments {

    private Random random;

    @Before
    public void setup() {
        long seed = System.currentTimeMillis();
        random = new Random(seed);
        System.out.println("Using random seed " + seed);

    }

    @Test
    public void test() throws Exception {
        Root root = new Root();
        Wiki svwp = new Wiki();
        svwp.setConsumerUrl("https://sv.wikipedia.org/w");
        svwp.setDefaultLanguage("sv");
        svwp.setName("Svenska Wikipedia");
        root.getWikiByConsumerUrl().put(svwp.getConsumerUrl(), svwp);

        {
            Page page = new Page();
            page.setTitle("Title 1");
            page.setTimestampSegmented(LocalDateTime.parse("2021-11-13T10:00:00"));
            page.setRevisionAtSegmentation(1L);

            PageSegment segment = new PageSegment();
            segment.setHash(new byte[]{1, 1});
            segment.setLowestIndexAtSegmentation(1);
            page.getSegments().add(segment);

            svwp.getPagesByTitle().put(page.getTitle(), page);
        }
        {
            Page page = new Page();
            page.setTitle("Title 2");
            page.setTimestampSegmented(LocalDateTime.parse("2021-11-13T11:00:00"));
            page.setRevisionAtSegmentation(2L);

            {
                PageSegment segment = new PageSegment();
                segment.setHash(new byte[]{2, 2});
                segment.setLowestIndexAtSegmentation(2);
                page.getSegments().add(segment);
            }
            {
                PageSegment segment = new PageSegment();
                segment.setHash(new byte[]{2, 1});
                segment.setLowestIndexAtSegmentation(1);
                page.getSegments().add(segment);
            }

            svwp.getPagesByTitle().put(page.getTitle(), page);
        }
        {
            Page page = new Page();
            page.setTitle("Title 3");
            page.setTimestampSegmented(LocalDateTime.parse("2021-11-13T11:00:00"));
            page.setRevisionAtSegmentation(3L);

            PageSegment segment = new PageSegment();
            segment.setHash(new byte[]{3});
            segment.setLowestIndexAtSegmentation(2);
            page.getSegments().add(segment);

            svwp.getPagesByTitle().put(page.getTitle(), page);
        }
        {
            Page page = new Page();
            page.setTitle("Title 4 should be ignored due to recent synthesis");
            page.setTimestampSegmented(LocalDateTime.now());
            page.setRevisionAtSegmentation(4L);

            PageSegment segment = new PageSegment();
            segment.setHash(new byte[]{4});
            segment.setLowestIndexAtSegmentation(0);
            page.getSegments().add(segment);

            PageSegmentVoice pageSegmentVoice = new PageSegmentVoice();
            pageSegmentVoice.setTimestampSynthesized(LocalDateTime.now());
            segment.getSynthesizedVoices().add(pageSegmentVoice);

            svwp.getPagesByTitle().put(page.getTitle(), page);
        }


        Prevalence prevalence = new Prevalence();
        prevalence.setPrevalyer(PrevaylerFactory.createTransientPrevayler(root));

        PriorityService priorityService = new PriorityService();

        List<SynthesizeService.SynthesizeCommand> list = prevalence.execute(new SynthesizeService.GatherCandidatesQuery(priorityService, 100));
        Assert.assertEquals(4, list.size());

        Assert.assertEquals("Title 1", list.get(0).getTitle());
        Assert.assertEquals("Title 2", list.get(1).getTitle());
        Assert.assertArrayEquals(new byte[]{2, 1}, list.get(1).getHash());
        Assert.assertEquals("Title 2", list.get(2).getTitle());
        Assert.assertArrayEquals(new byte[]{2, 2}, list.get(2).getHash());
        Assert.assertEquals("Title 3", list.get(3).getTitle());
    }

}

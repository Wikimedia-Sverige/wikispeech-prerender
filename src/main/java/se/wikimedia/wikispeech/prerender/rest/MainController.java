package se.wikimedia.wikispeech.prerender.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.wikimedia.wikispeech.prerender.mediawiki.PageApi;
import se.wikimedia.wikispeech.prerender.service.SegmentService;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.GetWiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.*;
import se.wikimedia.wikispeech.prerender.site.WikiResolver;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@RestController()
@RequestMapping(path = "/api")
public class MainController {

    private final Prevalence prevalence;
    private final PageApi pageApi;
    private final SegmentService segmentService;
    private final ObjectMapper objectMapper;

    public MainController(
            @Autowired Prevalence prevalence,
            @Autowired SegmentService segmentService,
            @Autowired PageApi pageApi
    ) {
        this.prevalence = prevalence;
        this.pageApi = pageApi;
        this.segmentService = segmentService;
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            path = "wiki",
            produces = "application/json"
    )
    public ResponseEntity<String> createWiki(
            @RequestParam String consumerUrl
    ) throws Exception {
        // todo assert correct consumerUrl.
        Wiki wiki = prevalence.execute(new GetWiki(consumerUrl));
        if (wiki != null) {
            return ResponseEntity.badRequest().body("{\"error\", \"Already exists\"}");
        }
        WikiResolver resolver = new WikiResolver();
        resolver.detect(consumerUrl);

        PageApi.PageInfo pageInfo = pageApi.getPageInfo(consumerUrl, resolver.getMainPageTitle());

        CreateWiki createWiki = new CreateWiki();
        createWiki.setConsumerUrl(consumerUrl);
        createWiki.setName(resolver.getWikiName());
        createWiki.setDefaultLanguage(pageInfo.getPageLanguage());
        Set<Integer> namespaces = new LinkedHashSet<>();
        namespaces.add(0);
        namespaces.add(pageInfo.getNamespaceIdentity());
        createWiki.setPollRecentChangesNamespaces(new ArrayList<>(namespaces));
        createWiki.setTimestampOfLastRecentChangesItemProcessed(OffsetDateTime.now().minusDays(7));
        // todo request setting
        createWiki.setMaximumSynthesizedVoiceAge(Duration.ofDays(30));
        // todo request setting
        createWiki.setVoicesPerLanguage(resolver.getDefaultVoicesByLanguage());
        wiki = prevalence.execute(createWiki);

        CreateNonSegmentedPage createMainPage = new CreateNonSegmentedPage();
        createMainPage.setConsumerUrl(wiki.getConsumerUrl());
        createMainPage.setTitle(resolver.getMainPageTitle());
        createMainPage.setLanguageAtSegmentation(pageInfo.getPageLanguage());
        createMainPage.setRevisionAtSegmentation(pageInfo.getLastRevisionIdentity());
        createMainPage.setPriority(10F);
        Page mainPage = prevalence.execute(createMainPage);

        prevalence.execute(new SetWikiMainPage(consumerUrl, mainPage.getTitle()));

        segmentService.segment(consumerUrl, createMainPage.getTitle());

        return ResponseEntity.ok(objectMapper.writeValueAsString(wiki));
    }


}

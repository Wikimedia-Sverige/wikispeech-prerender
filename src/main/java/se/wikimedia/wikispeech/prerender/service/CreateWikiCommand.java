package se.wikimedia.wikispeech.prerender.service;

import se.wikimedia.wikispeech.prerender.mediawiki.PageApi;
import se.wikimedia.wikispeech.prerender.service.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Page;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.query.GetWiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateNonSegmentedPage;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.CreateWiki;
import se.wikimedia.wikispeech.prerender.service.prevalence.transaction.SetWikiMainPage;
import se.wikimedia.wikispeech.prerender.site.WikiResolver;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CreateWikiCommand {

    private final Prevalence prevalence;
    private final PageApi pageApi;
    private final SegmentService segmentService;

    private final String consumerUrl;

    private OffsetDateTime timestampOfLastRecentChangesItemProcessed = OffsetDateTime.now();

    private Duration maximumSynthesizedVoiceAge = Duration.ofDays(30);

    private Map<String, Set<String>> voicesPerLanguage = null;

    private Float mainPagePriority = 10F;

    public CreateWikiCommand(
            Prevalence prevalence,
            SegmentService segmentService,
            PageApi pageApi,

            String consumerUrl
    ) {
        this.prevalence = prevalence;
        this.pageApi = pageApi;
        this.segmentService = segmentService;

        this.consumerUrl = consumerUrl;
    }

    public Wiki execute() throws Exception {
        Wiki wiki = prevalence.execute(new GetWiki(consumerUrl));
        if (wiki != null)
            throw new IllegalStateException("Wiki with consumer URL '"+consumerUrl+"' already exists");
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
        createWiki.setTimestampOfLastRecentChangesItemProcessed(timestampOfLastRecentChangesItemProcessed);
        createWiki.setMaximumSynthesizedVoiceAge(maximumSynthesizedVoiceAge);
        if (voicesPerLanguage == null)
            createWiki.setVoicesPerLanguage(resolver.getDefaultVoicesByLanguage());
        else
            createWiki.setVoicesPerLanguage(voicesPerLanguage);

        wiki = prevalence.execute(createWiki);

        CreateNonSegmentedPage createMainPage = new CreateNonSegmentedPage();
        createMainPage.setConsumerUrl(wiki.getConsumerUrl());
        createMainPage.setTitle(resolver.getMainPageTitle());
        createMainPage.setLanguageAtSegmentation(pageInfo.getPageLanguage());
        createMainPage.setRevisionAtSegmentation(pageInfo.getLastRevisionIdentity());
        createMainPage.setPriority(mainPagePriority);
        Page mainPage = prevalence.execute(createMainPage);

        prevalence.execute(new SetWikiMainPage(consumerUrl, mainPage.getTitle()));

        segmentService.segment(consumerUrl, createMainPage.getTitle());

        return wiki;
    }

    public CreateWikiCommand setTimestampOfLastRecentChangesItemProcessed(OffsetDateTime timestampOfLastRecentChangesItemProcessed) {
        this.timestampOfLastRecentChangesItemProcessed = timestampOfLastRecentChangesItemProcessed;
        return this;
    }

    public CreateWikiCommand setMaximumSynthesizedVoiceAge(Duration maximumSynthesizedVoiceAge) {
        this.maximumSynthesizedVoiceAge = maximumSynthesizedVoiceAge;
        return this;
    }

    public CreateWikiCommand setVoicesPerLanguage(Map<String, Set<String>> voicesPerLanguage) {
        this.voicesPerLanguage = voicesPerLanguage;
        return this;
    }

    public CreateWikiCommand setMainPagePriority(Float mainPagePriority) {
        this.mainPagePriority = mainPagePriority;
        return this;
    }
}

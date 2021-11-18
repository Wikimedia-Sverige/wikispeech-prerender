package se.wikimedia.wikispeech.prerender.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import okhttp3.*;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class WikiResolver {

    public static void main(String... args) throws Exception {
        WikiResolver svwp = new WikiResolver();
        svwp.detect(SwedishWikipedia.CONSUMER_URL_SV_WIKIPEDIA);
        WikiResolver enwp = new WikiResolver();
        enwp.detect(EnglishWikipedia.CONSUMER_URL_EN_WIKIPEDIA);
        System.currentTimeMillis();
    }

    private final ObjectMapper objectMapper;

    public WikiResolver(
    ) {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Getter
    private String wikiName;
    @Getter
    private String mainPageUrl;
    @Getter
    private String mainPageTitle;
    @Getter
    private Document mainPageDocument;
    @Getter
    private Map<String, Set<String>> defaultVoicesByLanguage;

    public void detect(String consumerUrl) throws Exception {
        loadFromMainPage(consumerUrl);
        loadDefaultVoicesByLanguageFromWikispeechConfig();
    }

    private void loadFromMainPage(String consumerUrl) throws IOException, SAXException, XPathExpressionException {
        OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).build();

        String location = consumerUrl;
        int redirects = 0;
        while (redirects++ < 5) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(location).newBuilder();
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .build();
            Call call = client.newCall(request);
            Response response = call.execute();
            if (response.code() == 200) {
                mainPageUrl = location;
                mainPageTitle = location.substring(location.lastIndexOf("/") + 1);
                DOMParser parser = new DOMParser();
                parser.parse(new InputSource(response.body().byteStream()));
                mainPageDocument = parser.getDocument();
                wikiName = XPathFactory.newInstance().newXPath().compile("//TITLE/text()").evaluate(mainPageDocument);
                return;
            } else if (response.code() == 301) {
                location = response.header("Location");
            } else {
                throw new RuntimeException();
            }
        }
        throw new RuntimeException("Too many redirects");
    }

    public void loadDefaultVoicesByLanguageFromWikispeechConfig() throws Exception {
        Map<String, Set<String>> voicesByLanguage = new HashMap<>();
        String url = "https://raw.githubusercontent.com/wikimedia/mediawiki-extensions-Wikispeech/master/extension.json";
        ObjectNode objectNode = objectMapper.readValue(new URL(url), ObjectNode.class);
        ObjectNode voicesPerLanguage = (ObjectNode) objectNode.get("config").get("WikispeechVoices").get("value");
        for (Iterator<String> languages = voicesPerLanguage.fieldNames(); languages.hasNext(); ) {
            String language = languages.next();
            ArrayNode voicesJson = (ArrayNode) voicesPerLanguage.get(language);
            Set<String> voices = new HashSet<>(voicesJson.size());
            voicesByLanguage.put(language, voices);
            for (int voiceIndex = 0; voiceIndex < voicesJson.size(); voiceIndex++) {
                String voice = voicesJson.get(voiceIndex).textValue();
                voices.add(voice);
            }
        }
        this.defaultVoicesByLanguage = voicesByLanguage;
    }

}
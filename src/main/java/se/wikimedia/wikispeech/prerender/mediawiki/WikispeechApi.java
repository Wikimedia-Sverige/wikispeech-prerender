package se.wikimedia.wikispeech.prerender.mediawiki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.Getter;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.service.Settings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WikispeechApi {

    private final Logger log = LogManager.getLogger(getClass());

    private final boolean skipJournalMetrics;
    private final String wikispeechBaseUrl;

    private final ObjectMapper objectMapper;
    private final OkHttpClient client;

    private final PageApi pageApi;

    @Autowired
    public WikispeechApi(
            Settings settings,
            PageApi pageApi,
            OkHttpClient client
    ) {
        this.pageApi = pageApi;
        this.client = client;

        this.wikispeechBaseUrl = settings.getString("WikispeechApi.wikispeechBaseUrl", "https://wikispeech.wikimedia.se/w");
        this.skipJournalMetrics = settings.getBoolean("WikispeechApi.skipJournalMetrics", true);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public void segment(String consumerUrl, String page, Collector<Segment> collector) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(wikispeechBaseUrl + "/api.php").newBuilder();
        urlBuilder.addQueryParameter("action", "wikispeech-segment");
        urlBuilder.addQueryParameter("consumer-url", consumerUrl);
        urlBuilder.addQueryParameter("format", "json");
        urlBuilder.addQueryParameter("page", page);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.code() != 200) {
            throw new IOException("Response" + response);
        }

        JsonNode json = objectMapper.readTree(response.body().byteStream());
        ArrayNode segmentsJson = objectMapper.convertValue(json.get("wikispeech-segment").get("segments"), ArrayNode.class);
        for (int i = 0; i < segmentsJson.size(); i++) {
            if (!collector.collect(objectMapper.convertValue(segmentsJson.get(i), Segment.class))) {
                break;
            }
        }

    }

    /**
     * @param consumerUrl
     * @param title
     * @return null if page does not exist.
     * @throws IOException
     */
    public Long getCurrentRevision(String consumerUrl, String title) throws IOException {
        PageApi.PageInfo pageInfo = pageApi.getPageInfo(consumerUrl, title);
        return pageInfo == null ? null : pageInfo.getLastRevisionIdentity();
    }

    public static class MWException extends IOException {

        @Getter
        private final JsonNode error;

        public MWException(JsonNode error) {
            this.error = error;
        }

        public String getExceptionClass() {
            return error.toString();
        }
    }

    /**
     * A cache of the greatest revision known of all pages processed in all wikis
     * todo: Make it a guava-cache that flush out oldest items after X MB size
     */
    private Map<String, Map<String, Long>> mostRecentRevisionSeenCache = new HashMap<>();

    private long getGreatestRevisionKnown(String consumerUrl, String title, long knownRevision) {
        Map<String, Long> revisionsByTitle = mostRecentRevisionSeenCache.computeIfAbsent(consumerUrl, k -> new HashMap<>());
        Long revision = revisionsByTitle.get(title);
        if (revision == null) {
            revisionsByTitle.put(title, knownRevision);
            return knownRevision;
        }
        return revision;
    }

    private void setGreatestRevisionKnown(String consumerUrl, String title, long revision) {
        mostRecentRevisionSeenCache.computeIfAbsent(consumerUrl, k -> new HashMap<>()).put(title, revision);
    }

    public ListenResponseEnvelope listen(String consumerUrl, String title, String segmentHash, long lastKnownRevision, String language, String voice) throws IOException {
        ListenResponseEnvelope envelope = new ListenResponseEnvelope();
        long greatestRevisionKnown = getGreatestRevisionKnown(consumerUrl, title, lastKnownRevision);
        try {
            envelope.setResponse(doListen(consumerUrl, segmentHash, greatestRevisionKnown, language, voice));
            envelope.setRevision(lastKnownRevision);
        } catch (MWException mwException) {
            if (mwException.getError().toString().contains("OutdatedOrInvalidRevisionException")) {
                greatestRevisionKnown = getCurrentRevision(consumerUrl, title);
                setGreatestRevisionKnown(consumerUrl, title, greatestRevisionKnown);
                envelope.setResponse(doListen(consumerUrl, segmentHash, greatestRevisionKnown, language, voice));
                envelope.setRevision(greatestRevisionKnown);
            } else {
                throw mwException;
            }
        }
        return envelope;
    }

    private ListenResponse doListen(String consumerUrl, String segmentHash, long revision, String language, String voice) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(wikispeechBaseUrl + "/api.php").newBuilder();
        urlBuilder.addQueryParameter("action", "wikispeech-listen");
        urlBuilder.addQueryParameter("consumer-url", consumerUrl);
        urlBuilder.addQueryParameter("format", "json");
        urlBuilder.addQueryParameter("skip-journal-metrics", String.valueOf(skipJournalMetrics));
        urlBuilder.addQueryParameter("lang", language);
        if (voice != null) {
            urlBuilder.addQueryParameter("voice", voice);
        }
        urlBuilder.addQueryParameter("revision", String.valueOf(revision));
        urlBuilder.addQueryParameter("segment", segmentHash);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();
        final ByteArrayOutputStream baos;
        try {
            if (response.code() != 200) {
                throw new IOException("Response" + response);
            }

            baos = new ByteArrayOutputStream(49152);
            IOUtils.copy(response.body().byteStream(), baos);
        } finally {
            response.close();
        }

        JsonNode json = objectMapper.readTree(baos.toByteArray());
        if (json.has("error")) {
            if (json.get("error").has("errorclass")) {
                throw new MWException(json);
            }
            throw new IOException("Wikispeech responded with an error!" + objectMapper.writeValueAsString(json));
        }
        ListenResponse listenResponse = objectMapper.convertValue(json.get("wikispeech-listen"), ListenResponse.class);
        if (listenResponse == null) {
            throw new RuntimeException("Failed to deserialize JSON response!" + objectMapper.writeValueAsString(json));
        }
        return listenResponse;
    }

    @Data
    public static class ListenResponseEnvelope {
        private ListenResponse response;
        private long revision;
    }

    @Data
    public static class ListenResponse {
        private byte[] audio;
        private List<Token> tokens;
    }

    @Data
    public static class Token {
        private int endtime;
        private String orth;
        private String expanded;
    }

    @Data
    public static class Segment {
        private SegmentContent[] content;
        private int startOffset;
        private int endOffset;
        private String hash;
    }

    @Data
    public static class SegmentContent {
        private String path;
        private String string;
    }

}

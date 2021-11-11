package se.wikimedia.wikispeech.prerender.mediawiki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.Getter;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.OkHttpUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WikispeechApi {

    private Logger log = LogManager.getLogger();

    private boolean skipJournalMetrics = true;
    private String wikispeechBaseUrl = "https://wikispeech.wikimedia.se/w";

    private ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient client;

    public void open() {
        client = OkHttpUtil.clientFactory();

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
     *
     * @param consumerUrl
     * @param title
     * @return null if page does not exist.
     * @throws IOException
     */
    public Long getCurrentRevision(String consumerUrl, String title) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(consumerUrl + "/api.php").newBuilder();
        urlBuilder.addQueryParameter("action", "query");
        urlBuilder.addQueryParameter("prop", "info");
        urlBuilder.addQueryParameter("format", "json");
        urlBuilder.addQueryParameter("titles", title);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.code() != 200) {
            throw new IOException("Response" + response);
        }

        JsonNode json = objectMapper.readTree(response.body().byteStream());

        JsonNode pages = json.get("query").get("pages");
        if (pages.has("-1")) {
            return null;
        }
        JsonNode page = pages.get(pages.fieldNames().next());
        return page.get("lastrevid").longValue();
    }

    public static class MWException extends IOException {

        @Getter
        private JsonNode error;

        public MWException(JsonNode error) {
            this.error = error;
        }

        public String getExceptionClass() {
            return error.get("error").get("errorclass").textValue();
        }
    }

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

    public ListenResponseEnvelope listen(String consumerUrl, String title, String segmentHash, long lastKnownRevision, String lang) throws IOException {
        ListenResponseEnvelope envelope = new ListenResponseEnvelope();
        long greatestRevisionKnown = getGreatestRevisionKnown(consumerUrl, title, lastKnownRevision);
        try {
            envelope.setResponse(doListen(consumerUrl, segmentHash, greatestRevisionKnown, lang));
            envelope.setRevision(lastKnownRevision);
        } catch (MWException mwException) {
            if ("MediaWiki\\Wikispeech\\Segment\\OutdatedOrInvalidRevisionException".equals(mwException.getExceptionClass())) {
                greatestRevisionKnown = getCurrentRevision(consumerUrl, title);
                setGreatestRevisionKnown(consumerUrl, title, greatestRevisionKnown);
                envelope.setResponse(doListen(consumerUrl, segmentHash, greatestRevisionKnown, lang));
                envelope.setRevision(greatestRevisionKnown);
            } else {
                throw mwException;
            }
        }
        return envelope;
    }

    private ListenResponse doListen(String consumerUrl, String segmentHash, long revision, String lang) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(wikispeechBaseUrl + "/api.php").newBuilder();
        urlBuilder.addQueryParameter("action", "wikispeech-listen");
        urlBuilder.addQueryParameter("consumer-url", consumerUrl);
        urlBuilder.addQueryParameter("format", "json");
        urlBuilder.addQueryParameter("skip-journal-metrics", String.valueOf(skipJournalMetrics));
        urlBuilder.addQueryParameter("lang", lang);
        urlBuilder.addQueryParameter("revision", String.valueOf(revision));
        urlBuilder.addQueryParameter("segment", segmentHash);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.code() != 200) {
            throw new IOException("Response" + response);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(49152);
        IOUtils.copy(response.body().byteStream(), baos);
        try {
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
        } catch (Exception exception) {
            log.error("Failed processing response: {}", new String(baos.toByteArray()), exception);
            throw exception;
        }
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

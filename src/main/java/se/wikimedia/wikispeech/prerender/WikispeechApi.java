package se.wikimedia.wikispeech.prerender;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WikispeechApi {

    private Logger log = LogManager.getLogger();

    private boolean skipJournalMetrics = true;
    private String wikispeechBaseUrl = "https://wikispeech.wikimedia.se/w";

    private ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient client;

    public void open() {
        client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .addInterceptor(
                        new Interceptor() {
                            @NotNull
                            @Override
                            public Response intercept(@NotNull Chain chain) throws IOException {
                                Request originalRequest = chain.request();
                                Request requestWithUserAgent = originalRequest
                                        .newBuilder()
                                        .header("Content-Type", "application/json")
                                        .header("User-Agent", "WMSE Wikispeech API Java client")
                                        .build();

                                return chain.proceed(requestWithUserAgent);
                            }
                        })
                .build();

    }

    public List<Segment> segment(String consumerUrl, String page) throws IOException {
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
        return objectMapper.convertValue(json.get("wikispeech-segment").get("segments"), new TypeReference<List<Segment>>() {
        });
    }

    public long getCurrentRevision(String consumerUrl, String title) throws IOException {
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
        JsonNode page = pages.get(pages.fieldNames().next());
        return page.get("lastrevid").longValue();
    }

    public ListenResponse listen(String consumerUrl, String segmentHash, long revision, String lang) throws IOException {
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
                throw new RuntimeException("Wikispeech responded with an error!" + objectMapper.writeValueAsString(json));
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

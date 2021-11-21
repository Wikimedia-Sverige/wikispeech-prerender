package se.wikimedia.wikispeech.prerender.mediawiki;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class PageApi {

    private final Logger log = LogManager.getLogger(getClass());

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public PageInfo getPageInfo(String consumerUrl, String title) throws IOException {
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
        try {
            return objectMapper.convertValue(pages.get(pages.fieldNames().next()), PageInfo.class);
        } catch (Exception e) {
            log.error("Failed to deserialize PageInfo from {}", pages, e);
            throw e;
        }
    }

    public okhttp3.Headers getHttpHeaders(String consumerUrl, String title) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(consumerUrl).newBuilder();
        urlBuilder.addQueryParameter("title", title);

        Request request = new Request.Builder()
                .head()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.code() != 200) {
            throw new IOException("Response" + response);
        }

        return response.headers();
    }

    public PageApi(
            @Autowired OkHttpClient client
    ) {
        this.client = client;
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Data
    public static class PageInfo {
        @JsonProperty(value = "pageid")
        private Long pageIdentity;
        @JsonProperty(value = "ns")
        private Integer namespaceIdentity;
        @JsonProperty(value = "title")
        private String title;
        @JsonProperty(value = "contentmodel")
        private String contentModel;
        @JsonProperty(value = "pagelanguage")
        private String pageLanguage;
        @JsonProperty(value = "pagelanguagehtmlcode")
        private String pageLanguageHtmlCode;
        @JsonProperty(value = "pagelanguagedir")
        private String pageLanguageDirectory;
        @JsonProperty(value = "touched")
        private OffsetDateTime touched;
        @JsonProperty(value = "lastrevid")
        private Long lastRevisionIdentity;
        @JsonProperty(value = "length")
        private Integer length;
        @JsonProperty(value = "redirect")
        private String redirect;
        @JsonProperty(value = "new")
        private String _new;
    }

}

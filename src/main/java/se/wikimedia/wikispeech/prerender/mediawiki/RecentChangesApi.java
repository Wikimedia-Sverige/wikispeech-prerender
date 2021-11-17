package se.wikimedia.wikispeech.prerender.mediawiki;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.wikimedia.wikispeech.prerender.Collector;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

@Component
public class RecentChangesApi {

    private final Logger log = LogManager.getLogger(getClass());

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public RecentChangesApi(
            @Autowired OkHttpClient client
    ) {
        this.client = client;
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public void get(
            String wikiBaseUrl,
            int namespace,
            OffsetDateTime fromTimestamp,
            Collector<Item> collector
    ) throws IOException {
        get(wikiBaseUrl, List.of(namespace), fromTimestamp, collector);
    }

    public void get(
            String wikiBaseUrl,
            List<Integer> namespaces,
            OffsetDateTime fromTimestamp,
            Collector<Item> collector
    ) throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(wikiBaseUrl + "/api.php").newBuilder();
        urlBuilder.addQueryParameter("action", "query");
        urlBuilder.addQueryParameter("list", "recentchanges");
        urlBuilder.addQueryParameter("rcdir", "newer");
        if (fromTimestamp != null) {
            urlBuilder.addQueryParameter("rcstart", fromTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (namespaces != null && !namespaces.isEmpty()) {
            StringBuilder values = new StringBuilder(namespaces.size() * 4);
            for (Iterator<Integer> iterator = namespaces.iterator(); iterator.hasNext(); ) {
                int namespace = iterator.next();
                values.append(namespace);
                if (iterator.hasNext()) {
                    values.append("|");
                }
            }
            urlBuilder.addQueryParameter("rcnamespace", values.toString());
        }
        urlBuilder.addQueryParameter("rclimit", "500");
        urlBuilder.addQueryParameter("format", "json");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.code() != 200) {
            throw new IOException("Response" + response);
        }

        JsonNode json = objectMapper.readTree(response.body().byteStream());
        ArrayNode array = (ArrayNode) json.get("query").get("recentchanges");
        for (int i = 0; i < array.size(); i++) {
            Item item;
            try {
                item = objectMapper.convertValue(array.get(i), Item.class);
            } catch (Exception e) {
                log.error("Failed to deserialize item from {}", array.get(i), e);
                throw e;
            }
            if (!collector.collect(item)) {
                break;
            }
        }

    }

    @Data
    public static class Item {
        @JsonProperty("type")
        private ItemType type;
        @JsonProperty("ns")
        private int namespaceIdentity;
        private String title;
        @JsonProperty("pageid")
        private int pageIdentity;
        @JsonProperty("revid")
        private int revisionIdentity;
        @JsonProperty("old_revid")
        private int oldRevisionIdentity;
        @JsonProperty("rcid")
        private int recentChangesIdentity;
        @JsonProperty("timestamp")
        private OffsetDateTime timestamp;
        @JsonProperty("actionhidden")
        private String actionhidden;
    }

    public static enum ItemType {
        @JsonProperty("new")
        created,
        @JsonProperty("edit")
        edit,
        @JsonProperty("log")
        log
    }

}

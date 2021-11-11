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
import se.wikimedia.wikispeech.prerender.Collector;
import se.wikimedia.wikispeech.prerender.OkHttpUtil;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RecentChangesApi {

    public static void main(String[] args) throws Exception {
        RecentChangesApi api = new RecentChangesApi();
        api.open();
        api.get("https://sv.wikipedia.org/w", 0, null, new Collector<Item>() {
            @Override
            public boolean collect(Item collected) {
                return true;
            }
        });
    }

    private Logger log = LogManager.getLogger();

    private OkHttpClient client;
    private ObjectMapper objectMapper;

    public void open() {
        client = OkHttpUtil.clientFactory();
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

        HttpUrl.Builder urlBuilder = HttpUrl.parse(wikiBaseUrl + "/api.php").newBuilder();
        urlBuilder.addQueryParameter("action", "query");
        urlBuilder.addQueryParameter("list", "recentchanges");
        urlBuilder.addQueryParameter("rcdir", "newer");
        if (fromTimestamp != null) {
            urlBuilder.addQueryParameter("rcend", fromTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        urlBuilder.addQueryParameter("rcnamespace", String.valueOf(namespace));
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
            if (!collector.collect(objectMapper.convertValue(array.get(i), Item.class))) {
                break;
            }
        }

    }

    @Data
    public static class Item {
        @JsonProperty("type")
        private String type;
        @JsonProperty("ns")
        private int ns;
        private String title;
        @JsonProperty("pageid")
        private int pageid;
        @JsonProperty("revid")
        private int revid;
        @JsonProperty("old_revid")
        private int old_revid;
        @JsonProperty("rcid")
        private int rcid;
        @JsonProperty("timestamp")
        private OffsetDateTime timestamp;
    }

}

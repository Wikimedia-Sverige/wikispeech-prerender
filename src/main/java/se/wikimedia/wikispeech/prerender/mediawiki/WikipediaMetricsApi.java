package se.wikimedia.wikispeech.prerender.mediawiki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class WikipediaMetricsApi {

    public static void main(String[] args) throws Exception {
        WikipediaMetricsApi api = new WikipediaMetricsApi(new OkHttpClient());
        WikipediaMetricsApi.PageViews pageViews = api.getPageViewsTop("sv.wikipedia", LocalDate.parse("2023-03-14"));
        System.currentTimeMillis();

    }

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public WikipediaMetricsApi(
            OkHttpClient client
    ) {
        this.client = client;
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    }

    private static final DateTimeFormatter pathSuffixDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public PageViews getPageViewsTop(String wiki, LocalDate date) throws IOException {
        // /sv.wikipedia/all-access/2023/03/27
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://wikimedia.org/api/rest_v1/metrics/pageviews/top/" + wiki + "/all-access/" + date.format(pathSuffixDateFormatter)).newBuilder();

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        JsonNode json;
        try {

            if (response.code() == 404)
                // occurs if there is no data for this date, e.g. a future date seen from the tz of the remote server
                return null;

            if (response.code() != 200) {
                throw new IOException("Response" + response);
            }

            json = objectMapper.readTree(response.body().byteStream());
        } finally {
            response.close();
        }

        return objectMapper.convertValue(json.get("items").get(0), PageViews.class);
    }

    @Data
    public static class PageViews {
        private String project;
        private String access;
        private String year;
        private String month;
        private String day;
        private List<PageViewArticle> articles;
    }

    @Data
    public static class PageViewArticle {
        private String article;
        private int views;
        private int rank;

    }

}

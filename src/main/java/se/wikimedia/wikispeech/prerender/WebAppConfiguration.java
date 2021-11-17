package se.wikimedia.wikispeech.prerender;


import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = "se.wikimedia.wikispeech.prerender")
public class WebAppConfiguration {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
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

}

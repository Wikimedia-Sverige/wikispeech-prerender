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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import se.wikimedia.wikispeech.prerender.service.Settings;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = "se.wikimedia.wikispeech.prerender")
public class WebAppConfiguration implements SchedulingConfigurer {

    @Bean
    public OkHttpClient okHttpClient(Settings settings) {
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
                                        .header("User-Agent", settings.getString("WebAppConfiguration.userAgent", "WMSE Wikispeech Prerender"))
                                        .build();

                                return chain.proceed(requestWithUserAgent);
                            }
                        })
                .build();
    }

    @Bean
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(10);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

}

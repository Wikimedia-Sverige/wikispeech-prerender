package se.wikimedia.wikispeech.prerender;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {

    public static OkHttpClient clientFactory() {
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

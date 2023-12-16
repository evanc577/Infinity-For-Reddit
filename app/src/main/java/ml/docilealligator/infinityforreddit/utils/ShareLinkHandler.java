package ml.docilealligator.infinityforreddit.utils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

public class ShareLinkHandler {
    private final Retrofit retrofit;

    @Inject
    public ShareLinkHandler(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    private String fetchRedirectUrl(String urlString) throws IOException {
        final OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        okhttp3.Call call = retrofit.newBuilder()
                .client(client)
                .build()
                .callFactory()
                .newCall(new Request.Builder()
                        .url(urlString)
                        .build());
        try (okhttp3.Response response = call.execute()) {
            return response.request().url().toString();
        }
    }

    public CompletableFuture<String> handleUrlResolve(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchRedirectUrl(urlString);
            } catch (IOException e) {
                return null;
            }
        });
    }
}
package ml.docilealligator.infinityforreddit;

import android.content.SharedPreferences;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ml.docilealligator.infinityforreddit.apis.StreamableAPI;
import ml.docilealligator.infinityforreddit.network.AccessTokenAuthenticator;
import ml.docilealligator.infinityforreddit.network.RedgifsAccessTokenAuthenticator;
import ml.docilealligator.infinityforreddit.network.ServerAccessTokenAuthenticator;
import ml.docilealligator.infinityforreddit.network.SortTypeConverterFactory;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.guava.GuavaCallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@Module(includes = AppModule.class)
abstract class NetworkModule {

    @Provides
    @Named("base")
    @Singleton
    static OkHttpClient provideBaseOkhttp(@Named("proxy") SharedPreferences mProxySharedPreferences) {
        boolean proxyEnabled = mProxySharedPreferences.getBoolean(SharedPreferencesUtils.PROXY_ENABLED, false);
        Proxy.Type proxyType = Proxy.Type.valueOf(mProxySharedPreferences.getString(SharedPreferencesUtils.PROXY_TYPE, "HTTP"));
        String proxyHost = mProxySharedPreferences.getString(SharedPreferencesUtils.PROXY_HOSTNAME, "127.0.0.1");
        int proxyPort = Integer.parseInt(mProxySharedPreferences.getString(SharedPreferencesUtils.PROXY_PORT, "1080"));

        var builder =  new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);

        if (proxyEnabled) {
            InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
            Proxy proxy = new Proxy(proxyType, proxyAddr);
            builder.proxy(proxy);
        }

        return builder.build();
    }

    @Provides
    @Named("base")
    @Singleton
    static Retrofit provideBaseRetrofit(@Named("base") OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(APIUtils.API_BASE_URI)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(SortTypeConverterFactory.create())
                .addCallAdapterFactory(GuavaCallAdapterFactory.create())
                .build();
    }

    @Provides
    static ConnectionPool provideConnectionPool() {
        return new ConnectionPool(0, 1, TimeUnit.NANOSECONDS);
    }

    @Provides
    @Named("no_oauth")
    static Retrofit provideRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit;
    }

    @Provides
    @Named("oauth")
    static Retrofit provideOAuthRetrofit(@Named("base") Retrofit retrofit,
                                         @Named("default") OkHttpClient okHttpClient) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.OAUTH_API_BASE_URI)
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Named("default")
    @Singleton
    static OkHttpClient provideOkHttpClient(@Named("base") OkHttpClient httpClient,
                                            @Named("base") Retrofit retrofit,
                                            RedditDataRoomDatabase redditDataRoomDatabase,
                                            @Named("current_account") SharedPreferences currentAccountSharedPreferences,
                                            ConnectionPool connectionPool) {
        return httpClient.newBuilder()
                .authenticator(new AccessTokenAuthenticator(retrofit, redditDataRoomDatabase, currentAccountSharedPreferences))
                .connectionPool(connectionPool)
                .build();
    }

    @Provides
    @Named("server")
    @Singleton
    static OkHttpClient provideServerOkHttpClient(@Named("base") OkHttpClient httpClient,
                                            RedditDataRoomDatabase redditDataRoomDatabase,
                                            @Named("current_account") SharedPreferences currentAccountSharedPreferences,
                                            ConnectionPool connectionPool) {
        return httpClient.newBuilder()
                .authenticator(new ServerAccessTokenAuthenticator(redditDataRoomDatabase, currentAccountSharedPreferences))
                .connectionPool(connectionPool)
                .build();
    }

    @Provides
    @Named("oauth_without_authenticator")
    @Singleton
    static Retrofit provideOauthWithoutAuthenticatorRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.OAUTH_API_BASE_URI)
                .build();
    }

    @Provides
    @Named("upload_media")
    @Singleton
    static Retrofit provideUploadMediaRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.API_UPLOAD_MEDIA_URI)
                .build();
    }

    @Provides
    @Named("upload_video")
    @Singleton
    static Retrofit provideUploadVideoRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.API_UPLOAD_VIDEO_URI)
                .build();
    }

    @Provides
    @Named("download_media")
    @Singleton
    static Retrofit provideDownloadRedditVideoRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl("http://localhost/")
                .build();
    }

    @Provides
    @Named("RedgifsAccessTokenAuthenticator")
    static Interceptor redgifsAccessTokenAuthenticator(@Named("current_account") SharedPreferences currentAccountSharedPreferences) {
        return new RedgifsAccessTokenAuthenticator(currentAccountSharedPreferences);
    }

    @Provides
    @Named("redgifs")
    @Singleton
    static Retrofit provideRedgifsRetrofit(@Named("RedgifsAccessTokenAuthenticator") Interceptor accessTokenAuthenticator,
                                           @Named("base") OkHttpClient httpClient,
                                           @Named("base") Retrofit retrofit,
                                           ConnectionPool connectionPool) {
        OkHttpClient.Builder okHttpClientBuilder = httpClient.newBuilder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request()
                                .newBuilder()
                                .header("User-Agent", APIUtils.getUserAgent())
                                .build()
                ))
                .addInterceptor(accessTokenAuthenticator)
                .connectionPool(connectionPool);

        return retrofit.newBuilder()
                .baseUrl(APIUtils.REDGIFS_API_BASE_URI)
                .client(okHttpClientBuilder.build())
                .build();
    }

    @Provides
    @Named("imgur")
    @Singleton
    static Retrofit provideImgurRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.IMGUR_API_BASE_URI)
                .build();
    }

    @Provides
    @Named("vReddIt")
    @Singleton
    static Retrofit provideVReddItRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl("http://localhost/")
                .build();
    }

    @Provides
    @Named("streamable")
    @Singleton
    static Retrofit provideStreamableRetrofit(@Named("base") Retrofit retrofit) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.STREAMABLE_API_BASE_URI)
                .build();
    }

    @Provides
    @Named("online_custom_themes")
    @Singleton
    static Retrofit provideOnlineCustomThemesRetrofit(@Named("base") Retrofit retrofit, @Named("server") OkHttpClient httpClient) {
        return retrofit.newBuilder()
                .baseUrl(APIUtils.SERVER_API_BASE_URI)
                .client(httpClient)
                .build();
    }

    @Provides
    @Singleton
    static StreamableAPI provideStreamableApi(@Named("streamable") Retrofit streamableRetrofit) {
        return streamableRetrofit.create(StreamableAPI.class);
    }
}

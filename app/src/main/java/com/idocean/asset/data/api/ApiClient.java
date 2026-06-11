package com.idocean.asset.data.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.idocean.asset.config.AppConfig;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static Retrofit retrofit;
    private static String resolvedBaseUrl;

    private ApiClient() {
    }

    public static synchronized AssetApiService getAssetApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            Interceptor apiKeyInterceptor = chain -> {
                Request.Builder builder = chain.request().newBuilder();
                if (AppConfig.hasApiKey()) {
                    builder.addHeader("x-api-key", AppConfig.API_KEY);
                }
                return chain.proceed(builder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(apiKeyInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build();

            Gson gson = new GsonBuilder().serializeNulls().create();
            resolvedBaseUrl = AppConfig.BASE_URL;

            retrofit = new Retrofit.Builder()
                    .baseUrl(resolvedBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(AssetApiService.class);
    }

    public static synchronized String getResolvedBaseUrl() {
        if (resolvedBaseUrl == null || resolvedBaseUrl.trim().isEmpty()) {
            resolvedBaseUrl = AppConfig.BASE_URL;
        }
        return resolvedBaseUrl;
    }
}

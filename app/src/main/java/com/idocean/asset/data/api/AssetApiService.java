package com.idocean.asset.data.api;

import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public interface AssetApiService {
    @GET("get-db")
    Call<ResponseBody> getAssets();

    @GET("get-db")
    Call<ResponseBody> getAssets(@QueryMap Map<String, String> queryMap);

    @GET
    Call<ResponseBody> getAssets(@Url String url);

    @POST("update-asset")
    Call<ResponseBody> updateAsset(@Body JsonObject requestDto);

    @POST("checkout-asset")
    Call<ResponseBody> checkoutAsset(@Body JsonObject requestDto);

    @POST
    Call<ResponseBody> checkinAssets(@Url String url, @Body JsonObject requestDto);
}

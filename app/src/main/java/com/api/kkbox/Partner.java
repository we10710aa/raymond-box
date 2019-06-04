package com.api.kkbox;


import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

import static android.content.Context.MODE_PRIVATE;

public class Partner {
    private static Partner partnerInstance;
    private PartnerApi partnerApi;


    public interface PartnerApi {
        String BASE_URL = "https://api.kkbox.com/v1.1/";
        @GET("me")
        Call<JsonObject> getMe(@Header("Authorization")String accessToken);
        @GET("tracks/{trackID}?territory=TW")
        Call<JsonObject> getTrackInfo(@Path("trackID")String trackID,@Header("Authorization")String accessToken);
    }
    Partner(){

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PartnerApi.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        partnerApi = retrofit.create(PartnerApi.class);

    }

    public static Partner getInstance(){
        if(partnerInstance==null){
            partnerInstance = new Partner();
        }
        return partnerInstance;
    }

    public PartnerApi getPartnerApi(){ return this.partnerApi; }

    public static String parseUserImageUrl(JsonObject response){
        String url = response.getAsJsonArray("images")
                .get(1).getAsJsonObject().get("url").getAsString();
        return url;
    }
    public static String parseUserID(JsonObject response){
        String id = response.get("id").getAsString();
        return id;
    }
    public static String parseTrackImageUrl(JsonObject response){
        String url = response.getAsJsonObject("album")
                .getAsJsonArray("images")
                .get(1).getAsJsonObject()
                .get("url").getAsString();
        return url;
    }
    public static String parseUserName(JsonObject response){
        String name = response.get("name").getAsString();
        return name;
    }
}
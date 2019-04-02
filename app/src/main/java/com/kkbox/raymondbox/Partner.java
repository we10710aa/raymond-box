package com.kkbox.raymondbox;


import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public class Partner {
    public interface PartnerApi {
        String BASE_URL = "https://api.kkbox.com/v1.1/";
        @GET("me")
        Call<JsonObject> getMe(@Header("authorization") String token);
        @GET("tracks/{trackID}?territory=TW")
        Call<JsonObject> getTrackInfo(@Path("trackID")String trackID,
                                      @Header("authorization") String token);


    }
    public static PartnerApi getInstance(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PartnerApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PartnerApi api = retrofit.create(PartnerApi.class);
        return api;
    }
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
}

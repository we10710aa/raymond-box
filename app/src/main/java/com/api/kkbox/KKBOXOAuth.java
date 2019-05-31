package com.api.kkbox;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class KKBOXOAuth {
    private static KKBOXOAuth instance;
    private KKBOXOAuthApi kkboxOAuthApi;

    public interface KKBOXOAuthApi {
        String BASE_URL ="https://account.kkbox.com/";
        String DEVICE_FLOW = "http://oauth.net/grant_type/device/1.0";
        String REFRESH_TOKEN = "refresh_token";
        String DEFAULT_SCOPE = "user_profile user_territory user_account_status";

        @POST("oauth2/device/code")
        @FormUrlEncoded
        @Headers({"Content-Type: application/x-www-form-urlencoded"})
        Call<JsonObject> getDeviceCodeApiCall(@Field("client_id")String clientId,
                                              @Field("scope")String scope);

        @POST("oauth2/token")
        @FormUrlEncoded
        @Headers({"Content-Type: application/x-www-form-urlencoded"})
        Call<JsonObject> getTokenApiCall(@Field("grant_type")String grantType,
                                         @Field("client_id")String clientID,
                                         @Field("client_secret")String clientSecret,
                                         @Field("code")String deviceCode);

        @GET("oauth2/tokeninfo")
        Call<JsonObject> getTokenIntrospectionApiCall(@Query("access_token")String token);
    }
    private KKBOXOAuth(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KKBOXOAuthApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        kkboxOAuthApi = retrofit.create(KKBOXOAuthApi.class);
    }
    public static KKBOXOAuth getInstance(){
        if(instance==null){
            instance = new KKBOXOAuth();
        }
        return  instance;
    }

    public KKBOXOAuthApi getKKBOXOauthApi(){
        return this.kkboxOAuthApi;
    }
}

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

        @POST("oauth2/token")
        @FormUrlEncoded
        @Headers({"Content-Type: application/x-www-form-urlencoded"})
        Call<JsonObject> getTokenRefreshCall(@Field("grant_type")String grantType,
                                              @Field("refresh_token")String refreshToken,
                                              @Field("client_id")String clientID,
                                              @Field("client_secret")String clientSecret);

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

    public static String parseDeviceCode(JsonObject response){
        if(response.has("device_code")){
            return response.get("device_code").getAsString();
        }
        return null;
    }

    public static String parseDeviceVerificationUrl(JsonObject response){
        if(response.has("verification_qrcode")){
            return response.get("verification_qrcode").getAsString();
        }
        return null;
    }

    public static String parseAccessToken(JsonObject response){
        if(response.has("access_token")){
            return response.get("access_token").getAsString();
        }
        return null;
    }

    public static String parseExpiresIn(JsonObject response){
        if(response.has("expires_in")){
            return response.get("expires_in").getAsString();
        }
        return null;
    }

    public static String parseRefreshToken(JsonObject response){
        if(response.has("refresh_token")){
            return response.get("refresh_token").getAsString();
        }
        return null;
    }
}

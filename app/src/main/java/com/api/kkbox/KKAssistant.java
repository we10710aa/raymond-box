package com.api.kkbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kkbox.raymondbox.KKBrainActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static android.content.Context.MODE_PRIVATE;

public class KKAssistant {
    public static final int AUDIO_INEFFECTIVE=-1;
    public static final int AUDIO_PLAY=0;
    public static final int AUDIO_SKIP_FORWARD=1;
    public static final int AUDIO_SKIP_BACKWARD=2;
    public static final int AUDIO_PAUSE=3;
    public static final int AUDIO_RESUME=4;
    public static final int AUDIO_ERROR =-2 ;

    private static KKAssistant kkAssistantInstance;
    private KKBrainApi kkBrainApi;

    public interface KKBrainApi {
        @POST("/")
        @Headers({"Content-type: application/json"})
        Call<JsonObject> getAssistantCall(@Body RequestBody body);
        String BASE_URL = "https://nlu.assistant.kkbox.com";
    }
    private  KKAssistant(){
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KKBrainApi.BASE_URL)
                .client(client)  //get rid of this line if you don't want http logging
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        KKBrainApi api = retrofit.create(KKBrainApi.class);
        kkBrainApi = api;

    }

    public static KKAssistant getInstance(){
        if(kkAssistantInstance == null) {
            kkAssistantInstance = new KKAssistant();
        }
        return kkAssistantInstance;
    }

    public KKBrainApi getKkBrainApi(){
        return  this.kkBrainApi;
    }

    public static RequestBody getRequestBody(final String key,final String userID,final String accessToken){
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("version","1.0");
        map.put("context",
                new LinkedHashMap<String,Object>(){{
                    put("System",new LinkedHashMap<String,Object>(){{
                        put("user",new LinkedHashMap<String,String>(){{
                            put("userId",userID);
                            put("accessToken",accessToken);
                        }});
                    }});
                }});
        map.put("request",new LinkedHashMap<String,Object>(){{
            put("type","DirectiveRequest");
            put("requestId",userID);
            put("timestamp", Calendar.getInstance().getTime().toString());
            put("asr",new LinkedHashMap<String,String>(){{
                put("text",key);
            }});
        }});
        JSONObject json = new JSONObject(map);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("Content-type: application/json"), json.toString());
        return requestBody;
    }
    public static int parsePlayType(JsonObject response){
        if(response.getAsJsonObject("response").getAsJsonArray("directives").size()==0){
            return KKAssistant.AUDIO_INEFFECTIVE;
        }
        String type =response.getAsJsonObject("response")
                .getAsJsonArray("directives")
                .get(0).getAsJsonObject()
                .get("type").getAsString();
        if(type.equals("AudioPlayer.Play"))
            return KKAssistant.AUDIO_PLAY;
        else if (type.equals("AudioPlayer.SkipForward"))
            return KKAssistant.AUDIO_SKIP_FORWARD;
        else if( type.equals("AudioPlayer.SkipBackward"))
            return KKAssistant.AUDIO_SKIP_BACKWARD;
        else if (type.equals("AudioPlayer.Pause"))
            return KKAssistant.AUDIO_PAUSE;
        else if (type.equals("AudioPlayer.Resume"))
            return KKAssistant.AUDIO_RESUME;
        else
            return KKAssistant.AUDIO_ERROR;
    }
    public static ArrayList<String> parsePlayList(JsonObject response){
        ArrayList<String> playlist = new ArrayList<>();
        JsonArray responseList = response.getAsJsonObject("response")
                .getAsJsonArray("directives")
                .get(0).getAsJsonObject()
                .getAsJsonObject("playlist")
                .getAsJsonArray("data");
        for (JsonElement element : responseList) {
            playlist.add(element.getAsJsonObject().get("id").getAsString());
        }
        return playlist;
    }
    public static String parseShowText(JsonObject response){
        if(response.getAsJsonObject("response").has("outputSpeech")){
            String text = response.getAsJsonObject("response")
                    .getAsJsonObject("outputSpeech")
                    .get("text").getAsString();
            return text;
        }
        return null;
    }

}


package com.kkbox.raymondbox;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

class KKAssistant {
    public static final int AUDIO_INEFFECTIVE=-1;
    public static final int AUDIO_PLAY=0;
    public static final int AUDIO_SKIP_FORWARD=1;
    public static final int AUDIO_SKIP_BACKWARD=2;
    public static final int AUDIO_PAUSE=3;
    public static final int AUDIO_RESUME=4;
    public static final int AUDIO_ERROR =-2 ;

    public interface KKAssistantApi{
        @POST("/")
        @Headers({"Content-type: application/json"})
        Call<JsonObject> assistant(@Body RequestBody body);
        String BASE_URL = "https://nlu.assistant.kkbox.com";
    }
    public static KKAssistantApi getInstance(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KKAssistantApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        KKAssistantApi api = retrofit.create(KKAssistantApi.class);
        return api;
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
            put("requestId","08ad060515cbb52836954fae5dc2f90z");
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


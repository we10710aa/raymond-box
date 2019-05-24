package com.kkbox.raymondbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.api.kkbox.KKAssistant;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KKBrainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kkbrain);
        ((TextView)findViewById(R.id.textview_kkbrain_body)).setText("{\n" +
                "   \"version\":\"1.0\",\n" +
                "   \"context\":{\n" +
                "      \"System\":{\n" +
                "         \"user\":{\n" +
                "            \"userId\":\"USER_ID\",\n" +
                "            \"accessToken\":\"ACCESS_TOKEN\"\n" +
                "         }\n" +
                "      }\n" +
                "   },\n" +
                "   \"request\":{\n" +
                "      \"type\":\"DirectiveRequest\",\n" +
                "      \"requestId\":\"08ad060515cbb52836954fae5dc2f9c2\",\n" +
                "      \"timestamp\":\"Fri May 24 11:39:38 GMT+08:00 2019\",\n" +
                "      \"asr\":{\n" +
                "         \"text\":\"歌曲 Intent\"\n" +
                "      }\n" +
                "   }\n" +
                "}");
    }
    public void sendRequest(View view){
        String query = ((EditText)findViewById(R.id.editText_kkbrain_intent)).getText().toString();
        final Call<JsonObject> request =  KKAssistant.getInstance(this).ask(query);
        final long startTime = System.nanoTime();
        request.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                long endTime = System.nanoTime();
                StringBuilder builder = new StringBuilder();
                builder.append("response time(ms):"+(endTime - startTime) / 1000000+"\n");
                builder.append("response show text:");
                builder.append(KKAssistant.parseShowText(response.body())+"\n");
                builder.append("response SongIDs:\n");
                if(KKAssistant.parsePlayType(response.body())!=KKAssistant.AUDIO_INEFFECTIVE) {
                    List<String> playlist = KKAssistant.parsePlayList(response.body());
                    for (String song : playlist) {
                        builder.append(song + "\n");
                    }
                }
                ((TextView)findViewById(R.id.textview_kkbrain_response)).setText(
                        builder.toString()
                );

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }
}

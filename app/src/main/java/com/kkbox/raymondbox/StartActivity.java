package com.kkbox.raymondbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.api.kkbox.Partner;
import com.google.gson.JsonObject;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartActivity extends AppCompatActivity {
    private Button startButton;
    private TextView startTextView;
    private ProgressBar startProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        startButton = findViewById(R.id.btn_StartActivity);
        startProgressBar = findViewById(R.id.progressBar_StartActivity);
        startTextView = findViewById(R.id.textView_StartActivity);
        startButton.setEnabled(false);
        final SharedPreferences preferences = getSharedPreferences("USER",MODE_PRIVATE);

        if(preferences.contains("access_token")){
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartActivity.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivityIfNeeded(intent,5);
                }
            });
            Call<JsonObject> call = Partner.getInstance(this).getPartnerApi().getMe();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    String userID = Partner.parseUserID(response.body());
                    preferences.edit().putString("id",userID).apply();
                    String userName = Partner.parseUserName(response.body());
                    startTextView.setText("Welcome, "+userName);
                    startProgressBar.setVisibility(View.INVISIBLE);
                    startTextView.setVisibility(View.VISIBLE);
                    startButton.setEnabled(true);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d("StartActivity",t.getMessage());
                }
            });
        }
        else{
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartActivity.this,LoginActivity.class);
                    startActivity(intent);
                }
            });
            startProgressBar.setVisibility(View.INVISIBLE);
            startTextView.setVisibility(View.VISIBLE);
            startButton.setEnabled(true);
        }
    }
}

package com.kkbox.raymondbox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.api.kkbox.Partner;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartActivity extends AppCompatActivity {
    private ConstraintLayout loggedInLayout;
    private ConstraintLayout notLogggedInLayout;
    private Button buttonLogin;
    private GridView gridViewFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        loggedInLayout = findViewById(R.id.constraintLayout_start_logged);
        notLogggedInLayout = findViewById(R.id.constrainLayout_start_notLogged);
        loggedInLayout.setVisibility(View.INVISIBLE);
        notLogggedInLayout.findViewById(R.id.btn_start_login).setVisibility(View.INVISIBLE);

        final SharedPreferences preferences = getSharedPreferences("USER",MODE_PRIVATE);
        if(preferences.contains("access_token")){
            setUserCard(preferences);
        }
        else{
            buttonLogin = findViewById(R.id.btn_start_login);
            buttonLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartActivity.this,LoginActivity.class);
                    startActivity(intent);
                }
            });
            buttonLogin.setVisibility(View.VISIBLE);
            notLogggedInLayout.findViewById(R.id.pb_start).setVisibility(View.INVISIBLE);
        }
        gridViewFunctions = findViewById(R.id.gridview_start);
        gridViewFunctions.setAdapter(new StartFunctionAdapter(this));

    }

    private void setUserCard(final SharedPreferences preferences) {
        Call<JsonObject> call = Partner.getInstance(this).getPartnerApi().getMe();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                final String userID = Partner.parseUserID(response.body());
                String userName = Partner.parseUserName(response.body());
                String url = Partner.parseUserImageUrl(response.body());
                final String token = preferences.getString("access_token",null);
                preferences.edit().putString("id",userID).apply();
                Picasso.get().load(url).into((ImageView)findViewById(R.id.img_start_avatar));
                ((TextView)findViewById(R.id.tv_start_userid)).setText("ID:"+userID);
                ((TextView)findViewById(R.id.tv_start_username)).setText(userName);
                ((TextView)findViewById(R.id.tv_start_accesstoken))
                        .setText("TOKEN:"+token);
                findViewById(R.id.btn_start_copyID).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(StartActivity.this,"copied userID",Toast.LENGTH_LONG).show();
                        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        manager.setPrimaryClip(ClipData.newPlainText("simple text",userID));
                    }
                });

                findViewById(R.id.btn_start_copytoken).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(StartActivity.this,"copied token",Toast.LENGTH_LONG).show();
                        ClipboardManager manager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        manager.setPrimaryClip(ClipData.newPlainText("simple text",token));
                    }
                });
                findViewById(R.id.btn_start_logout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        preferences.edit().clear().commit();
                        StartActivity.this.recreate();
                    }
                });
                loggedInLayout.setVisibility(View.VISIBLE);
                notLogggedInLayout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("StartActivity",t.getMessage());
            }
        });
    }

}

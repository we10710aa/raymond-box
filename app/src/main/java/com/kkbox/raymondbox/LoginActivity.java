package com.kkbox.raymondbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.api.kkbox.KKBOXOAuth;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class LoginActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private ImageView imgQrcode;
    private Handler pollingHandler=null;
    private HandlerThread pollingThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Scan QRcode to login");
        progressBar = findViewById(R.id.progress_bar);
        imgQrcode = findViewById(R.id.img_qrcode);
        final Call<JsonObject> urlCall = KKBOXOAuth.getInstance().getKKBOXOauthApi()
                .getDeviceCodeApiCall(ClientInfo.clientID, KKBOXOAuth.KKBOXOAuthApi.DEFAULT_SCOPE);
        urlCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                String url = response.body().get("verification_qrcode").getAsString();
                String deviceCode = response.body().get("device_code").getAsString();
                Log.d("LoginActivity","got url: "+url+" and device code: "+deviceCode);
                try {
                    String encodedURL = URLEncoder.encode(url,"UTF-8");
                    String qrcodeUrl =
                            String.format("https://qrcode.kkbox.com.tw/generator?content=%s" +
                                    "&image_size=%s" +
                                    "&logo_size=%s" +
                                    "&response_type=%s",encodedURL,800,200,"image");
                    Picasso.get().load(qrcodeUrl).into(imgQrcode);
                    progressBar.setVisibility(View.GONE);
                    imgQrcode.setVisibility(View.VISIBLE);
                    tokenPolling(deviceCode);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("LoginActivity",t.getMessage());
            }
        });
    }

    private void tokenPolling(String deviceCode) {
        final Call<JsonObject>tokenCall = KKBOXOAuth.getInstance().getKKBOXOauthApi().getTokenApiCall(
                KKBOXOAuth.KKBOXOAuthApi.DEVICE_FLOW,
                ClientInfo.clientID,
                ClientInfo.clientSecret,
                deviceCode
        );
        pollingThread = new HandlerThread("pollingThread");
        pollingThread.start();
        pollingHandler=new Handler(pollingThread.getLooper());
        Runnable pollingTask = new Runnable() {
            @Override
            public void run() {
                Response<JsonObject>res =null;
                try {
                    res = tokenCall.clone().execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(res == null){
                    Log.d("LoginPolling","Null Response, retry");
                    pollingHandler.postDelayed(this,3000);
                }
                else if (res.body().has("error")){
                    Log.d("LoginPolling","not yet authorized, retry");
                    pollingHandler.postDelayed(this,3000);
                }
                else if(res.code()!= 200){
                    Log.d("LoginPolling","receive error code:"+res.code());
                    pollingHandler.postDelayed(this,3000);
                }
                else{
                    SharedPreferences pref = getSharedPreferences("USER",MODE_PRIVATE);
                    pref.edit()
                            .putString("access_token",res.body().get("access_token").getAsString())
                            .putString("expires_in", res.body().get("expires_in").getAsString())
                            .putString("refresh_token",res.body().get("refresh_token").getAsString())
                            .apply();
                    Log.d("LoginPolling","login success");
                    pollingHandler.removeCallbacks(this);
                    pollingThread.quit();
                    Intent intent = new Intent(LoginActivity.this,StartActivity.class);
                    startActivity(intent);
                }
            }
        };
        pollingHandler.postDelayed(pollingTask,7000);

    }

    @Override
    protected void onPause() {
        if(pollingHandler!=null){
            pollingHandler.removeCallbacks(pollingThread);
        }
        if (pollingThread!=null){
            pollingThread.quit();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(pollingHandler!=null){
            pollingHandler.removeCallbacks(pollingThread);
        }
        if (pollingThread!=null){
            pollingThread.quit();
        }
        super.onStop();
    }
}

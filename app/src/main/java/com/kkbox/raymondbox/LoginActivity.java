package com.kkbox.raymondbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;

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
    private DevicAuthFlow devicAuthFlow;
    private Handler pollingHandler=null;
    private String deviceCode;
    private HandlerThread pollingThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Scan QRcode to login");
        progressBar = findViewById(R.id.progress_bar);
        imgQrcode = findViewById(R.id.img_qrcode);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DevicAuthFlow.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        devicAuthFlow = retrofit.create(DevicAuthFlow.class);
        final Call<JsonObject> urlCall = devicAuthFlow.getQrcode(ClientInfo.clientID,
                "user_profile user_territory user_account_status");
        urlCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                String url = response.body().get("verification_qrcode").getAsString();
                deviceCode = response.body().get("device_code").getAsString();
                Log.d("LoginActivity","got url: "+url+" and device code: "+deviceCode);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                try {
                    Bitmap bitmap = barcodeEncoder.encodeBitmap(url, BarcodeFormat.QR_CODE,800,800);
                    imgQrcode.setImageBitmap(bitmap);
                    progressBar.setVisibility(View.GONE);
                    imgQrcode.setVisibility(View.VISIBLE);
                    tokenPolling();
                } catch (WriterException e) {
                    Log.e("LoginActivity",e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("LoginActivity",t.getMessage());
            }
        });
    }

    private void tokenPolling() {
        final Call<JsonObject>tokenCall = devicAuthFlow.getToken(
                "http://oauth.net/grant_type/device/1.0",
                ClientInfo.clientID,ClientInfo.clientSecret,deviceCode);
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

    interface DevicAuthFlow{
        @POST("oauth2/device/code")
        @FormUrlEncoded
        @Headers({"Content-Type: application/x-www-form-urlencoded"})
        Call<JsonObject> getQrcode(@Field("client_id")String clientId,
                                   @Field("scope")String scope);

        @POST("oauth2/token")
        @FormUrlEncoded
        @Headers({"Content-Type: application/x-www-form-urlencoded"})
        Call<JsonObject> getToken(@Field("grant_type")String grantType,
                                  @Field("client_id")String clientID,
                                  @Field("client_secret")String clientSecret,
                                  @Field("code")String deviceCode);

        @GET("oauth2/tokeninfo")
        Call<JsonObject> tokenInfo(@Query("access_token")String token);
        String BASE_URL ="https://account.kkbox.com/";
    }
}

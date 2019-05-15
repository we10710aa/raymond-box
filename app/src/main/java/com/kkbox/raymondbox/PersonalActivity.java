package com.kkbox.raymondbox;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.api.kktix.Kktix;
import com.api.kktix.KktixRss;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonalActivity extends AppCompatActivity {


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_recommend:
                    return true;
                case R.id.nav_play:
                    Intent intent = new Intent(PersonalActivity.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivityIfNeeded(intent,5);
                    return true;
                case R.id.nav_unknown:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setSelectedItemId(R.id.nav_recommend);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        final ListView listView = findViewById(R.id.list_view);


        Call<KktixRss> call = Kktix.getInstance().getInterface().getRss("4","zh-TW");
        call.enqueue(new Callback<KktixRss>() {
            @Override
            public void onResponse(Call<KktixRss> call, Response<KktixRss> response) {
                if((response.code()!=200)||(response.body().getEntryList()==null)){
                    Log.d("PersonalActivity","retry connection");
                    call.clone().enqueue(this);
                }

                PersonalActivityAdapter adapter = new PersonalActivityAdapter(
                        PersonalActivity.this,R.layout.list_item,response.body().getEntryList());

                ((ProgressBar)findViewById(R.id.progressBar_personal)).setVisibility(View.GONE);
                listView.setAdapter(adapter);



                String date = response.body().getEntryList().get(2).getPublished();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(format.parse(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<KktixRss> call, Throwable t) {

            }
        });
    }
}

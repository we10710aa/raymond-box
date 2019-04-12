package com.kkbox.raymondbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.api.kktix.KktixRssFeed;

import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

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


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Kktix.url)
                .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(new Persister(new AnnotationStrategy())))
                .build();
        Kktix kkTixRss = retrofit.create(Kktix.class);
        Call<KktixRssFeed> call = kkTixRss.getRss("4","zh-TW");
        call.enqueue(new Callback<KktixRssFeed>() {
            @Override
            public void onResponse(Call<KktixRssFeed> call, Response<KktixRssFeed> response) {
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
            public void onFailure(Call<KktixRssFeed> call, Throwable t) {

            }
        });
    }
    private interface Kktix {
        String url = "https://kktix.com/";
        @GET("events.atom")
        Call<KktixRssFeed> getRss(@Query("category_id")String id, @Query("locale")String locale);
    }
}
package com.api.kktv;

import com.api.kktix.KktixRss;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class KKTV {
    interface KKTVApi{
            String url = "https://kktix.com/";
            @GET("events.atom")
            Call<KktixRss> getRss(@Query("category_id")String id, @Query("locale")String locale);
    }
}

package com.api.kktix;

import com.kkbox.raymondbox.PersonalActivity;

import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Kktix {
    private static Kktix kInstance = new Kktix();
    private KktixInterfface kktixInterfface;

    public interface KktixInterfface {
        String url = "https://kktix.com/";
        @GET("events.atom")
        Call<KktixRss> getRss(@Query("category_id")String id, @Query("locale")String locale);
    }

    Kktix(){
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KktixInterfface.url)
                .client(client)
                .addConverterFactory(SimpleXmlConverterFactory.createNonStrict(new Persister(new AnnotationStrategy())))
                .build();
        kktixInterfface = retrofit.create(KktixInterfface.class);
    }

    public static Kktix getInstance(){
        return kInstance;
    }

    public KktixInterfface getInterface(){
        return kktixInterfface;
    }
}

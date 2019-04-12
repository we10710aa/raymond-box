package com.api.aws;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.VoiceId;
import com.kkbox.raymondbox.MainActivity;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class PollyTextToSpeech {
    // Backend resources
    private AmazonPollyPresigningClient client;
    protected MainActivity context;

    public PollyTextToSpeech(MainActivity _context, String poolId, Regions regions) {
        CognitoCachingCredentialsProvider provider = new CognitoCachingCredentialsProvider(
                _context, poolId, regions);
        client = new AmazonPollyPresigningClient(provider);
        this.context = _context;
    }

    class SpeakAsyncTask extends AsyncTask<Void,Void,Exception>{
        String text;
        AmazonPollyPresigningClient client;
        MediaPlayer mediaPlayer;
        SpeakAsyncTask(AmazonPollyPresigningClient client,MediaPlayer mediaPlayer,String text){
            this.text = text;
            this.client = client;
            this.mediaPlayer = mediaPlayer;
        }
        @Override
        protected Exception doInBackground(Void...voids) {
            SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                    new SynthesizeSpeechPresignRequest()
                            .withText(text)
                            .withVoiceId("Zhiyu")
                            .withOutputFormat(OutputFormat.Mp3);
            String url = client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest).toString();
            try {
                mediaPlayer.setDataSource(url);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception e) {
            if(e!=null){
                Log.e("PollyTextToSpeech",e.getMessage());
            }
            else {
                mediaPlayer.prepareAsync();
            }
        }
    }

    public void pollySpeakText(MediaPlayer mediaPlayer, String text) {

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d("PollyTextToSpeech", "what=" + what + ",extra=" + extra);
                mp.release();
                return false;
            }
        });

        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
        new SpeakAsyncTask(client,mediaPlayer,text).execute();
    }

    public void shutDown() {
        this.client.shutdown();
    }

}


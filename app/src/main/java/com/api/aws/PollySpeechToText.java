package com.api.aws;

import android.media.AudioAttributes;
import android.media.MediaPlayer;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.kkbox.raymondbox.MainActivity;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class PollySpeechToText {
    // Backend resources
    private AmazonPollyPresigningClient client;
    protected MainActivity context;
    protected MediaPlayer mediaPlayer;
    public PollySpeechToText(MainActivity _context, String poolId, Regions regions){
        CognitoCachingCredentialsProvider provider = new CognitoCachingCredentialsProvider(
                _context,poolId,regions);
        client = new AmazonPollyPresigningClient(provider);
        this.context =_context;
    }
    public Future<String> getVoiceUrl(final String text){
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                        new SynthesizeSpeechPresignRequest()
                                .withText(text)
                                .withVoiceId("Zhiyu")
                                .withOutputFormat(OutputFormat.Mp3);
                return client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest).toString();
            }
        };

        return Executors.newSingleThreadExecutor().submit(callable);
    }
    public MediaPlayer getMediaPlayerInstance(String text) throws ExecutionException, InterruptedException, IOException {
        MediaPlayer  mediaPlayer= new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });

        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
        mediaPlayer.setDataSource(this.getVoiceUrl(text).get());
        return mediaPlayer;

    }
    public void shutDown(){
        this.client.shutdown();
    }

}


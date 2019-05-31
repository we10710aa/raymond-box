package com.kkbox.raymondbox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kkbox.kklinxdevicesdk.Config;
import com.kkbox.kklinxdevicesdk.KKLINXDeviceSdk;
import com.kkbox.kklinxdevicesdk.PlaybackState;

public class KKlinxActivity extends AppCompatActivity {
    private KKLINXDeviceSdk kklinx;
    private HandlerThread kklinxThread;
    private Handler kklinxThreadHandler;
    private Handler seekBarHandler;

    private ImageButton buttonPlayPause;
    private ImageButton buttonNext;
    private ImageButton buttonLast;
    private SeekBar playbackBar;
    private TextView totalTime;
    private TextView passedTime;
    private TextView songName;

    final Runnable seekBarUpdate = new Runnable() {
        @Override
        public void run() {
            int p = playbackBar.getProgress()+1000;
            if(p>playbackBar.getMax()){
                playbackBar.setProgress(playbackBar.getMax());
                passedTime.setText(convertToString(playbackBar.getMax()));
            }
            else{
                playbackBar.setProgress(p);
                passedTime.setText(convertToString(p));
            }
            seekBarHandler.postDelayed(this,1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kklinx);
        getAllView();

        Config config = new Config(
                getApplicationContext(),
                "KKLINX Demo",
                "Raymond",
                "PIXEL2 XL",
                ClientInfo.clientID,
                ClientInfo.clientSecret,
                44444,
                getFilesDir().getAbsolutePath());
        kklinx = new KKLINXDeviceSdk(config);
        kklinx.registerEventListener(new Listenter());
        kklinx.initialize();

        kklinxThread = new HandlerThread("klinxThread");
        kklinxThread.start();
        kklinxThreadHandler = new Handler(kklinxThread.getLooper());
        kklinxThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                kklinx.startMainLoop();
            }
        });

        seekBarHandler = new Handler(getMainLooper());

    }

    private void getAllView() {
        buttonPlayPause = findViewById(R.id.btn_kklinx_play);
        buttonNext = findViewById(R.id.btn_kklinx_playNext);
        buttonLast = findViewById(R.id.btn_kklinx_playLast);
        playbackBar = findViewById(R.id.seekBar_kklinx);
        totalTime = findViewById(R.id.textview_kklinx_totaltime);
        passedTime = findViewById(R.id.textview_kklinx_passedtime);
        songName = findViewById(R.id.textview_kklinx_songName);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kklinx.playNext();
            }
        });
        buttonLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kklinx.playPrevious();
            }
        });
        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kklinx.pause();
            }
        });
        playbackBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    kklinx.seek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public static String convertToString(int mseconds){
        StringBuilder builder = new StringBuilder();
        builder.append(mseconds/60000);
        builder.append(":");
        builder.append((String.format("%02d",mseconds/1000%60)));
        return builder.toString();
    }

    public void updateUI(final PlaybackState state){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int duration = state.getDuration();
                songName.setText(state.getSongName());
                playbackBar.setMax(duration);
                playbackBar.setProgress(state.getPosition());
                totalTime.setText(convertToString(duration));
                passedTime.setText(convertToString(state.getPosition()));
            }
        };
        runOnUiThread(runnable);
    }

    @Override
    protected void onStop() {
        Log.d("KKlinxActivity","onStop");
        if(kklinxThreadHandler!=null){
            kklinxThread.quit();
            kklinx.destroy();
        }
        if(seekBarHandler!=null){
            seekBarHandler.removeCallbacks(seekBarUpdate);
        }
        super.onStop();
    }

    private class Listenter implements KKLINXDeviceSdk.EventListener {

        @Override
        public void onReady() {
            kklinx.setVolume((float) 0.5);
            SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
            String access_token = pref.getString("access_token", null);
            String userID = pref.getString("id", null);
            kklinx.authenticate(userID, access_token);
        }

        @Override
        public void onOnline() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(KKlinxActivity.this,
                            "kklinx onOnline",Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onOffline() {

        }

        @Override
        public void onVolumeChanged(float v) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (v * maxVolume), AudioManager.FLAG_SHOW_UI);

        }

        @Override
        public void onPlaybackStart() {

        }

        @Override
        public void onPlaybackStop(int i) {

        }

        @Override
        public void onLoading(PlaybackState playbackState) {

        }

        @Override
        public void onLoaded(PlaybackState playbackState) {
            Log.d("onLoaded",playbackState.getDuration()+"");
            updateUI(playbackState);
        }

        @Override
        public void onLoadError(int i, String s) {

        }

        @Override
        public void onPlaying(PlaybackState playbackState) {
            seekBarHandler.postDelayed(seekBarUpdate,1000);
            buttonPlayPause.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
            buttonPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    kklinx.pause();
                }
            });
        }

        @Override
        public void onPaused(PlaybackState playbackState) {
            updateUI(playbackState);
            seekBarHandler.removeCallbacks(seekBarUpdate);
            buttonPlayPause.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp));
            buttonPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    kklinx.resume();
                }
            });
        }

        @Override
        public void onResumed(PlaybackState playbackState) {
            updateUI(playbackState);
            buttonPlayPause.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
            buttonPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    kklinx.pause();
                }
            });
        }

        @Override
        public void onStopped(PlaybackState playbackState) {
            seekBarHandler.removeCallbacks(seekBarUpdate);
        }

        @Override
        public void onPlaylistEnded(int i) {

        }

        @Override
        public void onCredentialUpdated(String s, String s1, String s2) {

        }
    }

}

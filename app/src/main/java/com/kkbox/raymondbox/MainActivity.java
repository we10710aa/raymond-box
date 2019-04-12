package com.kkbox.raymondbox;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.api.aws.PollyTextToSpeech;
import com.api.kkbox.KKAssistant;
import com.api.kkbox.Partner;
import com.google.gson.JsonObject;
import com.kkbox.kklinxdevicesdk.Config;
import com.kkbox.kklinxdevicesdk.KKLINXDeviceSdk;
import com.kkbox.kklinxdevicesdk.PlaybackState;
import com.squareup.picasso.Picasso;
import com.zqc.opencc.android.lib.ChineseConverter;
import com.zqc.opencc.android.lib.ConversionType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private MediaController controller;
    private KKLINXDeviceSdk kklinx;
    private Thread kklinxThread;
    private PlaybackState state;
    private int position = 0;
    private ArrayList<String> playlist;

    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private static final String KEYPHRASE = "ok raymond";
    private static final String KWS_SEARCH = "wakeup";

    protected PollyTextToSpeech pollyTextToSpeech;
    private TextToSpeech textToSpeech;

    //request code
    private static final int ME_PERMISSION = 1;
    private static final int SPEECH_TO_TEXT = 200;

    Handler positionUpdateHandler = new Handler();
    final Runnable positionUpdater = new Runnable() {
        public void run() {
            position += 500;
            positionUpdateHandler.postDelayed(this, 500);
        }
    };

    private TextView trackInfo;
    private TextView textInitializing;
    private ProgressBar progressBar;
    private ImageView trackImage;
    private Switch pollySwitch;
    private BottomNavigationView bottomNavigationView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate");
        setContentView(R.layout.activity_main);
        setUiComponnent();

        Config config = new Config(
                getApplicationContext(),
                "Raymond's Mac Pro",
                "Raymond",
                "Mac Pro 2017",
                ClientInfo.clientID,
                ClientInfo.clientSecret,
                44444,
                getFilesDir().getAbsolutePath());
        kklinx = new KKLINXDeviceSdk(config);
        kklinx.registerEventListener(new KKLINXEventListener());

        if (kklinxThread == null) {
            kklinxThread = new Thread() {
                @Override
                public void run() {
                    kklinx.startMainLoop();
                }
            };
        }
        pollyTextToSpeech = new PollyTextToSpeech(
                this, ClientInfo.awsPoolID, ClientInfo.awsRegions);

        textToSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    if (textToSpeech.isLanguageAvailable(Locale.TAIWAN)
                            == TextToSpeech.LANG_AVAILABLE) {
                        textToSpeech.setLanguage(Locale.TAIWAN);
                    }
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    ME_PERMISSION);
        } else {
            new SetUpRecognizerTask().execute();
        }
        kklinx.initialize();
        kklinxThread.start();
        registerMdns(getApplicationContext(), "Raymond's Mac Pro", 44444);

    }

    private void setUiComponnent() {
        setTitle("Raymond's Box");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        trackInfo = findViewById(R.id.text_view);
        textInitializing = findViewById(R.id.text_initializing);
        progressBar = findViewById(R.id.progressBar_MainActivity);
        trackImage = findViewById(R.id.img_song);
        pollySwitch = findViewById(R.id.switchAmazon);
        bottomNavigationView = findViewById(R.id.btm_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_play);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.nav_recommend:
                        startActivity(new Intent(MainActivity.this, PersonalActivity.class));
                        break;
                    case R.id.nav_play:
                        break;
                    case R.id.nav_unknown:
                        break;
                }
                return true;
            }
        });
        //MediaPlayer control panel
        controller = new MediaController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kklinx.playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kklinx.playPrevious();
            }
        });
        controller.setMediaPlayer(new MediaControlListener());
        controller.setAnchorView(findViewById(R.id.main_view));
        controller.setEnabled(true);

        findViewById(R.id.main_view).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                controller.show(0);
                return false;
            }
        });
    }

    class SetUpRecognizerTask extends AsyncTask<Void, Void, Exception> {
        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                Assets assets = new Assets(MainActivity.this);
                File assetDir = assets.syncAssets();
                recognizer = SpeechRecognizerSetup.defaultSetup()
                        .setAcousticModel(new File(assetDir, "en-us-ptm"))
                        .setDictionary(new File(assetDir, "cmudict-en-us.dict"))
                        .getRecognizer();
                recognizer.addListener(new KeyWordListener());
                recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e != null) {
                Log.e("Recognizer", e.getMessage());
            } else {
                recognizer.stop();
                recognizer.startListening(KWS_SEARCH);
                progressBar.setVisibility(View.INVISIBLE);
                textInitializing.setVisibility(View.INVISIBLE);
                trackImage.setVisibility(View.VISIBLE);
                trackInfo.setVisibility(View.VISIBLE);
                pollySwitch.setVisibility(View.VISIBLE);
            }
        }
    }

    private void registerMdns(Context context, String serviceName, Integer servicePort) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setServiceType("_kklinx._tcp");
        serviceInfo.setPort(servicePort);
        serviceInfo.setAttribute("CPath", "/device");

        NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d("KKLINX", "MDNS Service registered");
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d("KKLINX", "MDNS Service registration failed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d("KKLINX", "MDNS Service unregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d("KKLINX", "MDNS Service unregistration failed");
            }
        };

        NsdManager mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actions_speak:
                recognizer.stop();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        getApplication().getPackageName());
                startActivityForResult(intent, SPEECH_TO_TEXT);
                break;
            case R.id.actions_logout:
                kklinx.stopPlayback();
                kklinx.destroy();
                recognizer.stop();
                recognizer.shutdown();
                pollyTextToSpeech.shutDown();
                getSharedPreferences("USER", MODE_PRIVATE).edit().clear().commit();
                startActivity(new Intent(this, StartActivity.class));
                break;
        }
        return true;
    }

    @Override
    protected void onPause() {
        if (recognizer != null)
            recognizer.stop();
        Log.d("MainActivity", "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("MainActivity", "onStop");
        super.onStop();
    }

    @Override
    protected void onResume() {
        Log.d("Mainactivity", "onResume");
        super.onResume();
        if (recognizer != null) {
            recognizer.stop();
            recognizer.startListening(KWS_SEARCH);
        }
        bottomNavigationView.setSelectedItemId(R.id.nav_play);
    }

    @Override
    protected void onRestart() {
        Log.d("Mainactivity", "onRestart");
        super.onRestart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SPEECH_TO_TEXT:
                switch (resultCode) {
                    case RESULT_OK:
                        ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        Log.d("Speech2Text: ", result.get(0));
                        String key = result.get(0);
                        askKKAssistant(key);
                        break;
                    case RESULT_CANCELED:
                        askKKAssistant("我想聽愛情怎麼了嗎");
                        break;
                }
        }
    }

    private void askKKAssistant(final String key) {
        Call<JsonObject> request = KKAssistant.getInstance(this).ask(key);
        final TextView timeLog = findViewById(R.id.text_timeLog);
        final long startTime = System.nanoTime();
        request.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, final Response<JsonObject> response) {
                long endTime = System.nanoTime();
                timeLog.append("\n" + key + "spent(ms):" + (endTime - startTime) / 1000000);
                Log.d("KKassistant result", response.body().toString());
                if (pollySwitch.isChecked()) {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            executeKKAssistantInstruction(response.body());
                            mp.release();
                        }
                    });
                    speakText(KKAssistant.parseShowText(response.body()), mediaPlayer);
                } else {
                    speakText(KKAssistant.parseShowText(response.body()));
                    executeKKAssistantInstruction(response.body());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("KKLINX assistant", t.getMessage());
            }
        });
    }

    private void executeKKAssistantInstruction(JsonObject kklinxResponse) {
        switch (KKAssistant.parsePlayType(kklinxResponse)) {
            case KKAssistant.AUDIO_PLAY:
                playlist = KKAssistant.parsePlayList(kklinxResponse);
                kklinx.loadTracks(playlist);
                break;
            case KKAssistant.AUDIO_SKIP_FORWARD:
                kklinx.playNext();
                break;
            case KKAssistant.AUDIO_SKIP_BACKWARD:
                kklinx.playPrevious();
                break;
            case KKAssistant.AUDIO_PAUSE:
                kklinx.pause();
                break;
            case KKAssistant.AUDIO_RESUME:
                kklinx.resume();
                break;
            case KKAssistant.AUDIO_INEFFECTIVE:
                kklinx.resume();
                break;
            case KKAssistant.AUDIO_ERROR:
                Log.d("assistant", "error parsing" + kklinxResponse);
                break;
        }
    }

    private void speakText(String text, MediaPlayer mediaPlayer) { //Speak using amazon polly
        if (text == null) {
            return;
        }
        kklinx.pause();
        pollyTextToSpeech.pollySpeakText(mediaPlayer,
                ChineseConverter.convert(text, ConversionType.T2S, MainActivity.this));
    }

    private void speakText(String text) { //Speak using native method
        if (text == null) {
            return;
        }
        kklinx.pause();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ME_PERMISSION:
                Log.d("onActivityResult", "successful get authorized");
                new SetUpRecognizerTask().execute();
                break;
        }
    }

    private void updateUI() {
        final Call<JsonObject> trackFetcher = Partner.getInstance(this)
                .getPartnerApi().getTrackInfo(state.getSongId());
        trackFetcher.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                String url = Partner.parseTrackImageUrl(response.body());
                Picasso.get().load(url).into((ImageView) findViewById(R.id.img_song));
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("MainActivity", "update image failure");
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    controller.show(0);
                } catch (Exception e) {
                    Log.d("KKLINX", e.getMessage());
                }
                trackInfo.setGravity(Gravity.CENTER);
                trackInfo.setText(String.format("%s\n%s\n%s",
                        state.getSongName(), state.getAlbumName(), state.getArtistName()));

            }
        });
    }

    class MediaControlListener implements MediaController.MediaPlayerControl {
        @Override
        public void start() {
            kklinx.resume();
        }

        @Override
        public void pause() {
            kklinx.pause();
        }

        @Override
        public int getDuration() {
            if (state != null) {
                return state.getDuration();
            }
            return 0;
        }

        @Override
        public int getCurrentPosition() {
            return position;
        }

        @Override
        public void seekTo(int pos) {
            kklinx.seek(pos);
        }

        @Override
        public boolean isPlaying() {
            if (state != null && state.getStatus() == PlaybackState.PlaybackStatus.PLAYING) {
                return true;
            }
            return false;
        }

        @Override
        public int getBufferPercentage() {
            return 0;
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return 0;
        }

    }

    class KKLINXEventListener implements KKLINXDeviceSdk.EventListener {

        public void onReady() {
            Log.d("KKLINX", "onReady");
            kklinx.setVolume((float) 0.5);
            SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
            String access_token = pref.getString("access_token", null);
            String userID = pref.getString("id", null);
            Log.d("ssss", userID + access_token);
            kklinx.authenticate(userID, access_token);
        }

        public void onOnline() {
            Log.d("KKLINX", "onOnline");
        }

        public void onOffline() {
            Log.d("KKLINX", "onOffline");
        }

        public void onVolumeChanged(float value) {
            Log.d("KKLINX", String.format("OnVolumeChanged: %f", value));
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (value * maxVolume), AudioManager.FLAG_SHOW_UI);
        }

        public void onPlaybackStart() {
        }

        public void onPlaybackStop(int i) {
            if (i == KKLINXDeviceSdk.KKLINX_PLAYBACK_ENDED_INTERRUPTION) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "KKLINX_PLAYBACK_ENDED_INTERRUPTION",
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else if (i == KKLINXDeviceSdk.KKLINX_PLAYBACK_ENDED_SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "KKLINX_PLAYBACK_ENDED_SUCCESS",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        public void onLoading(PlaybackState state) {
            Log.d("KKLINX", "" + Runtime.getRuntime().maxMemory() + " " + Runtime.getRuntime().totalMemory() + " " + Runtime.getRuntime().freeMemory());
            updateLog(state);
            MainActivity.this.state = state;
            position = state.getPosition();
            updateUI();
        }

        public void onLoaded(PlaybackState state) {
            updateLog(state);
            MainActivity.this.state = state;
            position = state.getPosition();
            updateUI();
        }

        public void onLoadError(int code, String message) {
            Log.d("KKLINX", String.format("onLoadError: %d %s", code, message));
        }

        public void onPlaying(PlaybackState state) {
            updateLog(state);
            MainActivity.this.state = state;
            position = state.getPosition();
            positionUpdateHandler.postDelayed(positionUpdater, 500);
            updateUI();
        }

        public void onPaused(PlaybackState state) {
            updateLog(state);
            MainActivity.this.state = state;
            position = state.getPosition();
            positionUpdateHandler.removeCallbacks(positionUpdater);
            updateUI();
        }

        public void onResumed(PlaybackState state) {
            updateLog(state);
            MainActivity.this.state = state;
            position = state.getPosition();
            updateUI();
        }

        public void onStopped(PlaybackState state) {
            updateLog(state);
            MainActivity.this.state = state;
            position = state.getPosition();
            positionUpdateHandler.removeCallbacks(positionUpdater);
            updateUI();
        }

        public void onPlaylistEnded(int reason) {
            Log.d("KKLINX", String.format("onPlaylistEnded: %d", reason));
        }

        public void onCredentialUpdated(String userId, String accessToken, String refreshToken) {
            Log.d("KKLINX", String.format("onCredentialUpdated: %s %s %s", userId, accessToken, refreshToken));
        }

        public void updateLog(PlaybackState state) {
            Log.d("KKLINX", String.format("%s %d %d %d %s %s %s %s",
                    ((Enum) state.getStatus()).name(),
                    state.getIndex(),
                    state.getPosition(),
                    state.getDuration(),
                    state.getSongId(),
                    state.getSongName(),
                    state.getAlbumName(),
                    state.getArtistName()
            ));
        }
    }

    class KeyWordListener implements edu.cmu.pocketsphinx.RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {
            if (!recognizer.getSearchName().equals(KWS_SEARCH)) {
                recognizer.stop();
                recognizer.startListening(KWS_SEARCH);
            }
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null)
                return;

            String text = hypothesis.getHypstr();
            if (text.equals(KEYPHRASE)) {
                recognizer.stop();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        getApplication().getPackageName());
                startActivityForResult(intent, SPEECH_TO_TEXT);
            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
        }

        @Override
        public void onError(Exception e) {

        }

        @Override
        public void onTimeout() {
            recognizer.stop();
            recognizer.startListening(KWS_SEARCH);
        }
    }
}


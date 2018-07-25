package com.nomand.driveassistant;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener, RecognitionVisualiser{

    private TextToSpeech tts;
    private ProgressBar progBar;
    private TextView mainText;
    private VoiceListener voiceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // get the progress bar, we will modify it later
        progBar = findViewById(R.id.progressBar);
        mainText = findViewById(R.id.textView);
        voiceListener = VoiceListener.createListener(this);
        // setup the tts first
        tts = new TextToSpeech(this,this);
    }

    // tts set up
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Toast.makeText(this, "数据丢失或不支持",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ((ProgressBar)findViewById(R.id.progressBar)).setProgress(33);
        }
        // call async task to initiate voice recognition and get contact list
        new InitAsync().execute();
    }

    private class InitAsync extends AsyncTask<Void,Void,Exception> {

        @Override
        protected Exception doInBackground(Void...params) {
            //TODO: prepare the cmu sphinx voice listener
            try {
                Assets assets = new Assets(MainActivity.this);
                File assetDir = assets.syncAssets();
                voiceListener.setupRecognizer(assetDir);
            } catch (IOException e){
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) return;
            // something bad happened
            super.onPostExecute(e);
            Toast.makeText(MainActivity.this,"语音识别初始化失败",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        tts.stop();
        tts.shutdown();
    }

    @Override
    public void setText(String text) {
        mainText.setText(text);
    }

    @Override
    public void setTTS(String text) {
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        voiceListener.tearDown();
    }
}

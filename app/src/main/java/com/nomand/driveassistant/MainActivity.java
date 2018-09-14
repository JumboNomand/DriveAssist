package com.nomand.driveassistant;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener, RecognitionVisualiser{

    private TextToSpeech tts;               //tts object
    private ProgressBar progBar;            //progress bar, but not very useful
    private TextView mainText;              //main UI part
    private VoiceListener voiceListener;    //voice listener object for cmu-sphinx

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // get the progress bar, we will modify it later
        progBar = findViewById(R.id.progressBar);
        // main UI
        mainText = findViewById(R.id.textView);
        // create object, listener object will be initialized later
        voiceListener = VoiceListener.createListener(this);
        // setup the tts first
        tts = new TextToSpeech(this,this);
    }

    // tts set up
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);
            // Chinese tts not supported
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Toast.makeText(this, "数据丢失或不支持",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ((ProgressBar)findViewById(R.id.progressBar)).setProgress(33);
        }
        // call async task to initiate voice recognition
        new InitAsync().execute();
    }

    private class InitAsync extends AsyncTask<Void,Void,Exception> {

        @Override
        protected Exception doInBackground(Void...params) {
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
            if (e == null) {
                // setup finished, switch to menu recognition
                progBar.setVisibility(View.INVISIBLE);
                mainText.setText("菜单");
                setTTS("菜单");
                return;
            }
            // something bad happened
            super.onPostExecute(e);
            Toast.makeText(MainActivity.this,"语音识别初始化失败",Toast.LENGTH_SHORT).show();
            // what can I do, exit
            main_kill();
        }
    }

    // end this app
    void main_kill(){
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        tts.stop();
        voiceListener.pause();
        // don't shut down tts when this app is no longer visible
        //tts.shutdown();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        voiceListener.resume();
    }

    @Override
    public void setText(String text) {
        mainText.setText(text);
    }

    @Override
    public void setTTS(String text) {
        // pause since tts could interfere with voice recognizing
        voiceListener.pause();
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
        // blocks until finish
        while (tts.isSpeaking());
        voiceListener.resume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // clear tts and voice listener
        tts.shutdown();
        voiceListener.tearDown();
    }
}

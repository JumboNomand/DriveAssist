package com.nomand.driveassistant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener, RecognitionListener{


    private TextToSpeech tts;
    SpeechRecognizer recognizer;
    List<Contact> contacts;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 1;
    private static final int PERMISSIONS_REQUEST_READ_CONTACT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tts = new TextToSpeech(this,this);

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // Check if user has given permission to call phone
        permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
            return;
        }
        // Check if user has given permission to call phone
        permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACT);
            return;
        }

        // call async task to initiate voice recognition and get contact list
        new InitAsync().execute(this);
    }

    public void startMainProg(List<Contact> par_contacts){
        contacts = par_contacts;
        recognizer.startListening(RecogProcess.MENU);
        // fade the loading parts show the main menu
        findViewById(R.id.progressBar).setVisibility(0);
        ((TextView)findViewById(R.id.textView)).setText("菜单");
    }

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
    }


    @Override
    protected void onStop() {
        super.onStop();
        tts.stop();
        tts.shutdown();
    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        // Command Routing Logic here
        if (hypothesis == null) return;
        if (tts.isSpeaking()) return;

        String text = hypothesis.getHypstr();
        switch (text){
            case RecogProcess.MENU:
                recognizer.startListening(RecogProcess.MENU);
                tts.speak(RecogProcess.MENU,TextToSpeech.QUEUE_FLUSH,null);
                break;
            case RecogProcess.DIAL:
                recognizer.startListening(RecogProcess.DIAL);
                tts.speak(RecogProcess.DIAL,TextToSpeech.QUEUE_FLUSH,null);
                break;
            case RecogProcess.DIGI:
                recognizer.startListening(RecogProcess.DIGI);
                tts.speak(RecogProcess.DIGI,TextToSpeech.QUEUE_FLUSH,null);
                break;
            case RecogProcess.CONTACT:
                recognizer.startListening(RecogProcess.CONTACT);
                tts.speak(RecogProcess.CONTACT,TextToSpeech.QUEUE_FLUSH,null);
                break;
            default:
                Toast.makeText(this,"无法识别",Toast.LENGTH_SHORT);
                // use tts
                tts.speak("无法识别",TextToSpeech.QUEUE_FLUSH,null);
        }
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onResult(Hypothesis hypothesis) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }
}

package com.nomand.driveassistant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class VoiceListener implements RecognitionListener {

    // speech recognizer
    private static SpeechRecognizer recognizer;

    // a copy of reference of context
    private AppCompatActivity context;

    // recognizer need audio recording permission
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    // list of recognition process, i.e. states
    public static final HashMap<String,RecognizeProcess> recognitionList= new HashMap<>();

    // asset directory
    public static File assetsDir;

    // cannot instantiate a VoiceListener using constructor
    private VoiceListener(AppCompatActivity context){
        this.context = context;
    }

    // one voice listener limited
    public static VoiceListener createListener(AppCompatActivity context){
        if (context != null) return null;
        return new VoiceListener(context);
    }

    public void setupRecognizer(File in_assetsDir) throws IOException {
        // prevent repeated setup
        if (recognizer != null) return;
        if (context == null) throw new RuntimeException("No context");

        assetsDir = in_assetsDir;

        // get record audio permission
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        // recognizer setup
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "zh_broadcastnews_ptm"))
                .setDictionary(new File(assetsDir, "zh_broadcastnews_utf8.dic"))
                .getRecognizer();
        recognizer.addListener(this);

        //load recognition processes
        RecognitionProcessLoader(assetsDir);

        //start listening menu
        recognizer.startListening("菜单");
    }

    // The recognizer can be configured to perform multiple searches
    // of different kind and switch between them
    private void RecognitionProcessLoader(File assetsDir){
        // load recognize process from file
        BufferedReader loadListReader;
        try {
            loadListReader = new BufferedReader(new FileReader(new File(assetsDir, "load_list")));
        }catch (FileNotFoundException e){
            throw new RuntimeException(e);
        }

        String line;
        String[] lineArgs;
        Class recognitionClass;
        Field field;
        RecognizeProcess tempRP;

        while (true){
            try {
                line = loadListReader.readLine();
            }catch (IOException e){
                throw new RuntimeException(e);
            }
            if (line == null) break;
            // parse each line
            lineArgs = line.split(" ");
            lineArgs[0] = "com.nomand.driveassistant."+lineArgs[0];
            try {
                // search for class using reflection
                recognitionClass = Class.forName(lineArgs[0]);
                tempRP = (RecognizeProcess)recognitionClass.newInstance();
                tempRP.setName(lineArgs[1]);

                // after we loaded each recognition process, we put it in map, set it up
                recognitionList.put(lineArgs[1],tempRP);
                tempRP.setup(context);

                // search loading is different for different type
                field = recognitionClass.getField("type");
                if (field.get(null) == RecognizeProcessType.KEYWORD){
                    recognizer.addKeywordSearch(lineArgs[1], new File(assetsDir, lineArgs[2]));
                }else{
                    recognizer.addGrammarSearch(lineArgs[1], new File(assetsDir, lineArgs[2]));
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    // used for continuous decoding
    // all recognition state transition routed here
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        // skip null result
        if (hypothesis == null) return;
        String nextStep;
        String result = hypothesis.getHypstr();

        String searchName = recognizer.getSearchName();
        RecognizeProcess tempRP = recognitionList.get(searchName);
        try {
            nextStep = tempRP.handler(result);
            // bad thing happened or no match
            if (nextStep == null) recognizer.startListening("菜单");
            // if no recognition process(state) change, do nothing
            if (nextStep.equals(searchName)) return;
            // change state
            tempRP = recognitionList.get(nextStep);
            recognizer.stop();
            recognizer.startListening(nextStep);
            // note stop and start new listening first then update UI/tts, since tts would pause recognize process
            tempRP.switchTo();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    // used for pausing recognition process
    private static String pausedProcess;

    public void pause(){
        pausedProcess = recognizer.getSearchName();
        recognizer.stop();
    }

    public void resume(){
        recognizer.startListening(pausedProcess);
    }

    // not used
    @Override
    public void onBeginningOfSpeech() {

    }

    // called when met silence
    @Override
    public void onEndOfSpeech() {
        //recognizer.stop();
        // we don't sleep here
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        // not used
    }

    @Override
    public void onError(Exception e) {
        // not used
    }
    // no timeout in this application
    @Override
    public void onTimeout() {

    }

    public void tearDown(){
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }
}

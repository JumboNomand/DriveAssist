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

    private static SpeechRecognizer recognizer;

    private AppCompatActivity context;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public static final HashMap<String,Class> recognitionList= new HashMap<>();

    // cannot instantiate a VoiceListener
    private VoiceListener(AppCompatActivity context){
        this.context = context;
    }

    public static VoiceListener createListener(AppCompatActivity context){
        return new VoiceListener(context);
    }

    public void setupRecognizer(File assetsDir) throws IOException {
        if (recognizer != null) return;
        if (context == null) throw new RuntimeException("No context");

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
                //.setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
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
                recognitionClass = Class.forName(lineArgs[0]);
                field = recognitionClass.getField("name");
                tempRP = (RecognizeProcess)recognitionClass.newInstance();
                field.set(tempRP,lineArgs[1]);

                field = recognitionClass.getField("type");
                if (field.get(null) == RecognizeProcessType.KEYWORD){
                    recognizer.addKeywordSearch(lineArgs[1], new File(assetsDir, lineArgs[2]));
                }else{
                    recognizer.addGrammarSearch(lineArgs[1], new File(assetsDir, lineArgs[2]));
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }
            // after we loaded each recognition process, we put it in map, set it up
            recognitionList.put(lineArgs[1],recognitionClass);
            tempRP.setup(context);

        }
        // hardcode menu setup
        RecognizeProcessData.saveData("菜单",assetsDir);
        try{
            recognitionList.put("菜单",Class.forName("com.nomand.driveassistant.MenuRecognition"));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        // this setup will write the menu.kws file
        (new MenuRecognition()).setup(context);
        //recognizer.addKeywordSearch("菜单",new File(assetsDir, "menu.kws"));
        recognizer.addKeyphraseSearch("菜单","打电话");
    }

    // used for continuous decoding
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) return;
        // Keyword Recognition here
        String nextStep;
        String result = hypothesis.getHypstr();
        String searchName = recognizer.getSearchName();
        Class recognitionClass = recognitionList.get(searchName);
        RecognizeProcess tempRP;
        try {
            tempRP = (RecognizeProcess) recognitionClass.newInstance();
            if (recognitionClass.getField("type").get(null) == RecognizeProcessType.KEYWORD){
                nextStep = tempRP.handler(result);
                recognitionClass = recognitionList.get(nextStep);
                tempRP = (RecognizeProcess)(recognitionClass.newInstance());
                tempRP.switchTo(recognizer);
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    // called when met silence
    @Override
    public void onEndOfSpeech() {
        //recognizer.stop();
    }

    // main recognition logic here
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis == null) return;
        //TODO: REFACTOR

    }

    @Override
    public void onError(Exception e) {

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

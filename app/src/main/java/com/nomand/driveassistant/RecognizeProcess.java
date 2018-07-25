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
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

enum RecognizeProcessType {KEYWORD, GRAMMAR}

interface Confirmable {
    String afterConfirmHandler();
}

public abstract class RecognizeProcess {
    public static String name;

    protected static RecognitionVisualiser visualiser;

    public void setup(AppCompatActivity context){
        visualiser = (RecognitionVisualiser)context;
    }

    public abstract void switchTo(SpeechRecognizer recognizer);

    public abstract String handler(String result);
}

abstract class KeywordRecognition extends RecognizeProcess{

    public static final RecognizeProcessType type = RecognizeProcessType.KEYWORD;
}

class MenuRecognition extends KeywordRecognition {

    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
        name = "菜单";    // hardcoded
/*        File assetDir = (File) RecognizeProcessData.retrieveData(name);
        // read from the file to setup menu list
        FileWriter menuWriter;
        StringBuilder menuString = new StringBuilder();
        try {
            menuWriter = new FileWriter(new File(assetDir,"menu.kws"));

            for (String key:VoiceListener.recognitionList.keySet()){
                menuString.append(key);
                //TODO: change threshold logic
                menuString.append("/1e-10/\n");
            }
            //menuWriter.write(menuString.toString());
        }catch (Exception e){
            throw new RuntimeException(e);
        }*/
    }

    @Override
    public void switchTo(SpeechRecognizer recognizer) {
        recognizer.startListening(name);
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        Class targetClass;
        Field target;
        String nextStep;

        targetClass = VoiceListener.recognitionList.get(result);
        if (targetClass == null){
            visualiser.setTTS("我不知道");
            return name;
        }
        try {
            target = targetClass.getField("name");
            nextStep = (String)target.get(null);
        } catch (Exception e){
            // fuck me no way
            throw new RuntimeException(e);
        }

        return nextStep;
    }
}

abstract class GrammarRecognition extends RecognizeProcess{
    public static final RecognizeProcessType type = RecognizeProcessType.GRAMMAR;
}

class CallRecognition extends GrammarRecognition implements Confirmable{
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 1;
    private static final int PERMISSIONS_REQUEST_READ_CONTACT = 1;

    @Override
    public void setup(AppCompatActivity context) {
        // request permission to read contacts

        int permissionCheck;
        // Check if user has given permission to call phone
        permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
            return;
        }
        // Check if user has given permission to read contacts
        permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACT);
            return;
        }

    }

    @Override
    public void switchTo(SpeechRecognizer recognizer) {

    }

    @Override
    public String handler(String result) {
        return null;
    }

    @Override
    public String afterConfirmHandler() {
        // well, confirmed, we will call the phone
        return MenuRecognition.name;
    }
}

class ConfirmRecognition extends KeywordRecognition{
    @Override
    public void setup(AppCompatActivity context) {

    }

    @Override
    public void switchTo(SpeechRecognizer recognizer) {

    }

    @Override
    public String handler(String result) {
        return null;
    }
}




    // Check if user has given permission to record audio



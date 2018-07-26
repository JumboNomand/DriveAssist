package com.nomand.driveassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

    public abstract void switchTo();

    public abstract String handler(String result);
}

abstract class KeywordRecognition extends RecognizeProcess{

    public static final RecognizeProcessType type = RecognizeProcessType.KEYWORD;
}

class MenuRecognition extends KeywordRecognition {

    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
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
    public void switchTo() {
        visualiser.setText("菜单");
        visualiser.setTTS("主菜单");
        // wait until speak finish?
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        Class targetClass;

        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");
        String nextStep = recognizeList[0];

        targetClass = VoiceListener.recognitionList.get(nextStep);
        if (targetClass == null){
            visualiser.setText("?");
            visualiser.setTTS("我不知道");
            return name;
        }

        return nextStep;
    }
}

abstract class GrammarRecognition extends RecognizeProcess{
    public static final RecognizeProcessType type = RecognizeProcessType.GRAMMAR;
}

class CallRecognition extends KeywordRecognition{
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 1;
    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
        int permissionCheck;
        // Check if user has given permission to call phone
        permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE);
        while (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
        }
    }

    @Override
    public void switchTo() {
        visualiser.setText("打电话 电话号码还是联系人？");
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        Class targetClass;

        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");
        String nextStep = recognizeList[0];

        targetClass = VoiceListener.recognitionList.get(nextStep);
        if (targetClass == null){
            visualiser.setText("?");
            visualiser.setTTS("我不知道");
            return name;
        }

        return nextStep;
    }
}

class PhoneNumberRecognition extends GrammarRecognition implements Confirmable{

    private static final int PERMISSIONS_REQUEST_READ_CONTACT = 1;

    private static final HashMap<String,String> numberMap = new HashMap<>();

    private void fillNumberMap(){
        numberMap.put("一","1");
        numberMap.put("二","2");
        numberMap.put("三","3");
        numberMap.put("四","4");
        numberMap.put("五","5");
        numberMap.put("六","6");
        numberMap.put("七","7");
        numberMap.put("八","8");
        numberMap.put("九","9");
        numberMap.put("零","0");
    }

    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
        // request permission to read contacts
        fillNumberMap();

        int permissionCheck;
        // Check if user has given permission to read contacts
        permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);
        while (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACT);
        }

    }

    @Override
    public void switchTo() {
        visualiser.setText("请 电话号码");
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        visualiser.setText(result);
        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");
        if (recognizeList.length < 11) return name;

        visualiser.setText(result);
        callPhone(result);
        return null;
    }

    @Override
    public String afterConfirmHandler() {
        // well, confirmed, we will call the phone
        return MenuRecognition.name;
    }

    private void callPhone(String preNumber){
        // need to parse and process to real phone number
        StringBuilder realNumber = new StringBuilder("tel:");
        String[] digits = preNumber.split(" ");
        for (String digit:digits){
            realNumber.append(numberMap.get(digit));
        }
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse(realNumber.toString());
        intent.setData(data);
        ((AppCompatActivity)visualiser).startActivity(intent);
    }
}

class ConfirmRecognition extends KeywordRecognition{
    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
    }

    @Override
    public void switchTo() {
        visualiser.setText("确认？");
    }

    @Override
    public String handler(String result) {
        return null;
    }
}




    // Check if user has given permission to record audio



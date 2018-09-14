package com.nomand.driveassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

enum RecognizeProcessType {KEYWORD, GRAMMAR}

interface Confirmable {
    String afterConfirmHandler();
}

public abstract class RecognizeProcess {
    public String name;

    protected static RecognitionVisualiser visualiser;

    public void setName(String name){
        this.name = name;
    }

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
    public static String nameStatic;

    @Override
    public void setName(String name) {
        super.setName(name);
        nameStatic = name;
    }

    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
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
        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");
        String nextStep = recognizeList[0];

        RecognizeProcess tempPR = VoiceListener.recognitionList.get(nextStep);
        if (tempPR == null){
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
        visualiser.setTTS("电话号码或联系人");
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");
        String nextStep = recognizeList[0];

        RecognizeProcess tempPR = VoiceListener.recognitionList.get(nextStep);
        if (tempPR == null){
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

    private static String callingNumber;

    // well number mapping
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
        visualiser.setTTS("请说电话号码");
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        visualiser.setText(result);
        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");

        if (recognizeList[recognizeList.length-1].equals("菜单")) return MenuRecognition.nameStatic;

        if (recognizeList.length < 11) return name;

        callingNumber = result;

        visualiser.setTTS(callingNumber);

        // request confirmation
        RecognizeProcessData.saveData(ConfirmRecognition.nameStatic,name);
        return ConfirmRecognition.nameStatic;
    }

    @Override
    public String afterConfirmHandler() {
        // well, confirmed, we will call the phone
        callPhone(callingNumber);
        return MenuRecognition.nameStatic;
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

class ContactRecognition extends KeywordRecognition implements Confirmable{
    private List<Contact> contactList;
    private String callingNumber;

    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
        File assetDir = VoiceListener.assetsDir;
        // read from the file to setup menu list
        FileWriter contactWriter;
        StringBuilder menuString = new StringBuilder();

        contactList = ContactUtil.getContactList(context);

        try {
            contactWriter = new FileWriter(new File(assetDir,"contacts.kws"),false);

            for (Contact key:contactList){
                menuString.append(key.name);
                //TODO: change threshold logic
                menuString.append("/1e-45/\n");
            }
            // add back to menu functionality
            menuString.append("菜单/1e-45/\n");
            contactWriter.write(menuString.toString());
            contactWriter.close();
        }catch (Exception e){
            throw new RuntimeException(e);
        }


    }

    @Override
    public void switchTo() {
        visualiser.setText("请 联系人名");
        visualiser.setTTS("请说联系人");
    }

    @Override
    public String handler(String result) {
        // from the result, get the class of the result and switch to that recognition process
        visualiser.setText(result);
        // parse the result, take the last keyword recognized
        String[] recognizeList = result.split(" ");

        if (recognizeList[0].equals("菜单")) return MenuRecognition.nameStatic;

        callingNumber = ContactUtil.searchContactList(contactList,recognizeList[0]).phoneNum;
        if (callingNumber == null) return name;

        // speak the contact name
        visualiser.setTTS(recognizeList[0]);

        // request confirmation
        RecognizeProcessData.saveData(ConfirmRecognition.nameStatic,name);
        return ConfirmRecognition.nameStatic;
    }

    @Override
    public String afterConfirmHandler() {
        callPhone(callingNumber);
        return MenuRecognition.nameStatic;
    }

    private void callPhone(String preNumber){
        // need to filter out all non-number characters
        String[] numberSect = preNumber.split("[^0-9]");
        StringBuilder realNumber = new StringBuilder("tel:");
        for (String number: numberSect){
            realNumber.append(number);
        }

        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse(realNumber.toString());
        intent.setData(data);
        ((AppCompatActivity)visualiser).startActivity(intent);
    }
}

class ConfirmRecognition extends KeywordRecognition{

    private static final String CONFIRM = "确认";
    public static String nameStatic;

    @Override
    public void setName(String name) {
        super.setName(name);
        nameStatic = name;
    }

    @Override
    public void setup(AppCompatActivity context) {
        super.setup(context);
    }

    @Override
    public void switchTo() {
        visualiser.setText("确认？");
        visualiser.setTTS("确认或取消？");
    }

    @Override
    public String handler(String in_result) {
        // first parse the result
        String[] resultList = in_result.split(" ");
        String result = resultList[0];

        String lastStep = (String)RecognizeProcessData.retrieveData(name);

        if (lastStep == null){
            // TODO:something wrong
            return MenuRecognition.nameStatic;
        }

        if (result.equals(CONFIRM)){
            // call the corresponding after confirm handler
            return ((Confirmable)(VoiceListener.recognitionList.get(lastStep))).afterConfirmHandler();
        }else{
            // go back to previous search
            return lastStep;
        }

    }
}


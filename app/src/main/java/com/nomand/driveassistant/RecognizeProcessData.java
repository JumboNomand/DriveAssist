package com.nomand.driveassistant;

import java.util.HashMap;

public final class RecognizeProcessData {
    private static final HashMap<String,Object> data = new HashMap<>();

    public static void saveData(String name, Object input_data){
        data.put(name,input_data);
    }

    public static Object retrieveData(String name){
        return data.get(name);
    }
}

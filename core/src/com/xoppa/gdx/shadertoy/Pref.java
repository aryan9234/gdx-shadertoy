package com.xoppa.gdx.shadertoy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * (c) 2016 Abhishek Aryan
 *
 * @author Abhishek Aryan
 * @since 8/2/2016.
 *
 */
public class Pref {

    static Preferences preferences;
    private static final String CURRENT_FILE="currentFile";
    static final String CURRENT_FILE_PATH="shaders/startup.fragment.glsl";

    static void init(){

        preferences= Gdx.app.getPreferences(Constants.PREF);

    }

    static String getCurrentFile(){
        return preferences.getString(CURRENT_FILE,"shaders/startup.fragment.glsl");
    }

    static boolean hasCurrentFile(){
        return preferences.getBoolean("hasFile",false);
    }

    static void currentFileStatus(boolean status){

        preferences.putBoolean("hasFile",status);
        preferences.flush();
    }

    static void saveCurrentFile(String filePath){
        currentFileStatus(true);
        preferences.putString(CURRENT_FILE,filePath);
        preferences.flush();
    }






}

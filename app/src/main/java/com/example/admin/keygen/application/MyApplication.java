package com.example.admin.keygen.application;

import android.app.Application;
import android.content.Context;

/**
 * Created by admin on 2018/7/12.
 */

public class MyApplication extends Application {
    private static Context context;
    private static boolean isMater;
    private static boolean NTPSucceed;
    //public static final int MSG_REV = 0;
    //public static final int MSG_SEND = 1;
    private static String rawKey;
    private static String finalKey;


    @Override
    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }

    public static void setIsMater(boolean b){
        isMater = b;
    }

    public static boolean isMaster(){
        return isMater;
    }

    public static void setNTPSucceed(boolean b){NTPSucceed = b;}

    public static boolean getNTPSucceed(){return NTPSucceed;}

    public static String getRawKey() {return rawKey;}

    public static void setRawKey(String key) {rawKey = key;}

    public static String getFinalKey() {return finalKey;}

    public static void setfinalKey(String key) {finalKey = key;}

}

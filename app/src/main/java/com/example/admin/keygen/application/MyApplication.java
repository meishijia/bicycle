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


    @Override
    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        NTPSucceed = false;
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
}

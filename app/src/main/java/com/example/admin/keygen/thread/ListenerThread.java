package com.example.admin.keygen.thread;

/**
 * Created by admin on 2018/7/16.
 */

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.admin.keygen.activity.MainActivity;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Created by Faioo on 2018/5/31.
 */

public class ListenerThread implements Runnable {


    private int mPort;
    private Handler mHandler;
    private Socket s;
    private ServerSocket ss;

    public ListenerThread(int port, Handler handler)
    {
        this.mPort = port;
        this.mHandler = handler;
        try {
            ss = new ServerSocket(this.mPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Log.d("ListenerThread","ListenerThread is running");
        while (true)
        {
            try{
                //阻塞
                s = ss.accept();
                Log.d("ListenerThread","A device is connected");
                Message message = Message.obtain();
                message.what = MainActivity.DEVICE_CONNECTING;
                mHandler.sendMessage(message);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    public Socket getSocket(){
        return s;
    }

}



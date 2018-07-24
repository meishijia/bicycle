package com.example.admin.keygen.thread;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.admin.keygen.activity.MainActivity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static com.example.admin.keygen.activity.MainActivity.END;
import static com.example.admin.keygen.activity.MainActivity.END_INT;
import static com.example.admin.keygen.activity.MainActivity.INFO_RECON_START;
import static com.example.admin.keygen.activity.MainActivity.INFO_RECON_START_INT;
import static com.example.admin.keygen.activity.MainActivity.KEY_GEN_FINISHED;
import static com.example.admin.keygen.activity.MainActivity.KEY_GEN_FINISHED_INT;
import static com.example.admin.keygen.activity.MainActivity.KEY_GEN_START;
import static com.example.admin.keygen.activity.MainActivity.KEY_GEN_START_INT;
import static com.example.admin.keygen.activity.MainActivity.MSG_SEND;
import static com.example.admin.keygen.activity.MainActivity.NTP_SYNC_FAILED;
import static com.example.admin.keygen.activity.MainActivity.NTP_SYNC_FAILED_INT;
import static com.example.admin.keygen.activity.MainActivity.NTP_SYNC_REQUEST;
import static com.example.admin.keygen.activity.MainActivity.NTP_SYNC_REQUEST_INT;
import static com.example.admin.keygen.activity.MainActivity.NTP_SYNC_SUCCESS;
import static com.example.admin.keygen.activity.MainActivity.NTP_SYNC_SUCCESS_INT;



/**
 * Created by admin on 2018/7/16.
 */

public class ConnectThread extends Thread{
    private Socket socket;
    //private String serverAddress;
    private Handler mainHandler;
    private BufferedReader bufferedReader = null;
    //private InputStream inputStream;
    private BufferedOutputStream outputStream;
    public Handler threadHandler;

    public ConnectThread(Socket socket, Handler handler){
        this.socket = socket;
        this.mainHandler = handler;
    }

    @Override
    public void run() {
        if(socket==null){
            return;
        }
        Log.d("ConnectThread","connect thread is running");
        mainHandler.sendEmptyMessage(MainActivity.DEVICE_CONNECTED);
        try {
            //获取数据流
            //inputStream = socket.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            if(outputStream == null){
                Log.d("ConnectThread","When establish the socket, outputStream is null.");
            }
            //byte[] buffer = new byte[1024];
            //int bytes;
            new Thread(){
                @Override
                public void run(){
                    super.run();
                    try{
                        String content;
                        while ((content = bufferedReader.readLine()) != null){
                            Log.d("ConnectThread","Thread in connect thread ,and received message "+content);
                            if (content.equals(END)) {
                                Log.d("ConnectThread","connect thread received END message");
                                socket.close();
                                bufferedReader.close();
                                outputStream.close();
                            }
                            // only client will receive this message
                            else if(content.equals(NTP_SYNC_REQUEST)){
                                // 开启一个同步NTP的线程或者直接在这个线程完成
                                // if succeed
                                // send(NTP_SYNC_SUCCESS) to server\
                                // else
                                // send(NTP_SYNC_FAILED) to server
                                mainHandler.sendEmptyMessage(NTP_SYNC_REQUEST_INT);
                            }
                            //only server will receive this message
                            else if(content.equals(NTP_SYNC_SUCCESS)){
                                mainHandler.sendEmptyMessage(NTP_SYNC_SUCCESS_INT);
                            }
                            // only client will receive this message
                            else if(content.equals(KEY_GEN_START)){
                                // send message to MainActivity
                                mainHandler.sendEmptyMessage(KEY_GEN_START_INT);
                            }
                            //only server will receive this message
                            else if(content.equals(KEY_GEN_FINISHED)){
                                mainHandler.sendEmptyMessage(KEY_GEN_FINISHED_INT);
                            }
                            // only client will receive this message
                            else if(content.equals(INFO_RECON_START)){
                                mainHandler.sendEmptyMessage(INFO_RECON_START_INT);
                            }
                            //only server will receive this message
                            else if(content.equals(NTP_SYNC_FAILED)){
                                mainHandler.sendEmptyMessage(NTP_SYNC_FAILED_INT);
                            }
                            else {
                                Log.d("ConnectThread","Cannot recognize the content of message");
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }.start();

            Looper.prepare();
            threadHandler = new Handler(){
                @Override
                public void handleMessage(Message msg){
                    if (msg.what == MSG_SEND){
                        try {
                            outputStream.write((msg.obj.toString()+"\r\n").getBytes("utf-8"));
                            outputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d("ConnectThread","Send message failed");
                        }
                    }
                    else if(msg.what == END_INT){
                        try{
                            socket.close();
                            bufferedReader.close();
                            outputStream.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                    }
                }
            };
            if(threadHandler == null){
                Log.d("ConnectThread","threadHandler is null");
            }
            else{
                Log.d("ConnectThread","threadHandler is not null");
            }
            Looper.loop();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送数据
     */
    public void sendData(String msg){
        if(outputStream!=null){
            try {
                Log.d("ConnectThread","Sending message");
                outputStream.write(msg.getBytes());
                Message message = Message.obtain();
                message.what = MainActivity.SEND_MSG_SUCCSEE;
                Bundle bundle = new Bundle();
                bundle.putString("MSG",new String(msg));
                message.setData(bundle);
                mainHandler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = MainActivity.SEND_MSG_ERROR;
                Bundle bundle = new Bundle();
                bundle.putString("MSG",new String(msg));
                message.setData(bundle);
                mainHandler.sendMessage(message);
            }
        }
        else{
            Log.d("ConnectThread","outputStream is null.");
        }
    }
}


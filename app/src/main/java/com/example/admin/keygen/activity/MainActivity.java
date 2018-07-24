package com.example.admin.keygen.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.keygen.R;
import com.example.admin.keygen.thread.ConnectThread;
import com.example.admin.keygen.thread.KeyGenThread;
import com.example.admin.keygen.thread.ListenerThread;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public final static int MSG_REV = 11;
    public final static int MSG_SEND = 12;
    public final static int SEND_MSG_SUCCSEE = 13;
    public final static int SEND_MSG_ERROR = 14;
    public final static int PORT = 54321;
    public final static String MASTER_SSID = "MASTER";


    public final static int DEVICE_CONNECTING = 21;
    public final static int DEVICE_CONNECTED = 22;

    public final static int NTP_SYNC_REQUEST_INT = 31;
    public final static int NTP_SYNC_SUCCESS_INT = 32;
    public final static int NTP_SYNC_FAILED_INT = 33;
    public final static int KEY_GEN_START_INT = 34;
    public final static int KEY_GEN_SUCCESS_INT =35;
    public final static int KEY_GEN_FINISHED_INT = 36;
    public final static int INFO_RECON_START_INT = 37;
    public final static int END_INT = 38;

    public final static String NTP_SYNC_REQUEST = "NTP_SYNC_REQUEST";
    public final static String NTP_SYNC_SUCCESS = "NTP_SYNC_SUCCESS";
    public final static String NTP_SYNC_FAILED = "NTP_SYNC_FAILED";

    public final static String KEY_GEN_START = "KEY_GEN_START";
    public final static String KEY_GEN_SUCCESS = "KEY_GEN_SUCCESS";
    public final static String KEY_GEN_FINISHED = "KEY_GEN_FINISHED";

    public final static String INFO_RECON_START = "IOFO_RECON_START";
    public final static String END = "END";


    Button master;
    Button slave;
    Button start;
    TextView textView;
    TextView character;
    TextView keyTextView;
    TextView progressTextView;

    boolean isMaster;
    boolean charChoosed = false;
    boolean ntpSyncRequested = false;
    boolean ntpSyncSucceed = false;

    private ListenerThread listenerThread;
    private ConnectThread slaveConnectThread;
    private ConnectThread masterConnectThread;
    private MainHandler mainHandler;

    public String rawKey;

    WifiManager wifiManager;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initBroadcastReceiver();
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    private void init(){
        master = (Button)findViewById(R.id.master_button);
        slave = (Button)findViewById(R.id.slave_button);
        start = (Button)findViewById(R.id.start_button);
        textView = (TextView)findViewById(R.id.textView);
        character = (TextView)findViewById(R.id.character);
        keyTextView = (TextView)findViewById(R.id.key_show);
        progressTextView = (TextView)findViewById(R.id.progress_show);

        master.setOnClickListener(this);
        slave.setOnClickListener(this);
        start.setOnClickListener(this);

        //申请读写SD卡的权限，否则将无法创建文件
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },1);

        }

        mainHandler = new MainHandler(MainActivity.this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)){
                    int state = intent.getIntExtra("wifi_state",  0);
                    if(state == 13 && isMaster && charChoosed){
                        Log.d("MainActivity","热点已开启");
                        if(listenerThread == null){
                            listenerThread = new ListenerThread(PORT,mainHandler);
                            new Thread(listenerThread).start();
                            Log.d("MainActivity","On broadcastReceived: ListenerThread is running");
                        }
                        else{
                            Log.d("MainActivity","listenThread exists");
                        }
                    }
                    /*
                    else if(state == 11){
                        Log.d("MainActivity","热点已关闭");
                    }
                    else if(state == 10){
                        Log.d("MainActivity","热点正在关闭");
                    }
                    else if(state == 12){
                        Log.d("MainActivity","热点正在开启");
                    }
                    */
                }
                else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if( (!isMaster) && (wifiInfo.getSSID().equals("\""+MASTER_SSID+"\"")) && (slaveConnectThread == null) && charChoosed){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                                    final String serverAddress = intToIp(dhcpInfo.ipAddress);
                                    Log.d("MainActivity","on Broadcast Receive: Connected AP ID:"+serverAddress);
                                    Socket socket = new Socket(serverAddress, PORT);
                                    slaveConnectThread = new ConnectThread(socket, mainHandler);
                                    slaveConnectThread.start();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }

                            }
                        }).start();

                    }
                }

            }
        };

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    return;
                }
                else{
                    Toast.makeText(this,"You denied the permission",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    public void onClick(View view){
        int id = view.getId();
        switch (id){
            case R.id.master_button:
                if(!charChoosed){
                    isMaster = true;
                    charChoosed = true;
                    character.setText("Master");
                    start.setVisibility(View.VISIBLE);

                    if(isWifiApOpened()){
                        Toast.makeText(MainActivity.this,"热点已经开启",Toast.LENGTH_SHORT).show();
                        listenerThread = new ListenerThread(PORT,mainHandler);
                        new Thread(listenerThread).start();
                        Log.d("MainActivity","On clicked: ListenerThread is running");
                    }
                    else {
                        Toast.makeText(MainActivity.this,"请开启热点",Toast.LENGTH_SHORT).show();
                    }

                }
                else{
                    Toast.makeText(MainActivity.this,"您已经选择了角色",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.slave_button:
                if(!charChoosed){
                    isMaster = false;
                    charChoosed = true;
                    character.setText("Slave");
                    if(isConnected2Master()){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                                    final String serverAddress = intToIp(dhcpInfo.serverAddress);
                                    Log.d("MainActivity","on clicked: Connected AP ID:"+serverAddress);
                                    Socket socket = new Socket(serverAddress, PORT);
                                    slaveConnectThread = new ConnectThread(socket, mainHandler);
                                    slaveConnectThread.start();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }

                            }
                        }).start();
                    }
                    else{
                        Log.d("MainActivity","slave is't connected to master.");
                        Toast.makeText(MainActivity.this,"请连接MASTER热点",Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this,"您已经选择了角色",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.start_button:
                if(!charChoosed){
                    Toast.makeText(MainActivity.this,"请选择角色",Toast.LENGTH_SHORT).show();
                    break;
                }
                if(isMaster){
                    if(!isWifiApOpened()){
                        Toast.makeText(MainActivity.this,"请打开MASTER热点",Toast.LENGTH_SHORT).show();
                    }
                    if(masterConnectThread == null){
                        Toast.makeText(MainActivity.this,"等待slave设备连接",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        if(!ntpSyncRequested){
                            if(masterConnectThread.threadHandler == null){
                                Log.d("MainActivity","threadHandler is null");
                            }
                            masterConnectThread.threadHandler.sendMessage(getMessage(NTP_SYNC_REQUEST));
                            Log.d("MainActivity","master send the ntp request to slave");
                            //自己这边开始NTP同步
                            textView.append("master is sync ntp\n");
                            ntpSyncSucceed = true;
                            ntpSyncRequested = true;
                        }
                        else{
                            Toast.makeText(MainActivity.this,"已经启动",Toast.LENGTH_SHORT).show();
                        }

                    }

                }
        }
    }

    private void initBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");

        registerReceiver(receiver,intentFilter);
    }


    public boolean isWifiApOpened(){
        try{
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            int state = (int)method.invoke(wifiManager);
            Field field = wifiManager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            int value = (int)field.get(wifiManager);
            if (state == value) {
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private class MainHandler extends Handler{
        WeakReference<MainActivity> mActicity;
        public MainHandler(MainActivity activity){
            this.mActicity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message message){
            //MainActivity mainActivity = mActicity.get();
            super.handleMessage(message);
            switch (message.what){
                case DEVICE_CONNECTING:
                    Log.d("MainActivity","DEVICE_CONNECTING message is received");
                    masterConnectThread = new ConnectThread(listenerThread.getSocket(),mainHandler);
                    masterConnectThread.start();
                    break;
                case NTP_SYNC_REQUEST_INT:
                    textView.append("master says: sync the NTP\n");
                    slaveConnectThread.threadHandler.sendMessage(getMessage(NTP_SYNC_SUCCESS));
                    break;
                case NTP_SYNC_SUCCESS_INT:
                    if(ntpSyncSucceed){
                        //自己这边也开始密钥生成步骤
                        textView.append("slave says: sync succeeded\n");
                        Log.d("MainActivity", "handleMessage: master start to generate the key");
                        masterConnectThread.threadHandler.sendMessage(getMessage(KEY_GEN_START));
                        KeyGenThread keyGenThread = new KeyGenThread(mainHandler);
                        keyGenThread.start();
                        progressTextView.setText("Master is on key gen,please wait...");
                    }
                    break;
                case NTP_SYNC_FAILED_INT:
                    ntpSyncSucceed = false;
                    ntpSyncRequested = false;
                    textView.append("slave says: he failed sync NTP\n");
                    break;
                case KEY_GEN_START_INT:
                    textView.append("master says:ken gen start\n");
                    KeyGenThread keyGenThread = new KeyGenThread(mainHandler);
                    keyGenThread.start();
                    progressTextView.setText("Slave is on key gen,please wait...");
                    break;
                case KEY_GEN_SUCCESS_INT:
                    rawKey = message.obj.toString();
                    keyTextView.setText(rawKey);
                    progressTextView.setText("The key has been genearated");
                    if(!isMaster){
                        Log.d("MainActivity","slave key gen succeed");
                        slaveConnectThread.threadHandler.sendMessage(getMessage(KEY_GEN_FINISHED));
                    }
                    break;
                case KEY_GEN_FINISHED_INT:
                    masterConnectThread.threadHandler.sendMessage(getMessage(INFO_RECON_START));
                    textView.append("slave says: he finished the key gen procedure\n");
                    //开始信息协调程序
                    masterConnectThread.threadHandler.sendEmptyMessage(END_INT);
                    progressTextView.setText("All finished");
                    ntpSyncRequested=false;
                    ntpSyncSucceed = false;
                    break;
                case INFO_RECON_START_INT:
                    textView.append("master says:information reconciliation start\n");
                    //开始信息协调程序
                    progressTextView.setText("All finished");
                    ntpSyncRequested=false;
                    ntpSyncSucceed = false;
                    break;
            }
        }
    }


    private Message getMessage(String msgString){
        Message msg = Message.obtain();
        msg.what = MSG_SEND;
        msg.obj = msgString;
        return msg;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private boolean isConnected2Master(){
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.d("isConnected2Master","slave ssid:"+wifiInfo.getSSID());
        if(wifiInfo.getSSID().equals("\""+MASTER_SSID+"\"")){
            return true;
        }
        else{
            return false;
        }

    }

    private ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                Log.d("getConnectedIP",line);
                String[] splitted = line.split(" +");
                if (splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
}

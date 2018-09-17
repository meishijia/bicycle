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
import com.example.admin.keygen.RSCode.GF16;
import com.example.admin.keygen.RSCode.Polynomial16;
import com.example.admin.keygen.RSCode.RSDecoder16;
import com.example.admin.keygen.RSCode.Utils;
import com.example.admin.keygen.application.MyApplication;
import com.example.admin.keygen.thread.ConnectThread;
import com.example.admin.keygen.thread.KeyGenThread;
import com.example.admin.keygen.thread.ListenerThread;
import com.example.admin.keygen.zxingRSCode.GenericGF;
import com.example.admin.keygen.zxingRSCode.GenericGFPoly;
import com.example.admin.keygen.zxingRSCode.ReedSolomonDecoder;
import com.example.admin.keygen.zxingRSCode.Utils2;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    public final static String TAG = "MainActivity";

    public final static int MSG_SEND = 10;              //发送信息
    public final static int SEND_MSG_SUCCESS = 11;      //信息发送成功
    public final static int SEND_MSG_ERROR = 12;        //信息发送错误

    public final static int DEVICE_CONNECTING = 20;     //有设备正在连接

    public final static int NTP_SYNC_REQUEST_INT = 30;  //NTP同步请求
    public final static int NTP_SYNC_SUCCESS_INT = 31;  //NTP同步成功
    public final static int NTP_SYNC_FAILED_INT = 32;   //NTP同步失败
    public final static int KEY_GEN_START_INT = 33;     //开始生成密钥
    public final static int KEY_GEN_SUCCESS_INT =34;    //密钥生成成功 (MainActivity <- KeyGenThread)
    public final static int KEY_GEN_FINISHED_INT = 35;  //密钥生成完成
    public final static int INFO_RECON_START_INT = 36;  //开始信息协调
    public final static int SYNDROME_INT = 37;          //介绍到syndrome
    public final static int KEY_CONFIRM_INT = 38;       //密钥验证

    public final static int PAIRING_SUCCESS_INT = 40;   //配对成功
    public final static int PAIRING_FAILED_INT = 41;

    public final static int END_INT = 50;               //结束通信

    public final static String NTP_SYNC_REQUEST = "NTP_SYNC_REQUEST";
    public final static String NTP_SYNC_SUCCESS = "NTP_SYNC_SUCCESS";
    public final static String NTP_SYNC_FAILED = "NTP_SYNC_FAILED";

    public final static String KEY_GEN_START = "KEY_GEN_START";
    //public final static String KEY_GEN_SUCCESS = "KEY_GEN_SUCCESS";
    public final static String KEY_GEN_FINISHED = "KEY_GEN_FINISHED";  //密钥生成完成 (master <- slave)

    public final static String INFO_RECON_START = "IOFO_RECON_START";
    public final static String END = "END";

    public final static String KEY_CONFIRM_MESSAGE = "KEY_CONFIRM_MESSAGE";
    public final static String PAIRING_SUCCESS = "PAIRING_SUCCESS";
    public final static String PAIRING_FAILED = "PAIRING_FAILED";

    public final static int PORT = 54321;
    public final static String MASTER_SSID = "MASTER";

    Button master;
    Button slave;
    /**
     * start button 最开始是隐藏的
     * 当选择了master按钮后，会出现在master设备的界面上
     */
    Button start;
    TextView textView;
    TextView character;
    TextView keyTextView;
    TextView progressTextView;
    TextView newKeyTextView;

    /**
     * 为了这一个软件能通用
     * 设置了角色选择的功能
     * 有两种角色：master 和 slave
     */
    boolean isMaster;
    boolean charChoosed = false;

    /**
     * 设想最初需进行NTP实践同步步骤
     * 目前未实现这一功能
     */
    boolean ntpSyncRequested = false;
    boolean ntpSyncSucceed = false;

    /**
     * master 将开启两个线程
     * listenerThread 负责监听连接
     * masterConnectThread 是与slave设备进行连接并通信的线程
     */
    private ListenerThread listenerThread;
    private ConnectThread slaveConnectThread;
    private ConnectThread masterConnectThread;

    /**
     * mainHandler 将传递到开启的线程
     * 使得线程能够像主线程返回信息
     */
    private MainHandler mainHandler;

    /**
     * rawKey：生成的初始密钥
     */
    //public String rawKey;

    WifiManager wifiManager;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initBroadcastReceiver();
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private void init()
    {
        master = (Button)findViewById(R.id.master_button);
        slave = (Button)findViewById(R.id.slave_button);
        start = (Button)findViewById(R.id.start_button);
        textView = (TextView)findViewById(R.id.textView);
        character = (TextView)findViewById(R.id.character);
        keyTextView = (TextView)findViewById(R.id.key_show);
        progressTextView = (TextView)findViewById(R.id.progress_show);
        newKeyTextView = (TextView)findViewById(R.id.new_key);

        master.setOnClickListener(this);
        slave.setOnClickListener(this);
        start.setOnClickListener(this);

        //申请读写SD卡的权限，否则将无法创建文件，可能导致socket连接失败
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]
                    { Manifest.permission.WRITE_EXTERNAL_STORAGE },1);
        }


        mainHandler = new MainHandler(MainActivity.this);

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                final String action = intent.getAction();
                /**
                 * master需要开启热点
                 * 当检测到wifi热点开启时，创建监听线程
                 */
                if("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action))
                {
                    int state = intent.getIntExtra("wifi_state",  0);
                    if(state == 13 && isMaster && charChoosed)
                    {
                        Log.d("MainActivity","热点已开启");
                        if(listenerThread == null)
                        {
                            listenerThread = new ListenerThread(PORT,mainHandler);
                            new Thread(listenerThread).start();
                            Log.d("MainActivity","On broadcastReceived: ListenerThread is running");
                        }
                        else {
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
                /**
                 * slave需要连接上master的wifi热点才能创建连接进程
                 * 本来想增强软件的健壮性：在选择slave角色之后，如果没有连接上master的热点
                 * 重新连接之后会接受到这个广播，然后尝试连接
                 * 但没有成功，原因是 permission denied
                 */
                else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
                {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //Log.d(TAG, "onReceive: received the network state changed action");
                    //Log.d(TAG, "onReceive: isMaster "+isMaster);
                    //Log.d(TAG, "onReceive: ssid "+wifiInfo.getSSID());
                    //if(slaveConnectThread == null)
                    //{
                    //    Log.d(TAG, "onReceive: slaveConnectThread is null");
                    //}
                    //Log.d(TAG, "onReceive: charChoosed "+charChoosed);
                    if( (!isMaster) && (wifiInfo.getSSID().equals("\""+MASTER_SSID+"\"")) && (slaveConnectThread == null) && charChoosed)
                    {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try
                                {
                                    DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                                    final String serverAddress = intToIp(dhcpInfo.serverAddress);
                                    Log.d("MainActivity","on Broadcast Receive: Connected AP ID:"+serverAddress);
                                    Socket socket = new Socket(serverAddress, PORT);
                                    slaveConnectThread = new ConnectThread(socket, mainHandler);
                                    slaveConnectThread.start();
                                } catch (IOException e) {
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
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                else {
                    Toast.makeText(this,"You denied the permission",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    /**
     * 选择角色，角色选择之后不能再改变
     * 如果选择了master将出现 START KEYGEN ROCEDURE 按钮
     * 按下该按钮，密钥生成程序将启动
     * @param view
     */
    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        switch (id)
        {
            case R.id.master_button:
                if(!charChoosed)
                {
                    isMaster = true;
                    charChoosed = true;
                    character.setText("Master");
                    start.setVisibility(View.VISIBLE);

                    if(isWifiApOpened())
                    {
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
                if(!charChoosed)
                {
                    isMaster = false;
                    charChoosed = true;
                    character.setText("Slave");
                    if(isConnected2Master())
                    {
                        new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
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

                if(isMaster)
                {
                    if(!isWifiApOpened())
                    {
                        Toast.makeText(MainActivity.this,"请打开MASTER热点",Toast.LENGTH_SHORT).show();
                    }
                    if(masterConnectThread == null)
                    {
                        Toast.makeText(MainActivity.this,"等待slave设备连接",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        if(!ntpSyncRequested)
                        {
                            if(masterConnectThread.threadHandler == null)
                            {
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

    /**
     * 注册广播接收器
     */
    private void initBroadcastReceiver()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");

        registerReceiver(receiver,intentFilter);
    }


    /**
     * 判断wifi热点是否开启
     * @return
     */
    public boolean isWifiApOpened()
    {
        try
        {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            int state = (int)method.invoke(wifiManager);
            Field field = wifiManager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            int value = (int)field.get(wifiManager);
            if (state == value)
            {
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private class MainHandler extends Handler
    {
        WeakReference<MainActivity> mActicity;
        public MainHandler(MainActivity activity){
            this.mActicity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message message){
            //MainActivity mainActivity = mActicity.get();
            super.handleMessage(message);

            switch (message.what){
                // (master) ListenerThread -> MainActivity
                case DEVICE_CONNECTING:
                    Log.d("MainActivity","DEVICE_CONNECTING message is received");
                    masterConnectThread = new ConnectThread(listenerThread.getSocket(),mainHandler);
                    masterConnectThread.start();
                    break;
                // (slave) ConnectThread -> MainActivity
                case NTP_SYNC_REQUEST_INT:
                    Log.d(TAG, "handleMessage: master says: sync the NTP\n");
                    textView.append("master says: sync the NTP\n");
                    slaveConnectThread.threadHandler.sendMessage(getMessage(NTP_SYNC_SUCCESS));
                    break;
                //  (master) ConnectThread -> MainActivity
                case NTP_SYNC_SUCCESS_INT:
                    if(ntpSyncSucceed){
                        //master自己这边也开始密钥生成步骤
                        textView.append("slave says: sync succeeded\n");
                        //发送消息让slave也开始生成密钥
                        masterConnectThread.threadHandler.sendMessage(getMessage(KEY_GEN_START));
                        Log.d("MainActivity", "handleMessage: master start generating the key");
                        KeyGenThread keyGenThread = new KeyGenThread(mainHandler);
                        keyGenThread.start();
                        progressTextView.setText("Master is on key gen,please wait...");
                    }
                    break;
                //目前未考虑
                case NTP_SYNC_FAILED_INT:
                    ntpSyncSucceed = false;
                    ntpSyncRequested = false;
                    textView.append("slave says: he failed sync NTP\n");
                    break;
                // (slave)
                case KEY_GEN_START_INT:
                    textView.append("master says:ken gen start\n");
                    KeyGenThread keyGenThread = new KeyGenThread(mainHandler);
                    keyGenThread.start();
                    progressTextView.setText("Slave is on key gen,please wait...");
                    break;
                // (slave / master) KeyGenThread -> MainActivity
                case KEY_GEN_SUCCESS_INT:
                    Log.d(TAG, "handleMessage: isMaster: " + isMaster);
                    if(isMaster)
                    {
                        MyApplication.setfinalKey(MyApplication.getRawKey());
                    }
                    //rawKey = message.obj.toString();
                    Log.d(TAG, "rawkey:" + MyApplication.getRawKey() + "\n");
                    keyTextView.setText(MyApplication.getRawKey());
                    progressTextView.setText("The key has been genearated");
                    if(!isMaster){
                        //slave 将向master发送密钥生成完毕消息
                        Log.d("MainActivity","slave key gen succeed");
                        slaveConnectThread.threadHandler.sendMessage(getMessage(KEY_GEN_FINISHED));
                    }
                    break;
                // (master)
                case KEY_GEN_FINISHED_INT:

                    textView.append("slave says: he finished the key gen procedure\n");
                    //开始信息协调程序
                    Log.d(TAG, "master is sendding the syndrome to slave");
                    Log.d(TAG,"rawKey: "+MyApplication.getRawKey());
                    while (MyApplication.getRawKey() == null){
                        Log.d(TAG, "raw key is null");
                    }

                    GenericGFPoly[] masterSyndrome= Utils2.getSyndromePoly(MyApplication.getRawKey());
                    StringBuilder syndromesStr = new StringBuilder();
                    for(int i=0;i<3;i++){
                        textView.append(masterSyndrome[i].toString()+"\n");
                        int[] coefficients = masterSyndrome[i].getCoefficients();
                        Log.d(TAG, "the length of alice syndrome's coefficients: " + coefficients.length);
                        int[] padding = new int[12];
                        int distance = 12-coefficients.length;
                        for(int j=0;j<coefficients.length;j++){
                            padding[distance+j] = coefficients[j];
                        }
                        String coefficientsStr = Utils2.int2String(padding);
                        syndromesStr.append(coefficientsStr);
                    }

                    if(syndromesStr != null){
                        Message syndromeMessage = getMessage("Syndromes:"+syndromesStr);
                        masterConnectThread.threadHandler.sendMessage(syndromeMessage);
                    }
                    progressTextView.setText("Infomation reconciliation is starting");
                    break;
                //没有用到这个分支
                case INFO_RECON_START_INT:
                    textView.append("master says:information reconciliation start\n");
                    //开始信息协调程序
                    progressTextView.setText("All finished");
                    //ntpSyncRequested=false;
                    //ntpSyncSucceed = false;
                    break;
                // (slave)
                case SYNDROME_INT:
                    textView.append("master send the syndromes\n");
                    String masterSyndromStr = message.obj.toString();
                    Log.d("MainActivity", "masterSyndrome:"+masterSyndromStr);
                    String newKey = "";
                    try {
                        newKey = infoReconciliation(masterSyndromStr);
                    }catch (Exception e){
                        e.printStackTrace();
                        progressTextView.append(e.getMessage());
                    }
                    Log.d("MainActivity", "newkey:"+newKey);
                    MyApplication.setfinalKey(newKey);
                    newKeyTextView.setText(MyApplication.getFinalKey());
                    //将信息协调之后的新密钥发送给master进行确认，这一步需要改进，可用MAC
                    String digest = Utils2.getMACDigest(KEY_CONFIRM_MESSAGE);
                    textView.append(digest);
                    Message keyConfirm = getMessage("KeyConfirm:"+digest);
                    slaveConnectThread.threadHandler.sendMessage(keyConfirm);
                    break;
                // (master)
                case KEY_CONFIRM_INT:
                    textView.append("slave send the new key to confirm\n");
                    String bobDigest = message.obj.toString();
                    textView.append(bobDigest + "\n");
                    Log.d("MainActivity", "bobDigest:"+bobDigest);
                    String aliceDigest = Utils2.getMACDigest(KEY_CONFIRM_MESSAGE);
                    textView.append(aliceDigest + "\n");
                    textView.append("I'm the test string");
                    Log.d("MainActivity", "aliceDigest:"+aliceDigest);
                    Message pairingResult;
                    if(aliceDigest.trim().equals(bobDigest)){
                        progressTextView.setText("Key is same");
                        pairingResult = getMessage(PAIRING_SUCCESS);
                        MyApplication.setParingStatus(true);
                    }
                    else{
                        progressTextView.setText("Key is different");
                        pairingResult = getMessage(PAIRING_FAILED);

                    }
                    masterConnectThread.threadHandler.sendMessage(pairingResult);
                    masterConnectThread.threadHandler.sendMessage(getMessage(END));
                    break;
                case PAIRING_SUCCESS_INT:
                    progressTextView.setText("Key is same");
                    MyApplication.setParingStatus(true);
                    break;
                case PAIRING_FAILED_INT:
                    progressTextView.setText("Key is different");
                    break;
            }
        }
    }

    /**
     * 将输入字符串装在进Message对象中返回
     * @param msgString
     * @return
     */
    private Message getMessage(String msgString){
        Message msg = Message.obtain();
        msg.what = MSG_SEND;
        msg.obj = msgString;
        return msg;
    }


    /**
     * 输入master的syndrome String
     * 输出信息协调后的密钥
     * @param syndrome
     * @return
     */
    public String infoReconciliation(String syndrome) throws Exception
    {
        StringBuffer newKey = new StringBuffer();
        int[] syndromeInts = Utils2.string2Ints(syndrome);
        int count = syndromeInts.length;
        int len = count/3;
        Log.d(TAG, "infoReconciliation: syndrome block len: "+len);
        int[] aliceSyndromeInt1 = new int[len];
        int[] aliceSyndromeInt2 = new int[len];
        int[] aliceSyndromeInt3 = new int[len];
        for(int i=0;i<len;i++){
            aliceSyndromeInt1[i] = syndromeInts[i];
        }
        for(int i=0;i<len;i++){
            aliceSyndromeInt2[i] = syndromeInts[len+i];
        }
        for(int i=0;i<len;i++){
            aliceSyndromeInt3[i] = syndromeInts[len+len+i];
        }
        GenericGFPoly[] aliceSyndromes = new GenericGFPoly[3];
        aliceSyndromes[0] = new GenericGFPoly(GenericGF.AZTEC_PARAM,aliceSyndromeInt1);
        aliceSyndromes[1] = new GenericGFPoly(GenericGF.AZTEC_PARAM,aliceSyndromeInt2);
        aliceSyndromes[2] = new GenericGFPoly(GenericGF.AZTEC_PARAM,aliceSyndromeInt3);
        textView.append(aliceSyndromes[0].toString()+"\n");
        textView.append(aliceSyndromes[1].toString()+"\n");
        textView.append(aliceSyndromes[2].toString()+"\n");
        System.out.println("Bob rawKey: " + MyApplication.getRawKey());
        //GenericGFPoly[] bobSyndromes = Utils2.getSyndromePoly(rawKey);

        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
        //将bob/slave的rawKey分成3组int数组
        int[] slaveKey = new int[45];
        for(int i=0;i<32;i++)
        {
            String tmpStr = MyApplication.getRawKey().substring(i*4,(i+1)*4);
            slaveKey[i] = Integer.parseInt(tmpStr,2);
        }
        for(int i=32;i<45;i++)
        {
            slaveKey[i] = 0;
        }
        int[] rawKey1 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey1[i] = slaveKey[i];
        }
        int[] rawKey2 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey2[i] = slaveKey[15+i];
        }
        int[] rawKey3 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey3[i] = slaveKey[30+i];
        }
        // 每一个aliceSyndrome对应一个bobKey的数组进行信息协调

        String result1 = Utils2.int2String(decoder.myDecode(aliceSyndromes[0], rawKey1, 12));
        String result2 = Utils2.int2String(decoder.myDecode(aliceSyndromes[1], rawKey2, 12));
        String result3 = Utils2.int2String(decoder.myDecode(aliceSyndromes[2], rawKey3, 12));

        newKey.append(result1);
        newKey.append(result2);
        newKey.append(result3);


        if(newKey == null)
        {
            Log.d(TAG, "infoReconciliation: newKey is null");
            newKey.append("newKey is null");
        }
        System.out.println("Bob newKey: " + newKey.substring(0,128));
        return newKey.substring(0,128);

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.d(TAG+"lifeCircle", "onStart");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG+"lifeCircle", "onResume");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG+"lifeCircle", "onPause");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d(TAG+"lifeCircle", "onStop");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d(TAG+"lifeCircle", "onDestroy");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    /**
     * 判断slave是否连接上master的热点
     * @return
     */
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

    /*
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
    */

    /**
     * DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
     * final String serverAddress = intToIp(dhcpInfo.ipAddress);
     * 将int类型的IP地址转化为点分十进制
     * @param ipInt
     * @return
     */
    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
}

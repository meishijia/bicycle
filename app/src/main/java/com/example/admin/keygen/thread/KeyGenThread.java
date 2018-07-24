package com.example.admin.keygen.thread;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.admin.keygen.application.MyApplication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.admin.keygen.activity.MainActivity.KEY_GEN_SUCCESS_INT;

public class KeyGenThread extends Thread {

    private StringBuilder sampleData = new StringBuilder("");
    private String accPath;
    private String accDenoisedPath;
    private int count=0;
    private static final String TAG = "KeyGenThread";

    private SensorManager sm;
    private int sensorType;
    private  Handler mHandler;

    static {
        System.loadLibrary("native-lib");
    }

    public  KeyGenThread(Handler handler){
        this.mHandler = handler;
    }

    @Override
    public void run(){
        Log.d(TAG, "key gen thread is running");
        sm = (SensorManager) MyApplication.getContext().getSystemService(Context.SENSOR_SERVICE);
        sensorType = Sensor.TYPE_LINEAR_ACCELERATION;
        startSampleThenKeyGen();

    }

    private final SensorEventListener myAccelerometerListener = new SensorEventListener(){

        //复写onSensorChanged方法
        public void onSensorChanged(SensorEvent sensorEvent){
            if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){

                //Log.i(TAG,"onSensorChanged");

                float X_lateral = sensorEvent.values[0];
                float Y_longitudinal = sensorEvent.values[1];
                float Z_vertical = sensorEvent.values[2];

                float f[]={X_lateral,Y_longitudinal,Z_vertical};

                //写入时统一用float，独读出时统一用double
                sampleData.append(f[2]+"\r\n");
                count++;
                if (count == 100)
                {
                    Log.d(TAG,"采样12800次");
                    sm.unregisterListener(myAccelerometerListener);
                    writeData2File(sampleData, accPath);
                    wden(4,9,accPath,accDenoisedPath);
                    String raw_key = startKeyGen();
                    Message message = Message.obtain();
                    message.what = KEY_GEN_SUCCESS_INT;
                    message.obj = raw_key;
                    mHandler.sendMessage(message);

                }

            }
        }
        //复写onAccuracyChanged方法
        public void onAccuracyChanged(Sensor sensor , int accuracy){
            //Log.i(TAG, "onAccuracyChanged");
        }
    };

    private void startSampleThenKeyGen(){
        Log.d(TAG, "sample procedure started");
        //Toast.makeText(SampleActivity.this,"AccStartBtn is pressed!",Toast.LENGTH_SHORT).show();
        //20Hz=50000,50Hz=20000 100Hz=10000
        sm.registerListener(myAccelerometerListener, sm.getDefaultSensor(sensorType), 10000);
        int s_len = sampleData.length();
        sampleData.delete(0, s_len);
        count = 0;
        accPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download" + File.separator + getTime() + ".txt";
        accDenoisedPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download" + File.separator + getTime()+"_denoided.txt";
        Log.d(TAG, "String acc path:" + accPath);

    }

    public String getTime(){
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
        String time = simpleDateFormat.format(date);
        return time;
    }
    protected void writeData2File(StringBuilder data,String filename){
        try {
            File file = new File(filename);
            //Toast.makeText(SampleActivity.this,"文件写入中...",Toast.LENGTH_SHORT).show();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(data.toString());
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public native void wden(int scale,int dbn,String srcfile,String dstfile);

    private String startKeyGen(){

        String key_str="";

        try{
            float[] signal = load(accDenoisedPath);
            float[] normalized_z = mapminmax(signal, -10f, 10f);
            float[] abs_z = abs(normalized_z);
            int[] bin_z = bin_thre(abs_z, 2);
            float[] win_z = window(bin_z);
            int[] key = bin_thre(win_z, (float) 0.6);
            key_str = new String(toString(key));
            Log.i(TAG, "key_str: " + key_str);

        }catch (Exception e){
            e.printStackTrace();
        }
        return key_str;

    }


    protected float[] load(String filename){
        int i=0;
        float[] data = new float[12800];
        try{
            File file = new File(filename);
            BufferedReader bufread;
            String read;
            bufread = new BufferedReader(new FileReader(file));
            while((read=bufread.readLine()) != null){
                data[i] = Float.parseFloat(read);
                i++;
            }
            bufread.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        Log.d(TAG,Integer.toString(i));
        return data;
    }
    protected float[] mapminmax(float[] x, float ymin,float ymax){
        float[] y = new float[x.length];
        float xmin = min(x);
        float xmax = max(x);
        for(int i=0;i<x.length;i++){
            y[i] = ( (ymax-ymin)*(x[i]-xmin) ) / (xmax-xmin) + ymin;
        }
        return y;
    }

    protected float min(float[] x){
        float tmp = x[0];{
            for(int i=1;i<x.length;i++){
                if(x[i] < tmp){
                    tmp = x[i];
                }
            }
        }
        return tmp;
    }
    protected float max(float[] x){
        float tmp = x[0];{
            for(int i=1;i<x.length;i++){
                if(x[i] > tmp){
                    tmp = x[i];
                }
            }
        }
        return tmp;
    }

    protected float[] abs(float[] x){
        float[] y = new float[x.length];
        for(int i=0;i<x.length;i++){
            y[i] = Math.abs(x[i]);
        }
        return y;
    }

    protected int[] bin_thre(float[] x,float thres){
        int[] y= new int[x.length];
        for(int i=0;i<x.length;i++){
            if(x[i] >= thres){
                y[i] = 1;
            }
            else y[i] = 0;
        }
        return y;
    }

    protected float[] window(int[] x){
        float[] y=new float[128];
        for(int i=0;i<128;i++){
            float sum = 0;
            for(int j=0;j<100;j++){
                sum += x[100*i+j];
            }
            y[i] = sum / (float) 100;
            //Log.i(TAG,Float.toString(y[i]));
        }
        return y;
    }

    protected static StringBuilder toString(int[] x)
    {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<x.length;i++)
        {
            sb.append(x[i]);
        }
        return sb;
    }


}

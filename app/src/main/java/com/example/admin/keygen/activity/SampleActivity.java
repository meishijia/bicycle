package com.example.admin.keygen.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.keygen.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SampleActivity extends AppCompatActivity{

    TextView min;
    TextView sec;

    TextView accTextView;

    StringBuilder sampleData = new StringBuilder("");
    String accPath;
    String accDenoisedPath;

    File file;

    MyHandler myHandler;
    long timeUsedInSec;


    int count=0;

    //设置LOG标签
    private static final String TAG = "SampleActivity";

    private SensorManager sm;
    int sensorType;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        /**
         * 控件初始化
         */
        initView();
        startSample();
    }

    public void initView() {

        min = (TextView) findViewById(R.id.min);
        sec = (TextView) findViewById(R.id.sec);
        accTextView = (TextView) findViewById(R.id.accTextView);

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorType = Sensor.TYPE_LINEAR_ACCELERATION;

        myHandler = new MyHandler(this);

    }

    final SensorEventListener myAccelerometerListener = new SensorEventListener(){

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
                if (count == 12800)
                {
                    Toast.makeText(SampleActivity.this,"已采样12800次",Toast.LENGTH_LONG).show();
                    Log.i(TAG,"采样12800次");
                    sm.unregisterListener(myAccelerometerListener);
                    accTextView.setText("已保存文件！");
                    myHandler.sendEmptyMessage(0);

                    writeData2File(sampleData, accPath);
                    wden(4,9,accPath,accDenoisedPath);
                    Intent key_gen = new Intent(SampleActivity.this, KeyGenActivity.class);
                    key_gen.putExtra("accFilePath", accDenoisedPath);
                    startActivity(key_gen);
                }

            }
        }
        //复写onAccuracyChanged方法
        public void onAccuracyChanged(Sensor sensor , int accuracy){
            //Log.i(TAG, "onAccuracyChanged");
        }
    };


    private void startSample(){
        Log.i(TAG, "AccStartBtn is pressed!");
        //Toast.makeText(SampleActivity.this,"AccStartBtn is pressed!",Toast.LENGTH_SHORT).show();
        //20Hz=50000,50Hz=20000 100Hz=10000
        sm.registerListener(myAccelerometerListener, sm.getDefaultSensor(sensorType), 10000);
        int s_len = sampleData.length();
        sampleData.delete(0, s_len);
        count = 0;
        accPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download" + File.separator + getTime() + ".txt";
        accDenoisedPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download" + File.separator + getTime()+"_denoided.txt";
        Log.i(TAG, "String acc path:" + accPath);
        file = new File(accPath);
        min.setText("00");
        sec.setText("00");
        timeUsedInSec = 0;
        myHandler.sendEmptyMessage(1);

    }

    public void onPause(){
        /*
         * 很关键的部分：注意，说明文档中提到，即使activity不可见的时候，感应器依然会继续的工作，测试的时候可以发现，没有正常的刷新频率
         * 也会非常高，所以一定要在onPause方法中关闭触发器，否则讲耗费用户大量电量，很不负责。
         * */
        sm.unregisterListener(myAccelerometerListener);
        super.onPause();
    }

    public String getTime(){
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
        String time = simpleDateFormat.format(date);
        return time;
    }

    static class MyHandler extends Handler{
        WeakReference<SampleActivity> mActivity;
        private MyHandler(SampleActivity activity){
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            SampleActivity sampleActivity = mActivity.get();
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    // 添加更新ui的代码
                    if (true) {
                        sampleActivity.updateView();
                        sampleActivity.myHandler.sendEmptyMessageDelayed(1, 1000);
                    }
                    break;
                case 0:
                    break;
            }
        }

    }

    private void updateView() {
        timeUsedInSec += 1;
        int minute = (int) (timeUsedInSec / 60)%60;
        int second = (int) (timeUsedInSec % 60);
        if (minute < 10) {
                min.setText("0" + minute);
        }
        else {
            min.setText("" + minute);
        }
        if (second < 10) {
            sec.setText("0" + second);
        }
        else {
            sec.setText("" + second);
        }
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
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void wden(int scale,int dbn,String srcfile,String dstfile);
}

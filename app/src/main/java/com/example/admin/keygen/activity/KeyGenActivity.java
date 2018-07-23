package com.example.admin.keygen.activity;

import android.content.Intent;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.admin.keygen.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;


public class KeyGenActivity extends AppCompatActivity implements View.OnClickListener {

    private float[][] acc_data = null;
    private float[] acc_x = null;
    private float[] acc_y = null;
    private float[] acc_z = null;

    TextView keyTextView;
    Button startKeyGen;

    String accFilePath;
    String key_str;
    private static final String TAG = "KeyGenActivity";

    MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_gen);
        initView();
    }
    protected void initView(){

        keyTextView = (TextView)findViewById(R.id.key_text_view);
        startKeyGen = (Button)findViewById(R.id.startKeyGen);
        startKeyGen.setOnClickListener(this);

        myHandler = new MyHandler(KeyGenActivity.this);

        Intent intent = getIntent();
        accFilePath = intent.getStringExtra("accFilePath");

    }

    @Override
    public void onClick(View view){

        int id = view.getId();
        if(id == R.id.startKeyGen){
            try {

                Log.i(TAG, "KeyGenBtn is pressed!");
                float[] signal = load(accFilePath);
                float[] normalized_z = mapminmax(signal, -10f, 10f);
                float[] abs_z = abs(normalized_z);
                int[] bin_z = bin_thre(abs_z, 2);
                float[] win_z = window(bin_z);
                int[] key = bin_thre(win_z, (float) 0.6);
                key_str = new String(toString(key));
                Log.i(TAG, "key_str: " + key_str);
                myHandler.sendEmptyMessage(1);

            }catch (Exception e){
                e.printStackTrace();
                Log.e(TAG,e.getMessage());
            }

        }
    }



    /*
    protected float[][] load(String filename){
        float[][] acc_data = null;
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        int count = 0;
        try{
            in = openFileInput(filename);
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while((line=reader.readLine() )!= null){
                String[] tmp = line.split("\t");
                acc_data[0][count] = Float.valueOf(tmp[0]);
                acc_data[1][count] = Float.valueOf(tmp[1]);
                acc_data[2][count] = Float.valueOf(tmp[2]);
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if(reader!=null){
                try{
                    reader.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            return acc_data;
        }
    }
    */


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

    static class MyHandler extends Handler {
        WeakReference<KeyGenActivity> mActivity;
        private MyHandler(KeyGenActivity activity){
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            KeyGenActivity keyGenActivity = mActivity.get();
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    // 添加更新ui的代码
                    Log.i(TAG,"Handling the massage...");
                    keyGenActivity.setKey(keyGenActivity.key_str);
                    break;
                case 0:
                    break;
            }
        }

    }

    private void setKey(String key){
        Log.i(TAG,"Setting the key...");
        keyTextView.setText(key);
    }
}

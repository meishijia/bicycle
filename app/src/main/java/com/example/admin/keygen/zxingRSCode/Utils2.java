package com.example.admin.keygen.zxingRSCode;

import android.util.Base64;

import com.example.admin.keygen.application.MyApplication;

import java.security.Key;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Utils2
{
    public static GenericGFPoly[] getSyndromePoly(String rawKey)
    {
        int[] rawKeyInt = new int[45];
        for(int i=0;i<32;i++)
        {
            String tmpStr = rawKey.substring(i*4,(i+1)*4);
            rawKeyInt[i] = Integer.parseInt(tmpStr,2);
        }
        for(int i=32;i<45;i++)
        {
            rawKeyInt[i] = 0;
        }
        int[] rawKey1 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey1[i] = rawKeyInt[i];
        }
        int[] rawKey2 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey2[i] = rawKeyInt[15+i];
        }
        int[] rawKey3 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey3[i] = rawKeyInt[30+i];
        }

        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
        GenericGFPoly[] syndromes = new GenericGFPoly[3];
        syndromes[0] = decoder.getSyndromes(rawKey1,12);
        syndromes[1] = decoder.getSyndromes(rawKey2,12);
        syndromes[2] = decoder.getSyndromes(rawKey3,12);

        return syndromes;
    }

    public static String int2String(int[] data)
    {
        StringBuffer result=new StringBuffer("");
        for(int i=0;i<data.length;i++) {
            String tmp = Integer.toBinaryString(data[i]);
            int tmpLength = tmp.length();
            if(tmpLength < 4) {
                StringBuffer sb = new StringBuffer("");
                for(int j=0;j<4-tmpLength;j++) {
                    sb.append("0");
                }
                result.append(sb.toString()+tmp);
            }
            else {
                result.append(tmp);
            }
        }
        //System.out.println(result.toString());
        return result.toString();
    }

    public static int[] string2Ints(String syndrome)
    {
        int strLength = syndrome.length();
        int count = strLength/4;
        int[] syndromeBytes = new int[count];
        for(int i=0;i<count;i++){
            String tmp = syndrome.substring(i*4,(i+1)*4);
            syndromeBytes[i] = Integer.parseInt(tmp,2);
        }
        return syndromeBytes;
    }

    public static String getMACDigest(String data)
    {
        String resultStr = "";
        try
        {
            Key key = new SecretKeySpec(MyApplication.getFinalKey().getBytes("UTF-8"),"");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] dataBytes = data.getBytes("UTF-8");
            byte[] result = mac.doFinal(dataBytes);
            resultStr = Base64.encodeToString(result,Base64.DEFAULT);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return resultStr;
    }
}

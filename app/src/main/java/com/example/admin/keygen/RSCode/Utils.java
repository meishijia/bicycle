package com.example.admin.keygen.RSCode;

import android.util.Log;


public class Utils {

    public final static String TAG = "Utils";

    /**
     *
     * @param syndrome 来自alice的syndrome字符串
     * @return 每四位转成一个数值，以byte类型表示，返回byte数组
     */
    public static byte[] string2Bytes(String syndrome){
        int strLength = syndrome.length();
        int count = strLength/4;
        byte[] syndromeBytes = new byte[count];
        for(int i=0;i<count;i++){
            String tmp = syndrome.substring(i*4,(i+1)*4);
            syndromeBytes[i] = (byte)Integer.parseInt(tmp,2);
        }
        return syndromeBytes;
    }

    /**
     * 如[1,2,3] -> 000100100011
     * @param bytes 数组，值是域GF(16)中的元素
     * @return 将数组中的每个元素转化为4位二进制表示的形式拼接成字符串
     */
    public static String bytes2String(byte[] bytes){
        StringBuffer result=new StringBuffer("");
        for(int i=0;i<bytes.length;i++) {
            String tmp = Integer.toBinaryString(bytes[i]);
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

    /**
     * 将Polynomial数组中的所有Polynomial表示成字符串
     * 主要用于alice向bob发送自己的syndrome
     * alice的rawkey是128位，RS(15,3)每个block的长度为15*4=60，因此要协调完所有的密钥
     * 需要协调三次，即需要向bob发送3次syndrome
     * @param polys
     * @return
     */
    public static String polynomials2String(Polynomial16[] polys){
        int count = polys.length;
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<count;i++){
            sb.append(bytes2String(polys[i].toBytes()));
        }
        return sb.toString();
    }

    /**
     * 将单个多项式转化为字符串形式
     * @param poly
     * @return
     */
    public static String singlePoly2String(Polynomial16 poly){
        String result = bytes2String(poly.toBytes());
        Log.d(TAG, "singlePoly2String: result" + result);
        return result;
    }


    /**
     * 将一个128位的密钥转换成3个Polynomial
     * @param rawKey
     * @return
     */
    public static Polynomial16[] rawKey2Polynomials(String rawKey){
        Log.d(TAG, "rawKey: "+rawKey);
        if(rawKey == null){
            Log.d(TAG, "rawKey2Polynomials: rawKey is null");
            return null;
        }
        byte[] rawByteKey = new byte[45];

        for(int i=0;i<32;i++){
            String masterTmpStr = rawKey.substring(i*4,(i+1)*4);
            rawByteKey[i] = (byte)Integer.parseInt(masterTmpStr,2);
        }
        for(int i=32;i<45;i++){
            rawByteKey[i] = 0;
        }
        byte[] rawkey1 = new byte[15];
        byte[] rawkey2 = new byte[15];
        byte[] rawkey3 = new byte[15];
        for(int i=0;i<15;i++){
            rawkey1[i] = rawByteKey[i];
        }
        for(int i=0;i<15;i++){
            rawkey2[i] = rawByteKey[15+i];
        }
        for(int i=0;i<15;i++){
            rawkey3[i] = rawByteKey[30+i];
        }

        Polynomial16[] polynomials = new Polynomial16[3];
        polynomials[0] = new Polynomial16(rawkey1);
        Log.d(TAG, "rawKey2Polynomials: rawKeyPoly1 "+polynomials[0].toString());
        polynomials[1] = new Polynomial16(rawkey2);
        Log.d(TAG, "rawKey2Polynomials: rawKeyPoly2 "+polynomials[1].toString());
        polynomials[2] = new Polynomial16(rawkey3);
        Log.d(TAG, "rawKey2Polynomials: rawKeyPoly3 "+polynomials[2].toString());
        return polynomials;
    }

    /**
     * 将密钥的多项式组，分别计算syndrome多项式
     * @param polynomials
     * @return
     */
    public static Polynomial16[] getSyndromePolynomials(Polynomial16[] polynomials){
        RSDecoder16 decoder = new RSDecoder16();
        Polynomial16 syndromes1 = decoder.calSyndromes(polynomials[0]);
        Log.d(TAG, "getSyndromePolynomials: [1] "+syndromes1.toString());
        Polynomial16 syndromes2 = decoder.calSyndromes(polynomials[1]);
        Log.d(TAG, "getSyndromePolynomials: [2]"+syndromes2.toString());
        Polynomial16 syndromes3 = decoder.calSyndromes(polynomials[2]);
        Log.d(TAG, "getSyndromePolynomials: [3]"+syndromes3.toString());
        Polynomial16[] syndromePolynomials = {syndromes1,syndromes2,syndromes3};
        return syndromePolynomials;
    }




}

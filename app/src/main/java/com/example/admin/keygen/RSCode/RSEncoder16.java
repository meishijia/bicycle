package com.example.admin.keygen.RSCode;

import java.nio.ByteBuffer;

public class RSEncoder16 extends RSCode16{
    /**
     * 实际上在信息协调的时候用不到编码步骤
     * 将提取的密钥看做编码好之后的，因此
     * 15个symbol为一个block
     */
    /**
     * 构造函数 默认为RS(15,3)
     */
    public RSEncoder16() {
        super();
    }

    /**
     * RS编码
     * @param message 信息字符串
     * @return 已编码的字节数组
     */
    public byte[] encode(String message){
        //补足输入字符串
        String enlargedMessage=enlargeMessage(message);
        //计算RS编码的单元数:3字节为一个单元
        int unitCount=enlargedMessage.length()/this.k;
        ByteBuffer buffer=ByteBuffer.allocate(unitCount*this.n);
        for(int i=0;i<unitCount;i++){
            //逐个单元进行编码
            Polynomial16 poly=doEncode(enlargedMessage.substring(i*this.k, (i+1)*this.k));
            //将编码得到的码字多项式转换成字节形式，投入buffer中
            buffer.put(poly.toBytes());
        }
        return buffer.array();
    }

    /**
     * 对一个编码单元(大小为成员变量k)执行具体的编码操作
     * @param unit 信息字符串
     * @return 码字多项式
     */
    private Polynomial16 doEncode(String unit){
        //生成信息多项式m(x)
        Polynomial16 m=new Polynomial16(unit);
        //x^(2t)
        Polynomial16 twoT=new Polynomial16(this.two_t);
        twoT.coefficients[twoT.length()-1]=ONE;
        //用x^2t乘以m(x)
        Polynomial16 mprime=m.mul(twoT);
        //得到余式b(x)
        Polynomial16 b=mprime.mod(this.g);
        //生成码字多项式
        Polynomial16 c=mprime.sub(b);
        return c;
    }

    /**
     * 补足字符串长度，使其正好为成员变量k的整数倍
     * @param message
     * @return
     */
    public  String enlargeMessage(String message){
        //例如message长度是400，长度应为223＊2=446，后面添加46个零
        int rawLen=message.length();
        int unitCount=(int)Math.ceil(rawLen/(double)this.k);
        //得到补足之后的长度
        int len=this.k*unitCount;
        //如果原始长度不够，将补足的部分添0
        if(len!=rawLen){
            byte[] rawBytes=message.getBytes();
            byte[] bytes=new byte[len];
            for(int i=0;i<rawBytes.length;i++){
                bytes[i]=rawBytes[i];
            }
            for(int i=rawBytes.length;i<len;i++){
                bytes[i]=0;
            }
            return new String(bytes);
        }
        //原始长度正好为成员变量k的整数倍，直接返回
        return message;
    }
}

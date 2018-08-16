package com.example.admin.keygen.RSCode;

import java.nio.charset.StandardCharsets;

/**
 * 该类为表示系数为GF16的多项式
 * 拥有加减乘除等多项式运算方法
 */
public class Polynomial16 {
    //用于比较或中间运算 不可用于赋值 赋值应该新分配空间
    private final static GF16 ZERO = new GF16(0);

    // 多项式的系数
    public GF16[] coefficients;

    // 8x^2+3x^3+9x^5 --> [0,0,8,3,0,9]
    public Polynomial16(GF16[] coefficients) {
        this.coefficients = coefficients;
    }

    // ascii array to Polynomial(steps down)
    public Polynomial16(String string) {
        this(string.getBytes(StandardCharsets.US_ASCII));
    }

    public Polynomial16(byte[] bytes) {
        this.coefficients = new GF16[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            // bytes倒序转换为多项式的系数
            //把因为超过128变成负值的，还原成正值
            //但是在GF(16)中值不会超过256
            int posValue=bytes[i]>=0?bytes[i]:bytes[i]+256;
            //System.out.println("posValue"+posValue);
            this.coefficients[this.coefficients.length - 1 - i] = new GF16(
                    posValue);
        }
    }

    // degree=5 [0,0,0,0,0,0]
    public Polynomial16(int degree) {
        this.coefficients = new GF16[degree + 1];
        for (int i = 0; i < degree + 1; i++) {
            // this.coefficients[i]=new GF256(128);
            this.coefficients[i] = ZERO;
        }
    }

    public int length() {
        return this.coefficients.length;
    }

    // 有一位是常数
    public int degree() {
        // 对于 [0] [0,0] [0,0,0]... 返回0
        if (isEmpty()) {
            return 0;
        }
        int degree = length() - 1;
        for (int i = length() - 1; i >= 0; i--) {
            if (this.coefficients[i].equals(ZERO)) {
                degree--;
            } else {
                break;
            }
        }
        return degree;
    }

    public Polynomial16 add(Polynomial16 b) {
        Polynomial16 a = this;
        Polynomial16 c = new Polynomial16(Math.max(a.degree(), b.degree()));
        for (int i = 0; i <= a.degree(); i++) {
            c.coefficients[i] = c.coefficients[i].add(a.coefficients[i]);
        }

        for (int i = 0; i <= b.degree(); i++) {
            c.coefficients[i] = c.coefficients[i].add(b.coefficients[i]);
        }
        return c;
    }

    public Polynomial16 sub(Polynomial16 b) {
        Polynomial16 a = this;
        Polynomial16 c = new Polynomial16(Math.max(a.degree(), b.degree()));
        for (int i = 0; i <= a.degree(); i++) {
            c.coefficients[i] = c.coefficients[i].add(a.coefficients[i]);
        }

        for (int i = 0; i <= b.degree(); i++) {
            c.coefficients[i] = c.coefficients[i].sub(b.coefficients[i]);
        }
        return c;
    }

    public Polynomial16 mul(Polynomial16 b) {
        Polynomial16 a = this;
        Polynomial16 c = new Polynomial16(a.degree() + b.degree());
        for (int i = 0; i <= a.degree(); i++) {
            for (int j = 0; j <= b.degree(); j++) {
                c.coefficients[i + j] = c.coefficients[i + j]
                        .add(a.coefficients[i].mul(b.coefficients[j]));
            }
        }
        return c;
    }

    /*
     * while(remain的阶大于b的阶){ 算出两个多项式阶的差differ sub= b * x^differ * remain最高项的系数
     * remain }
     */
    // (3x^3+5x^2+1)%(x^2+1) --> 3*x*(x^2+1) --> 3*[0,1,0,1]
    // ([1,0,1]-->[0,1,0,1])
    public Polynomial16 mod(Polynomial16 b) {
        Polynomial16 remain = this;
        while (remain.degree() >= b.degree()) {
            // differ是b与remain阶的差
            int differ = remain.degree() - b.degree();
            GF16[] curCoefs = new GF16[b.coefficients.length + differ];
            // shift 将b整体乘以x^differ 以便与remain相减
            for (int i = 0; i < differ; i++) {
                curCoefs[i] = new GF16(0);
            }
            for (int i = differ, j = 0; i < curCoefs.length; i++, j++) {
                curCoefs[i] = b.coefficients[j];
            }
            // 乘以remain最高项的系数
            for (int i = 0; i < curCoefs.length; i++) {
                curCoefs[i] = curCoefs[i].mul(remain.coefficients[remain
                        .length() - 1]);
            }
            Polynomial16 subPoly = new Polynomial16(curCoefs);
            remain = remain.sub(subPoly);
        }
        return remain;
    }

    // evaluate 也是在GF下吗？！
    public GF16 evaluate(GF16 value) {
        GF16 result = new GF16(0);
        // 例如3x^2+x+1 -->先将3加入，可以乘以x两次，达到了效果
        for (int i = degree(); i >= 0; i--) {
            result = coefficients[i].add(value.mul(result));
        }
        return result;
    }

    public boolean isEmpty() {
        for (int i = 0; i < length(); i++) {
            if (!this.coefficients[i].equals(ZERO)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Polynomial16) {
            Polynomial16 tmp = (Polynomial16) obj;
            if (tmp.length() == length()) {
                boolean isEqual = true;
                for (int i = 0; i < length(); i++) {
                    if (!tmp.coefficients[i].equals(this.coefficients[i])) {
                        isEqual = false;
                    }
                }
                return isEqual;
            }
        }
        return false;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[this.coefficients.length];
        for (int i = 0; i < this.coefficients.length; i++) {
            bytes[this.coefficients.length - 1 - i] =  (byte) coefficients[i].getValue();
        }
        return bytes;
    }

    @Override
    public String toString() {
        if (degree() == 0)
            return "" + coefficients[0].getValue();
        if (degree() == 1)
            return coefficients[1].getValue() + "x + "
                    + coefficients[0].getValue();
        String s = coefficients[degree()].getValue() + "x^" + degree();
        for (int i = degree() - 1; i >= 0; i--) {
            if (coefficients[i].getValue() == 0)
                continue;
            else if (coefficients[i].getValue() > 0)
                s = s + " + " + (coefficients[i].getValue());
            else if (coefficients[i].getValue() < 0)
                s = s + " - " + (-coefficients[i].getValue());
            if (i == 1)
                s = s + "x";
            else if (i > 1)
                s = s + "x^" + i;
        }
        return s;
    }

    public String toValue() {
        byte[] bytes = new byte[this.coefficients.length];
        for (int i = 0; i < this.coefficients.length; i++) {
            bytes[this.coefficients.length - 1 - i] = (byte) coefficients[i]
                    .getValue();
        }
        return new String(bytes);
    }

    public String toValue(int length) {
        byte[] bytes = new byte[length];
        // 计算冗余校验数据的长度
        int redundantLen = this.coefficients.length - length;
        // 输入时倒序 现在倒序输出 得到输入的字符串
        for (int i = 0; i < length; i++) {
            bytes[length - 1 - i] = (byte) coefficients[i + redundantLen]
                    .getValue();
        }
        return new String(bytes);
    }
}

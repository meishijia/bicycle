package com.example.admin.keygen.RSCode;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class RSDecoder16 extends RSCode16 {

    public final static String TAG = "RSDecoder16";

    /**
     * 构造函数 默认为RS(15,3)
     */
    public RSDecoder16() {
        super();
    }

    /**
     * RS译码
     * @param receive 收到的数据
     * @return 译码之后的字符串
     */
    public String decode(byte[] receive){
        //计算译码单元数
        int unitCount=receive.length/this.n;
        //buffer用于存放已译码的字符串
        StringBuffer buffer=new StringBuffer();
        //为了更好操纵byte，将byte[]封装成ByteBuffer bytes
        ByteBuffer bytes=ByteBuffer.wrap(receive);
        byte[] unitBytes=new byte[this.n];
        for(int i=0;i<unitCount;i++){
            //将要译码的n个字节取到unitBytes中
            bytes.get(unitBytes);
            //将译码得到的字符串投入buffer
            buffer.append(doDecode(unitBytes));
        }
        return buffer.toString().trim();
    }

    /**
     * 对一个译码单元(大小为成员变量m)执行具体的译码操作
     * @param receive 收到的数据
     * @return 译码之后的字符串
     */
    public String doDecode(byte[] receive) {
        Polynomial16 receivePoly = new Polynomial16(receive);
        // 计算伴随式
        Polynomial16 syndromes = calSyndromes(receivePoly);
        //System.out.println("syndromes: "+syndromes);
        // BM算法
        Polynomial16[] bmPolys = calBerlekampMassey(syndromes);
        Polynomial16 sigma = bmPolys[0];
        Polynomial16 omega = bmPolys[1];
        // 钱搜索
        List[] chienLists = chienSearch(sigma);
        // forney算法
        List<GF16> YList = forney(omega, (List<GF16>) chienLists[0]);
        List<Integer> jList = chienLists[1];
        GF16[] errors = new GF16[this.n];
        // 错误多项式
        for (int i = 0; i < this.n; i++) {
            if (jList.contains(i)) {
                errors[i] = YList.get(jList.lastIndexOf(i));
            } else {
                errors[i] = ZERO;
            }
        }
        Polynomial16 errorPoly = new Polynomial16(errors);
        System.out.println("error polynomial: "+errorPoly);
        Polynomial16 codeword = receivePoly.sub(errorPoly);
        return codeword.toValue(this.k);
    }

    /**
     *
     * @param alice
     * @param bob
     * @return
     */
    public byte[] myDoDecode(byte[] alice,byte[] bob) {

        Polynomial16 alicePoly = new Polynomial16(alice);
        Polynomial16 bobPoly = new Polynomial16(bob);
        System.out.println("alice polynomial:" + alicePoly);
        System.out.println("  bob polynomial:" + bobPoly);
        Polynomial16 aliceSyndrome = calSyndromes(alicePoly);
        Polynomial16 bobSyndrome = calSyndromes(bobPoly);
        Polynomial16 diffSyndrome = aliceSyndrome.sub(bobSyndrome);

        Polynomial16[] bmPolys = calBerlekampMassey(diffSyndrome);
        Polynomial16 sigma = bmPolys[0];
        Polynomial16 omega = bmPolys[1];
        List[] chienLists = chienSearch(sigma);
        List<GF16> YList = forney(omega, (List<GF16>) chienLists[0]);
        List<Integer> jList = chienLists[1];
        GF16[] errors = new GF16[this.n];
        for (int i = 0; i < this.n; i++) {
            if (jList.contains(i)) {
                errors[i] = YList.get(jList.lastIndexOf(i));
            } else {
                errors[i] = ZERO;
            }
        }
        Polynomial16 errorPoly = new Polynomial16(errors);
        System.out.println("error polynomial: "+errorPoly);
        Polynomial16 codeword = bobPoly.sub(errorPoly);
        byte[] codewordByte = codeword.toBytes();
        for(int i=0;i<codewordByte.length;i++) {
            if(alice[i] != codewordByte[i]) {
                System.out.println("You died");
                return null;
            }
        }
        System.out.println("wooooooooo");
        return codewordByte;
    }

    public Polynomial16 getErrorPoly(Polynomial16 aliceSyndrome,Polynomial16 bobSyndrome){
        Polynomial16 diffSyndrome = aliceSyndrome.sub(bobSyndrome);

        GF16[] diffSyndromeCoeffetients = diffSyndrome.coefficients;
        System.out.println("----------diffsyndrome.coeffecients--------");
        for (GF16 c : diffSyndromeCoeffetients)
        {
            System.out.print(c+" ");
        }
        System.out.println("\n");


        GF16[] errors = new GF16[this.n];
        getErrorPolyBlock:
        {
            byte[] zero = {0};
            Polynomial16 zeroPoly = new Polynomial16(zero);
            //当diffSyndrome为0的时候，说明两个密钥相同，不需要再纠错
            if(diffSyndrome.equals(zeroPoly))
            {
                for(int i=0;i<this.n;i++)
                {
                    errors[i] = ZERO;
                }
                break getErrorPolyBlock;
            }

            Polynomial16[] bmPoly = calBerlekampMassey(diffSyndrome);
            Polynomial16 sigma = bmPoly[0];
            Polynomial16 omiga = bmPoly[1];
            List[] chienLists = chienSearch(sigma);
            List<GF16> YList = forney(omiga,(List<GF16>) chienLists[0]);
            List<Integer> jList = chienLists[1];

            for (int i = 0; i < this.n; i++) {
                if (jList.contains(i)) {
                    errors[i] = YList.get(jList.lastIndexOf(i));
                } else {
                    errors[i] = ZERO;
                }
            }

        }
        Polynomial16 errorPoly = new Polynomial16(errors);
        return errorPoly;
    }


    /**
     * 因为码字c=信息多项是m*生成多项式g(x) 因此通过码字能不能除尽g(x)来判断是否需要纠错
     */
    public boolean verify(byte[] receive) {
        Polynomial16 receivePoly = new Polynomial16(receive);
        return (receivePoly.mod(this.g)).isEmpty();
    }

    /**
     * 计算伴随式
     * @param receive 收到的多项式
     * @return 伴随式多项式
     */
    public Polynomial16 calSyndromes(Polynomial16 receive) {
        Polynomial16 syndromes = new Polynomial16(this.two_t);
        syndromes.coefficients[0] = new GF16(0);
        for (int i = 1; i <= two_t; i++) {
            syndromes.coefficients[i] = receive.evaluate(this.ALPHA.pow(i));
        }
        //System.out.println("the syndromes calculated with receive:"+syndromes);
        return syndromes;
    }

    /**
     * BM算法
     * @param s 伴随式多项式
     * @return sigma错误位置多项式和omega错误值多项式组成的多项式数组
     */
    public Polynomial16[] calBerlekampMassey(Polynomial16 s) {
        GF16[] syndromes = s.coefficients;
        Polynomial16[] sigmas = new Polynomial16[this.two_t + 2];
        Polynomial16[] omegas = new Polynomial16[this.two_t + 2];
        int[] D = new int[this.two_t + 2];
        int[] JSubD = new int[this.two_t + 2];
        GF16[] deltas = new GF16[this.two_t + 2];

        // 迭代前初始化
        sigmas[0] = new Polynomial16(new GF16[] { ONE });
        sigmas[1] = new Polynomial16(new GF16[] { ONE });
        omegas[0] = new Polynomial16(new GF16[] { ZERO });
        omegas[1] = new Polynomial16(new GF16[] { ONE });
        D[0] = D[1] = 0;
        JSubD[0] = -1;
        JSubD[1] = 0;
        deltas[0] = ONE;
        deltas[1] = syndromes[1];

        // 迭代
        // j应该从0开始，因为sigma[1]/D[1]未知
        // syndromes:伴随式 syndromes[n]:x^n项的系数
        // deltas[0]:delta_-1 deltas[1]:delta_0 deltas[2]:delta_1 依次类推
        // sigmas[0]:sigma_-1 sigmas[1]:sigma_0 sigmas[2]:sigma_1 依次类推
        for (int j = 0; j < this.two_t; j++) {
            // 计算修正项 Delta_j
            int degree = sigmas[j + 1].degree();
            GF16 mid_result = ZERO;
            for (int i = 1; i <= degree; i++) {
                mid_result = mid_result.add(syndromes[j + 1 - i]
                        .mul(sigmas[j + 1].coefficients[i]));
            }
            GF16 delta = syndromes[j + 1].add(mid_result);
            deltas[j + 1] = delta;

            if (delta.equals(ZERO)) {
                sigmas[j + 2] = sigmas[j + 1];
                omegas[j + 2] = omegas[j + 1];
                D[j + 2] = D[j + 1];
                JSubD[j + 2] = JSubD[j + 1] + 1;
            } else {
                // 这里已经确保delta[j]!=0
                // JSubD[0]:-1-D(-1) JSubD[1]:0-D(0) JSubD[2]:1-D(1)依次类推
                int max = j;
                for (int i = j; i >= 0; i--) {
                    if ((JSubD[i] > JSubD[max]) && (!deltas[i].equals(ZERO))) {
                        max = i;
                    }
                }

                GF16 tmp_test = deltas[j + 1].mul(deltas[max].pow(-1));
                Polynomial16 testPoly = new Polynomial16(1);
                testPoly.coefficients[1] = tmp_test;
                sigmas[j + 2] = sigmas[j + 1].sub(testPoly.mul(sigmas[max]));
                omegas[j + 2] = omegas[j + 1].sub(testPoly.mul(omegas[max]));
                D[j + 2] = sigmas[j + 2].degree();
                JSubD[j + 2] = j + 1 - D[j + 2];
            }
        }
        //System.out.println("error-lacator polynomial--sigma: "+sigmas[this.two_t + 1]);
        //System.out.println("error-value polynomial--omega: "+omegas[this.two_t + 1]);
        Polynomial16[] results = new Polynomial16[2];
        results[0] = sigmas[this.two_t + 1];
        results[1] = omegas[this.two_t + 1];
        return results;
    }

    /**
     * 钱搜索 把"求根"换成"验根"，也就是把alpha^0-alpha^(n-1)逐一代入sigma检验
     * @param sigma 错误位置多项式,sigma的根是差错位置数的倒数
     * @return 错误位置
     */
    public List[] chienSearch(Polynomial16 sigma) {
        List[] results = new List[2];
        List<GF16> gfList = new ArrayList<GF16>();
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 1; i <= this.n; i++) {
            // 在alpha到alpha^255中找到sigma的根
            if (sigma.evaluate(ALPHA.pow(i)).equals(ZERO)) {
                // sigma根的倒数即错误位置数
                gfList.add(ALPHA.pow(-i));
                list.add(this.n - i);
            }
        }
        results[0] = gfList;
        results[1] = list;
        return results;
    }

    /**
     * forney算法
     * @param omega 错误值多项式
     * @param XList 对应的错误位置
     * @return 错误多项式的系数
     */
    public List<GF16> forney(Polynomial16 omega, List<GF16> XList) {
        int t = this.two_t / 2;
        List<GF16> YList = new ArrayList<GF16>();
        for (int i = 0; i < XList.size(); i++) {
            GF16 X = XList.get(i);
            // 计算Y的分子和分母的(1/X)
            GF16 Y = X.pow(t);
            Y = Y.mul(omega.evaluate(X.inverse()));
            Y = Y.mul(X.inverse());
            // 计算分母的求和式
            GF16 prod = new GF16(1);
            for (int j = 0; j < t; j++) {
                GF16 Xj = this.ZERO;
                if (i == j) {
                    continue;
                }
                // 0到t-1有可能超过XList的范围
                if (j < XList.size()) {
                    Xj = XList.get(j);
                }
                prod = prod.mul(X.sub(Xj));
            }
            Y = Y.mul(prod.inverse());
            YList.add(Y);
        }
        return YList;
    }

}

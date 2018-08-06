package com.example.admin.keygen.RSCode;

public class RSCode16 {

    //编码输出符号长度
    protected int n;
    //编码输入符号长度
    protected int k;
    //编码冗余长度 n-k=2t 也有资料称为2s
    protected int two_t;
    //生成多项式
    protected Polynomial16 g;

    //GF(2^4)的一个本原元
    protected final static GF16 ALPHA=new GF16(2);
    //用于比较或中间运算 不可用于赋值 赋值应该新分配空间
    protected final static GF16 ZERO=new GF16(0);
    protected final static GF16 ONE=new GF16(1);

    /**
     * 构造函数 默认为RS(255, 223)
     */
    RSCode16() {
        // TODO Auto-generated constructor stub
        this(15, 3);
    }

    /**
     * 构造函数
     * @param n 编码输出符号长度
     * @param k 编码输入符号长度
     */
    private RSCode16(int n,int k){
        this.n=n;
        this.k=k;
        this.two_t=n-k;
        //计算生成矩阵
        calGeneratorPolynomial();
    }

    public Polynomial16 getGeneratorPolynomial() {
        return g;
    }

    public int getK() {
        return k;
    }

    public int getN() {
        return n;
    }

    public int getTwoT(){
        return this.two_t;
    }

    /**
     *计算生成矩阵
     */
    private void calGeneratorPolynomial(){
        GF16 num_1=new GF16(1);
        this.g=new Polynomial16(new GF16[]{num_1});
        for(int i=1;i<=this.two_t;i++){
            Polynomial16 p=new Polynomial16( new GF16[]{ALPHA.pow(i),num_1} );
            this.g=this.g.mul(p);
        }
    }

}

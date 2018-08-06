package com.example.admin.keygen.RSCode;

public class GF16 {
    /**
     * the field element, 2 is the primitive element
     * alphaTo[i] = 2^index
     */
    private static int[] alphaTo={
            1,2,4,8,3,6,12,11,5,10,7,14,15,13,9,1
    };
    /**
     * i = 2^expOf[i]
     */
    private static int[] expOf={
            15,0,1,4,2,8,5,10,3,14,9,7,6,13,11,12
    };

    private int value;

    public GF16(int value) {
        // TODO Auto-generated constructor stub
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }

    public GF16 inverse() {
        int e = expOf[this.value];
        return new GF16(alphaTo[15-e]);
    }

    public GF16 add(GF16 other) {
        return add(this.value,other.getValue());
    }

    public GF16 sub(GF16 other){
        return add(other);
    }

    public GF16 mul(GF16 other){
        return mul(this.value, other.getValue());
    }

    public GF16 pow(int n){
        int x=expOf[this.value];
        int y=x*n;
        //-15%255=-15  超过了数组范围
        while(y<0){
            y+=15;
        }
        int z=y%15;
        return new GF16(alphaTo[z]);
    }

    private GF16 add(int a,int b){
        //伽罗华域的加法为异或
        return new GF16(a^b);
    }

    private GF16 mul(int a,int b) {
        if ((a == 0) || (b == 0)) {
            return new GF16(0);
        }
        int x = expOf[a];
        int y = expOf[b];
        int z = (x + y) % 15;
        return new GF16(alphaTo[z]);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GF16){
            if(this.value==((GF16) obj).getValue()){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return ""+this.value;
    }
}

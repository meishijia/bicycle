#include <jni.h>
#include <string>
#include <fstream>
#include <iostream>
#include "CWavelet.h"
#include "Wden.h"

using namespace std;
using namespace wavelet;

Wden::Wden(){}

void Wden::wden(int s,int n,string srcfile,string dstfile){
        ifstream waveIn(srcfile);
        ofstream waveOut(dstfile);
        double *signal = new double[12800];
        for (int i = 0; i < 12800; i++)
            waveIn >> signal[i];

        double *pDen = new double[12800];
        CWavelet cw;
        int scale = s;
        int dbn = n;
        cw.InitDecInfo(12800,scale,dbn);
        cw.thrDenoise(signal, pDen, true);

        for (int i = 0; i < 12800; i++)
            waveOut << pDen[i] << endl;

        delete[] pDen;
        pDen = NULL;
        delete[] signal;
        signal = NULL;
    }

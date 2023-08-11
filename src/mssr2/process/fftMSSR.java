/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mssr2.process;

import ij.IJ;
import net.haesleinhuepf.clij2.CLIJ2;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Scaler;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import static mssr2.process.GeneralFFT.FourierInterpD;
import net.haesleinhuepf.clij.CLIJ;

/**
 *
 * @author raul_
 */
public class fftMSSR {
    // Discret Fourier Transform
    public static ImageProcessor FourierInterp(ImageProcessor I, int amp){
        Complex[][] Ifft = fft2D(I);
        Complex[][] IfftPad = addCross(Ifft, amp);
        IfftPad = ifft2D(IfftPad);
        return getFloatM(IfftPad, amp);
    }
    public static Complex[][] fft2D(ImageProcessor I){
        float[][] IFA = I.getFloatArray();
        int x = I.getWidth();
        int y = I.getHeight();
        Complex[][] ICM = new Complex[x][y];
        for(int i = 0; i<x; i++){
            for(int j =0; j<y; j++){
                ICM[i][j] = new Complex(IFA[i][j], 0);
            }
            ICM[i] = CooleyTukey(ICM[i]);
        }
        for(int i = 0; i<y; i++){
            Complex[] T = CooleyTukey(getColumn(ICM, i));
            for(int j = 0; j<x; j++){
                ICM[j][i] = T[j];
            }
        }
//        ImagePlus im = new ImagePlus("Image FFT", getFloatM(ICM));
//        im.show();
        return ICM;
    }
    public static Complex[][] ifft2D(Complex[][] I){
//        float[][] IFA = I.getFloatArray();
        int x = I.length;
        int y = I[0].length;
        Complex[][] ICM = new Complex[x][y];
        for(int i = 0; i<x; i++){
            ICM[i] = ifft(I[i]);
        }
//        System.out.println("Primera dim");
        for(int i = 0; i<y; i++){
            Complex[] T = ifft(getColumn(ICM, i));
            for(int j = 0; j<x; j++){
                ICM[j][i] = T[j];
            }
        }
//        System.out.println("Segunda dim");
//        ImagePlus im = new ImagePlus("Image iFFT", getFloatM(ICM));
//        im.show();
        return ICM;
    }
    ///////////////////////////
    public static Complex[] dft(Complex[] x){
        int n = x.length;
        Complex ZERO = new Complex(0, 0);
        Complex[] y = new Complex[n];
        for (int k = 0; k < n; k++) {
            y[k] = ZERO;
            for (int j = 0; j < n; j++) {
//                int power = (k * j) % n;
                int power = (k * j);
                double kth = -2 * power *  Math.PI / n;
                Complex wkj = new Complex(Math.cos(kth), Math.sin(kth));
                y[k] = y[k].plus(x[j].times(wkj));
//                System.out.println("k : " + k + " n: " + (j) + " x[n]: " + x[j].times(wkj));
//                System.out.println("iter: " + ((k*n) + (j) ) + " x[n]: " + x[j].times(wkj));
            }
        }
        return y;
    }
    public static Complex[] CooleyTukey(Complex[] x){
        x = x.clone();
        int n = x.length;
        int dimX = minPrimeFactor(n);
        int dimY = n/dimX;
        if(dimX == n){
            return dft(x);
        }
        
        Complex[][] matCT = vecToMat(x, dimX, dimY);
        for(int i = 0; i<dimY; i++){
            Complex[] colR = getColumn(matCT, i);
            colR = CooleyTukey(colR);
            for(int j = 0; j<dimX; j++){
                double kth = -2 * (i*j) *  Math.PI / n;
                Complex wkj = new Complex(Math.cos(kth), Math.sin(kth));
                matCT[j][i] = colR[j].times(wkj);
            }
        }
        
        for(int i=0; i<dimX; i++){
            matCT[i] = CooleyTukey(matCT[i]);
        }
        x = matToVec(matCT, dimX, dimY);
        return x;
    }
    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate
        for (int i = 0; i < n; i++) {
//            System.out.println("x:" + x[i]);
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = CooleyTukey(y);

        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by n
        for (int i = 0; i < n; i++) {
            y[i] = y[i].scale(1.0 / n);
        }

        return y;

    }
    ///////////////////////////
    private static Complex[] getColumn(Complex[][] arr, int index){
//        System.out.println("arr lenX: " + arr.length + " arr lenY: " + arr[0].length);
//        System.out.println("index: " + index);
        Complex[] col = new Complex[arr.length];
        for(int i=0; i<arr.length; i++){
           col[i] = arr[i][index];
        }
        return col;
    }
    public static Complex[][] vecToMat(Complex[] x, int dimX, int dimY){
        Complex[][] matR = new Complex[dimX][dimY];
        for(int i=0; i<dimX; i++){
            for(int j=0; j<dimY; j++){
                matR[i][j] = x[(i*dimY) + j];
            }
        }
        return matR;
    }
    public static Complex[] matToVec(Complex[][] matR, int dimX, int dimY){
        Complex[] x = new Complex[dimX*dimY];
        for(int i=0; i<dimX; i++){
            for(int j=0; j<dimY; j++){
                x[i + (j*dimX)] = matR[i][j];
            }
        }
        return x;
    }
    public static int[] GCD(int x, int y){
        int[] gcd = new int[3];
        gcd[0] = 1;
        for(int i = 1; i <= x && i <= y; i++){
            //returns true if both conditions are satisfied   
            if(x%i==0 && y%i==0){
                //storing the variable i in the variable gcd  
                gcd[0] = i;
            }
        }
        return gcd;
    }
    public static ArrayList primeFactors(int n){
        ArrayList<Integer> pF = new ArrayList<Integer>();
        pF.add(1);
        if(n == 1)
            return pF;
        // Print the number of 2s that divide n
        while (n % 2 == 0) {
//            System.out.print(2 + " ");
            n /= 2;
            pF.set(0, pF.get(0)*2);
        }
//        System.out.println("");

        // n must be odd at this point.  So we can
        // skip one element (Note i = i +2)
        for (int i = 3, j = 2; i <= Math.sqrt(n); i += 2, j++) {
            // While i divides n, print i and divide n
            pF.add(1);
            while (n % i == 0) {
//                System.out.print(i + " ");
                n /= i;
                pF.set(i-j, pF.get(i-j)*i);
            }
        }

//        System.out.println("");
        // This condition is to handle the case when
        // n is a prime number greater than 2
        if (n > 2){
//            System.out.print(n);
            pF.add(n);
        }
        int sz = pF.size();
        for(int i=1; i<=sz; i++){
            if(pF.get(sz-i) == 1){
               pF.remove(sz - i);
            }
        }
        return pF;
    }
    public static int minPrimeFactor(int n){
        if(n % 2 == 0) {
            return 2;
        }
        for (int i = 3; i <= Math.sqrt(n); i += 2) {
            if(n % i == 0) {
                return i;
            }
        }
        return n;
    }
    private static ImageProcessor getFloatM_Original(Complex[][] C){
        float[][] F = new float[C.length][C[0].length];
        for(int i=0; i<C.length; i++){
            for(int j=0; j<C[0].length; j++){
                Double t = C[i][j].re();
                F[i][j] = t.floatValue();
            }
        }
        return new FloatProcessor(F);
   }
    private static ImageProcessor getFloatM(Complex[][] C, int amp){
        int xRealD = C.length - (amp/2);
        int yRealD = C[0].length - (amp/2);
        float[][] F = new float[xRealD][yRealD];
        for(int i=0; i<xRealD; i++){
            for(int j=0; j<yRealD; j++){
                Double t = C[i][j].re();
                F[i][j] = t.floatValue();
            }
        }
        return new FloatProcessor(F);
   }
    private static Complex[][] addCross_Original(Complex[][] Img, int amp){
        int Xamp = Img.length;
        int Yamp = Img[0].length;
        int maxX = Xamp * amp;
        int maxY = Yamp * amp;
        Complex[][] fMR = new Complex[maxX][maxY];
        int mdx = (int) Math.ceil((double)Xamp/2);
        int mdy = (int) Math.ceil((double)Yamp/2);
        int lnx = Xamp - mdx;
        int lny = Yamp - mdy;
        double scal = amp*amp;
//        System.out.println("Xamp: " + Xamp + " Yamp: " + Yamp + " amp: " + amp);
//        System.out.println("maxX: " + maxX + " maxY: " + maxY);
//        System.out.println("Xamp-mdx: " + (Xamp-mdx) + " Yamp-mdy: " + (Yamp-mdy));
//        System.out.println("mdx: " + mdx + " mdy: " + mdy);
//        System.out.println("lnx: " + lnx + " lny: " + lny);
//        System.out.println("(maxX-lnx): " + (maxX-lnx) + " (maxY-lny): " + (maxY-lny));
        for(int i=0; i<Xamp*amp; i++){
            for(int j=0; j<Yamp*amp; j++){
//                System.err.println("i: " + i + " j: " + j);
                if(i<mdx && j<mdy){
                    fMR[i][j] = Img[i][j].scale(scal);
                } else if(i<mdx && j>=(maxY-lny)){
                    fMR[i][j] = Img[i][j+Yamp-maxY].scale(scal);
                } else if(i>=(maxX-lnx) && j<mdy){
                    fMR[i][j] = Img[i+Xamp-maxX][j].scale(scal);
                } else if(i>=(maxX-lnx) && j>=(maxY-lny)){
                    fMR[i][j] = Img[i+Xamp-maxX][j+Yamp-maxY].scale(scal);
                } else{
                    fMR[i][j] = new Complex();
                }
            }
        }
        return fMR;
    }
    private static Complex[][] addCross(Complex[][] Img, int amp){
        int Xamp = Img.length;
        int Yamp = Img[0].length;
        int maxX = (Xamp * amp) + (amp/2);
        int maxY = (Yamp * amp) + (amp/2);
        Complex[][] fMR = new Complex[maxX][maxY];
        int mdx = (int) Math.ceil((double)Xamp/2);
        int mdy = (int) Math.ceil((double)Yamp/2);
        int lnx = Xamp - mdx;
        int lny = Yamp - mdy;
        double scal = amp*amp;
//        System.out.println("Xamp: " + Xamp + " Yamp: " + Yamp + " amp: " + amp);
//        System.out.println("maxX: " + maxX + " maxY: " + maxY);
//        System.out.println("Xamp-mdx: " + (Xamp-mdx) + " Yamp-mdy: " + (Yamp-mdy));
//        System.out.println("mdx: " + mdx + " mdy: " + mdy);
//        System.out.println("lnx: " + lnx + " lny: " + lny);
//        System.out.println("(maxX-lnx): " + (maxX-lnx) + " (maxY-lny): " + (maxY-lny));
        for(int i=0; i<maxX; i++){
            for(int j=0; j<maxY; j++){
//                System.err.println("i: " + i + " j: " + j);
                if(i<mdx && j<mdy){
                    fMR[i][j] = Img[i][j].scale(scal);
                } else if(i<mdx && j>=(maxY-lny)){
                    fMR[i][j] = Img[i][j+Yamp-maxY].scale(scal);
                } else if(i>=(maxX-lnx) && j<mdy){
                    fMR[i][j] = Img[i+Xamp-maxX][j].scale(scal);
                } else if(i>=(maxX-lnx) && j>=(maxY-lny)){
                    fMR[i][j] = Img[i+Xamp-maxX][j+Yamp-maxY].scale(scal);
                } else{
                    fMR[i][j] = new Complex();
                }
            }
        }
        return fMR;
    }
    
////////////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args){
        new ImageJ();
//        System.out.println(CLIJ.clinfo());
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/Nanorulers Stack 100 images Datos para Raul.tif");
        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/AZ5A48KB_Atto655-1_x31y30_25x25.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_PSFCHECK_561_DONUTS_33MS_5POWERL.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_DAMIAN_PSFCHECK_561_DONUTS.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/DanielMartinez_BacP24EGFP_150ms_STORM-crop.tif");
        imgTest.show();

        int amp = 20;
        ImageProcessor imgProc = FourierInterp(imgTest.getProcessor(), amp);
        ImagePlus imgFourier = new ImagePlus("Fourier.tif", imgProc);
        ImagePlus imgBicubic = Scaler.resize(new ImagePlus("Bicubic", imgTest.getProcessor()), imgTest.getWidth()*amp, imgTest.getHeight()*amp, 1, "bicubic");
        imgBicubic.setTitle("Bicubic.tif");
        imgFourier.show();
        imgBicubic.show();
    }
    public static void main2(String[] args){
        int n = 20;
        Complex[] c = new Complex[n];
        for(int i=0; i<n; i++){
            c[i] = new Complex(i+1, 0);
        }
        System.out.println("\n------------------------ Valores Originales ------------------------");
        for(int i=0; i<n; i++){
            System.out.println("i: " + i + " val: " + c[i]);
        }

        Complex[] cfft = CooleyTukey(c);
        System.out.println("\n------------------------ Valores FFT ------------------------");
        for(int i=0; i<n; i++){
            System.out.println("i: " + i + " val: " + cfft[i]);
        }

        cfft = ifft(cfft);
        System.out.println("\n------------------------ Valores iFFT ------------------------");
        for(int i=0; i<n; i++){
            System.out.println("i: " + i + " val: " + cfft[i]);
        }

        
        
//        //x and y are the numbers to find the GCF  
//        int n = 55;
//        int minPF = minPrimeFactor(n);
//        System.out.println(minPF);
//        
//        ArrayList<Integer> pF = primeFactors(n);
//        System.out.println(pF);
//        
//        System.out.println("\n");
//        
//        int x = 12, y = 8;
//        int[] gcd;
//        //running loop form 1 to the smallest of both numbers  
//        gcd = GCD(x, y);
//        //prints the gcd
//        System.out.printf("GCD of %d and %d is: %d\n", x, y, gcd[0]);  
    }
}

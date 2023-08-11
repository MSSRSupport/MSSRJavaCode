/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mssr2.process;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Undo;
import static ij.measure.Measurements.MEAN;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;


public class FFTJ{
    // calculates the Fourier Interpolation from ImageProcessor
    public static ImageProcessor FourierInterp(ImageProcessor Im, int amp){
        ImagePlus tt = new ImagePlus("Image Pad",pad(Im));
        tt.show();
        Complex[][] Ifft = fft2D(pad(Im));
        ImagePlus iFFTPad = new ImagePlus("Image FFTPad", getFloatM(Ifft));
        iFFTPad.show();
        System.out.println("x: " + Ifft.length + " y: " + Ifft[0].length + "\n");
        Complex[][] I = addCross(Ifft, Im.getWidth()*amp, Im.getHeight()*amp, amp);
        ImagePlus iCross = new ImagePlus("Image Cross", getFloatM(I));
        iCross.show();
        Complex[][] ICM = ifft2D(pad(I));
        System.err.println("ifft");
        ImagePlus ifftImg = new ImagePlus("Image ifft", getFloatM(ICM));
        ifftImg.show();
//        return getFloatM(ICM);
        return getFloatM(ICM, Im.getWidth()*amp, Im.getHeight()*amp);
    }
    // calculates the fft of 2s array from ImageProcessor
    public static Complex[][] fft2D(ImageProcessor I){
        float[][] IFA = I.getFloatArray();
        int x = I.getWidth();
        int y = I.getHeight();
        Complex[][] ICM = new Complex[x][y];
        for(int i = 0; i<x; i++){
            for(int j =0; j<y; j++){
                ICM[i][j] = new Complex(IFA[i][j], 0);
            }
            ICM[i] = fft(ICM[i]);
        }
        for(int i = 0; i<y; i++){
            Complex[] T = fft(getColumn(ICM, i));
            for(int j = 0; j<x; j++){
                ICM[j][i] = T[j];
            }
        }
//        ImagePlus im = new ImagePlus("Image FFT", getFloatM(ICM));
//        im.show();
        return ICM;
    }
    // calculates the ifft of 2s array from Complex array
    public static Complex[][] ifft2D(Complex[][] I){
//        float[][] IFA = I.getFloatArray();
        int x = I.length;
        int y = I[0].length;
        Complex[][] ICM = new Complex[x][y];
        for(int i = 0; i<x; i++){
            ICM[i] = ifft(I[i]);
        }
        System.out.println("Primera dim");
        for(int i = 0; i<y; i++){
            Complex[] T = ifft(getColumn(ICM, i));
            for(int j = 0; j<x; j++){
                ICM[j][i] = T[j];
            }
        }
        System.out.println("Segunda dim");
//        ImagePlus im = new ImagePlus("Image iFFT", getFloatM(ICM));
//        im.show();
        return ICM;
    }
    // add cross inside cuadrants
    private static Complex[][] addCross(Complex[][] IMC, int Xamp, int Yamp, int amp){
        Xamp = IMC.length;
        Yamp = IMC[0].length;
        int maxX = (Xamp * amp) + (amp/2);
        int maxY = (Yamp * amp) + (amp/2);
        Complex[][] fMR = new Complex[maxX][maxY];
        int mdx = (int) Math.ceil((double)Xamp/2);
        int mdy = (int) Math.ceil((double)Yamp/2);
        int lnx = Xamp - mdx;
        int lny = Yamp - mdy;
        double scal = amp*amp;
        for(int i=0; i<maxX; i++){
            for(int j=0; j<maxY; j++){
                if(i<mdx && j<mdy){
                    fMR[i][j] = IMC[i][j].scale(scal);
                } else if(i<mdx && j>=(maxY-lny)){
                    fMR[i][j] = IMC[i][j+Yamp-maxY].scale(scal);
                } else if(i>=(maxX-lnx) && j<mdy){
                    fMR[i][j] = IMC[i+Xamp-maxX][j].scale(scal);
                } else if(i>=(maxX-lnx) && j>=(maxY-lny)){
                    fMR[i][j] = IMC[i+Xamp-maxX][j+Yamp-maxY].scale(scal);
                } else{
                    fMR[i][j] = new Complex();
                }
            }
        }
        return fMR;
    }
    private static Complex[][] addCross_Original(Complex[][] IMC, int Xamp, int Yamp, int amp){
        int maxN = Math.max(Xamp, Yamp);
        int k = 2;
        while(k<maxN) k *= 2;
        maxN = k;
        System.out.println("Xamp: " + Xamp + " Yamp: " + Yamp + " maxN: " + maxN);
//        Complex[][] finalMR = CreateComplexMatrix(maxN, maxN);
        Complex[][] finalM = CreateComplexMatrix(maxN, maxN);

        int maxX = IMC.length;
        int maxY = IMC[0].length;
        int mdx = (int) Math.ceil((double)maxX/2);
        int mdy = (int) Math.ceil((double)maxY/2);
        int lnx = maxX - mdx;
        int lny = maxY - mdy;
        double scal = amp*amp;
        for(int i=0; i<mdx; i++){
            for(int j=0; j<mdy; j++){
                System.out.println("(" + i + ", " + j + ") " + IMC[i][j]);
                finalM[i][j] = IMC[i][j].scale(scal);
            }
        }
        for(int i=0; i<mdx; i++){
            for(int j=0; j<(maxY-mdy); j++){
                finalM[i][maxY-lny+j] = IMC[i][mdy+j].scale(scal);
            }
        }
        for(int i=0; i<(maxX-mdx); i++){
            for(int j=0; j<mdy; j++){
                finalM[maxX-lnx+i][j] = IMC[mdx+i][j].scale(scal);
            }
        }
        for(int i=0; i<(maxX-mdx); i++){
            for(int j=0; j<(maxY-mdy); j++){
                finalM[maxX-lnx+i][maxY-lny+j] = IMC[mdx+i][mdy+j].scale(scal);
            }
        }
        return finalM;
    }
    // compute the FFT of x[], assuming its length n is a power of 2
    public static Complex[] fft(Complex[] x) {
        int n = x.length;
//        System.out.println("n: " + n);

        // base case
        if (n == 1) return new Complex[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) {
            throw new IllegalArgumentException("n is not a power of 2");
        }

        // compute FFT of even terms
        Complex[] even = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] evenFFT = fft(even);

        // compute FFT of odd terms
        Complex[] odd  = even;  // reuse the array (to avoid n log n space)
        for (int k = 0; k < n/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] oddFFT = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = evenFFT[k].plus (wk.times(oddFFT[k]));
            y[k + n/2] = evenFFT[k].minus(wk.times(oddFFT[k]));
        }
        return y;
    }
    // compute the inverse FFT of x[], assuming its length n is a power of 2
    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

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
    // get a column of 2d array
    public static Complex[] getColumn(Complex[][] array, int index){
        Complex[] column = new Complex[array.length]; // Here I assume a rectangular 2D array! 
        for(int i=0; i<column.length; i++){
           column[i] = array[i][index];
        }
        return column;
    }
    // display an array of Complex numbers to standard output
    private static ImageProcessor getFloatM(Complex[][] C){
        float[][] F = new float[C.length][C[0].length];
        for(int i=0; i<C.length; i++){
            for(int j=0; j<C[0].length; j++){
                Double t = C[i][j].re();
                F[i][j] = t.floatValue();
            }
        }
        return new FloatProcessor(F);
   }
    private static ImageProcessor getFloatM(Complex[][] C, int limX, int limY){
        float[][] F = new float[limX][limY];
        for(int i=0; i<limX; i++){
            for(int j=0; j<limY; j++){
                Double t = C[i][j].re();
                F[i][j] = t.floatValue();
            }
        }
        return new FloatProcessor(F);
    }
    public static Complex[][] pad(Complex[][] imgO){
        int originalWidth = imgO.length;
        int originalHeight = imgO[0].length;
        int maxN = Math.max(originalWidth, originalHeight);
        int newN = 2;
        while(newN<maxN) newN *= 2;
        if (newN==maxN && originalWidth==originalHeight) {
            return imgO;
        }
        Complex[][] imgF = new Complex[newN][newN];
        for(int x=0; x<newN; x++){
            for(int y=0; y<newN; y++){
                if(x<originalWidth && y<originalHeight){
                    imgF[x][y] = imgO[x][y];
                } else{
                    imgF[x][y] = new Complex();
                }
            }
        }
        return imgF;
    }
    public static ImageProcessor pad(ImageProcessor ip) {
        int originalWidth = ip.getWidth();
        int originalHeight = ip.getHeight();
        int maxN = Math.max(originalWidth, originalHeight);
        int i = 2;
        while(i<maxN) i *= 2;
        if (i==maxN && originalWidth==originalHeight) {
            return ip;
        }
        maxN = i;
    //        showStatus("Padding to "+ maxN + "x" + maxN);
        if (maxN>=65536) {
            IJ.error("FFT", "Padded image is too large ("+maxN+"x"+maxN+")");
            return null;
        }
        ImageStatistics stats = ImageStatistics.getStatistics(ip, MEAN, null);
        ImageProcessor ip2 = ip.createProcessor(maxN, maxN);
//        ip2.setValue(stats.mean);
        ip2.setValue(0);
        ip2.fill();
        ip2.insert(ip, 0, 0);
    //        padded = true;
        Undo.reset();
        return ip2;
    }
    private static Complex[][] CreateComplexMatrix(int x, int y){
        Complex[][] resul = new Complex[x][y];
        for(int i = 0; i<x; i++){
            for(int j = 0; j<y; j++){
                resul[i][j] = new Complex();
            }
        }
        return resul;
    }
// compute the DFT of x[] via brute force (n^2 time)
    public static Complex[] dftCorrecta(Complex[] x) {
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
    public static void main2(String[] args) {
        // TODO code application logic here
        
        int n = 5;
        int n2 = 8;
        Complex[] c = new Complex[n2];
        for(int i=0; i<n; i++){
            c[i] = new Complex(i+1, 0);
        }
        c[5] = new Complex();
        c[6] = new Complex();
        c[7] = new Complex();
        
        System.out.println("\n------------------------ Valores Originales ------------------------");
        for(int i=0; i<n2; i++){
            System.out.println("i: " + i + " val: " + c[i]);
//            System.out.println("val: " + real[i] + " + " + imag[i] + "i");
        }

        Complex[] cfft = fft(c);
        System.out.println("\n------------------------ Valores FFT ------------------------");
        for(int i=0; i<n2; i++){
            System.out.println("i: " + i + " val: " + cfft[i]);
//            System.out.println("val: " + real[i] + " + " + imag[i] + "i");
        }
    }
    public static void main(String[] args) {
        // TODO code application logic here
        
        new ImageJ();
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/Nanorulers Stack 100 images Datos para Raul.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_PSFCHECK_561_DONUTS_33MS_5POWERL.tif");
        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_DAMIAN_PSFCHECK_561_DONUTS.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/DanielMartinez_BacP24EGFP_150ms_STORM-crop.tif");
        imgTest.show();


        if (imgTest.isHyperStack()||imgTest.isComposite()||imgTest.getStackSize()>1) {
            imgTest.setDimensions(1,imgTest.getStackSize(),1);
        }
        if(imgTest.getBitDepth() != 32){
            ImageConverter iC = new ImageConverter(imgTest);
            iC.convertToGray32();
        }
        int amp = 5;
        ImageStack imgStck = new ImageStack(imgTest.getWidth()*amp, imgTest.getHeight()*amp);
//        System.out.println(imgTest.getNSlices());
        for( int i = 0; i<1; i++){
            imgTest.setSlice(i+1);
//            ImageProcessor imgProc = GeneralFFT.FourierInterpC(imgTest.getProcessor(), amp);
            ImageProcessor imgProc = FourierInterp(imgTest.getProcessor(), amp);
            imgStck.addSlice("", imgProc, i);
        }
        ImagePlus at = new ImagePlus("FInal", imgStck);
        at.show();
    }
}
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
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
//import mssr.process.Complex;
//import static mssr.process.FFTJ.fft;
//import static mssr.process.FFTJ.getColumn;

/**
 *
 * @author raul_
 */
public class GeneralFFT {
  // below FFT code very slightly modified from
  // http://nayuki.eigenstate.org/res/free-small-fft-in-multiple-languages/Fft.java
  /*
   * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result
   * back into the vector. The vector can have any length. This is a wrapper function.
   */
    public static void transform(double[] real, double[] imag) {
        if (real.length != imag.length)
            throw new IllegalArgumentException("Mismatched lengths");

        int n = real.length;
        if (n == 0){
            return;
        } else if ((n & (n - 1)) == 0){ // Is power of 2
//            Complex[] values = new Complex[n];
//            for(int i=0; i<n; i++){
//                values[i] = new Complex(real[i], imag[i]);
//            }
//
//            values = fftRdx2(values);
//            for(int i=0; i<n; i++){
//                real[i] = values[i].re();
//                imag[i] = values[i].im();
//            }
            transformRadix2(real, imag);
        } else{ // More complicated algorithm for aribtrary sizes
            transformBluestein(real, imag);
        }
//        for(int i=0; i<n; i++){
//            real[i] = Math.floor(real[i]*10000)/10000;
//            imag[i] = Math.floor(imag[i]*100000)/100000;
//        }
     }


  /*
   * Computes the inverse discrete Fourier transform (IDFT) of the given complex vector, storing the
   * result back into the vector. The vector can have any length. This is a wrapper function. This
   * transform does not perform scaling, so the inverse is not a true inverse.
   */
    public static void ifft(double[] real, double[] imag) {
        transform(imag, real);
        int n = real.length;
        for (int i = 0; i < n; i++) { // Scaling (because this FFT implementation omits it)
            real[i] = (real[i] / n);
            imag[i] = (imag[i] / n);
        }
    }
    private static void inverseTransform(double[] real, double[] imag) {
        transform(imag, real);
        int n = real.length;
    }

  /*
   * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result
   * back into the vector. The vector's length must be a power of 2. Uses the Cooley-Tukey
   * decimation-in-time radix-2 algorithm.
   */
  private static void transformRadix2(double[] real, double[] imag) {
//    System.out.println("2^k en double");
    // Initialization
    if (real.length != imag.length)
      throw new IllegalArgumentException("Mismatched lengths");
    int n = real.length;
    if (n <= 1)
      return;
    int levels = -1;
    for (int i = 0; i < 32; i++) {
      if (1 << i == n)
        levels = i; // Equal to log2(n)
    }
    if (levels == -1)
      throw new IllegalArgumentException("Length is not a power of 2");
    double[] cosTable = new double[n / 2];
    double[] sinTable = new double[n / 2];
    cosTable[0] = 1;
    sinTable[0] = 0;
    double qc = Math.cos(2 * Math.PI / n), qs = Math.sin(2 * Math.PI / n);
    for (int i = 1; i < n / 2; i++) {
      cosTable[i] = cosTable[i - 1] * qc - sinTable[i - 1] * qs;
      sinTable[i] = sinTable[i - 1] * qc + cosTable[i - 1] * qs;
    }

    // Bit-reversed addressing permutation
    for (int i = 0; i < n; i++) {
      int j = Integer.reverse(i) >>> (32 - levels);
      if (j > i) {
        double temp = real[i];
        real[i] = real[j];
        real[j] = temp;
        temp = imag[i];
        imag[i] = imag[j];
        imag[j] = temp;
      }
    }

    // Cooley-Tukey decimation-in-time radix-2 FFT
    for (int size = 2; size <= n; size *= 2) {
      int halfsize = size / 2;
      int tablestep = n / size;
      for (int i = 0; i < n; i += size) {
        for (int j = i, k = 0; j < i + halfsize; j++, k += tablestep) {
          double tpre = real[j + halfsize] * cosTable[k] + imag[j + halfsize] * sinTable[k];
          double tpim = -real[j + halfsize] * sinTable[k] + imag[j + halfsize] * cosTable[k];
          real[j + halfsize] = real[j] - tpre;
          imag[j + halfsize] = imag[j] - tpim;
          real[j] += tpre;
          imag[j] += tpim;
        }
      }
    }
  }
    public static Complex[] fftRdx2(Complex[] x) {
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
        Complex[] evenFFT = fftRdx2(even);

        // compute FFT of odd terms
        Complex[] odd  = even;  // reuse the array (to avoid n log n space)
        for (int k = 0; k < n/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] oddFFT = fftRdx2(odd);

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


  /*
   * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result
   * back into the vector. The vector can have any length. This requires the convolution function,
   * which in turn requires the radix-2 FFT function. Uses Bluestein's chirp z-transform algorithm.
   */
  private static void transformBluestein(double[] real, double[] imag) {
    // Find a power-of-2 convolution length m such that m >= n * 2 - 1
    int n = real.length;
    int m = 1;
    while (m < n * 2 - 1)
      m *= 2;

    // Trignometric tables
    double[] tc = new double[2 * n];
    double[] ts = new double[2 * n];
    tc[0] = 1;
    ts[0] = 0;
    double qc = Math.cos(Math.PI / n), qs = Math.sin(Math.PI / n);
    for (int i = 1; i < 2 * n; i++) {
      tc[i] = tc[i - 1] * qc - ts[i - 1] * qs;
      ts[i] = ts[i - 1] * qc + tc[i - 1] * qs;
    }
    double[] cosTable = new double[n];
    double[] sinTable = new double[n];
    for (int i = 0; i < n; i++) {
      int j = (int) ((long) i * i % (n * 2)); // This is more accurate than j = i * i
      cosTable[i] = tc[j];
      sinTable[i] = ts[j];
    }

    // Temporary vectors and preprocessing
    double[] areal = new double[m];
    double[] aimag = new double[m];
    for (int i = 0; i < n; i++) {
      areal[i] = real[i] * cosTable[i] + imag[i] * sinTable[i];
      aimag[i] = -real[i] * sinTable[i] + imag[i] * cosTable[i];
    }
    double[] breal = new double[m];
    double[] bimag = new double[m];
    breal[0] = cosTable[0];
    bimag[0] = sinTable[0];
    for (int i = 1; i < n; i++) {
      breal[i] = breal[m - i] = cosTable[i];
      bimag[i] = bimag[m - i] = sinTable[i];
    }

    // Convolution
    double[] creal = new double[m];
    double[] cimag = new double[m];
    convolve(areal, aimag, breal, bimag, creal, cimag);

    // Postprocessing
    for (int i = 0; i < n; i++) {
      real[i] = creal[i] * cosTable[i] + cimag[i] * sinTable[i];
      imag[i] = -creal[i] * sinTable[i] + cimag[i] * cosTable[i];
    }
  }

  /*
   * Computes the circular convolution of the given complex vectors. Each vector's length must be
   * the same.
   */
  public static void convolve(double[] xreal, double[] ximag, double[] yreal, double[] yimag,
      double[] outreal, double[] outimag) {
    if (xreal.length != ximag.length || xreal.length != yreal.length
        || yreal.length != yimag.length || xreal.length != outreal.length
        || outreal.length != outimag.length)
      throw new IllegalArgumentException("Mismatched lengths");

    int n = xreal.length;
    xreal = xreal.clone();
    ximag = ximag.clone();
    yreal = yreal.clone();
    yimag = yimag.clone();

    transform(xreal, ximag);
    transform(yreal, yimag);
    for (int i = 0; i < n; i++) {
      double temp = xreal[i] * yreal[i] - ximag[i] * yimag[i];
      ximag[i] = ximag[i] * yreal[i] + xreal[i] * yimag[i];
      xreal[i] = temp;
    }
    inverseTransform(xreal, ximag);
    for (int i = 0; i < n; i++) { // Scaling (because this FFT implementation omits it)
      outreal[i] = xreal[i] / n;
      outimag[i] = ximag[i] / n;
    }
  }
  
  /*
   * Computes the FFT of an Image Processor
   */
    public static ImageProcessor FourierInterpD(ImageProcessor I, int amp){
        float[][] IFA = I.getFloatArray().clone();
        int x = I.getWidth();
        int y = I.getHeight();
        double[][] real = new double[x][y];
        double[][] imag = real.clone();
        for(int i = 0; i<x; i++){
            for(int j =0; j<y; j++){
                imag[i][j] = 0;
            }
        }
        
        ///////////////////////////////////// FFT
        for(int i = 0; i<x; i++){
            for(int j =0; j<y; j++){
                real[i][j] = IFA[i][j];
            }
            GeneralFFT.transform(real[i], imag[i]);
        }
        ImagePlus imFFT = new ImagePlus("Image FFT 1ra Dir", getFloatM(real));
        imFFT.show();
        for(int i = 0; i<y; i++){
            double[] colR = getColumn(real, i);
            double[] colI = getColumn(imag, i);
            GeneralFFT.transform(colR, colI);
            for(int j = 0; j<x; j++){
                real[j][i] = colR[j];
                imag[j][i] = colI[j];
            }
        }
        imFFT = new ImagePlus("Image FFT 2da Dir", getFloatM(real));
        imFFT.show();
        
        ///////////////////////////////////// Padding
        double[][] fMR = new double[x*amp][y*amp];
        double[][] fMI = new double[x*amp][y*amp];
        addCross(real, imag, fMR, fMI, amp);
//        real = imag = null;
        ImagePlus imP = new ImagePlus("Image FFTPad", getFloatM(fMR));
        imP.show();
        
        ///////////////////////////////////// iFFT
        x = x*amp;
        y = y*amp;
        for(int i = 0; i<x; i++){
            GeneralFFT.ifft(fMR[i], fMI[i]);
        }
        for(int i = 0; i<y; i++){
            double[] colR = getColumn(fMR, i);
            double[] colI = getColumn(fMI, i);
            GeneralFFT.ifft(colR, colI);
            for(int j = 0; j<x; j++){
                fMR[j][i] = colR[j];
            }
        }
        ImagePlus im = new ImagePlus("Image iFFT", getFloatM(fMR));
        im.show();
        return getFloatM(fMR);
    }
    // get a column of 2d array
    private static double[] getColumn(double[][] arr, int index){
//        System.out.println("arr lenX: " + arr.length + " arr lenY: " + arr[0].length);
//        System.out.println("index: " + index);
        double[] col = new double[arr.length];
        for(int i=0; i<arr.length; i++){
           col[i] = arr[i][index];
        }
        return col;
    }
    private static void addCross(double[][] real, double[][] imag, double[][] fMR, double[][] fMI, int amp){
        int Xamp = real.length;
        int Yamp = real[0].length;
        int maxX = Xamp * amp;
        int maxY = Yamp * amp;
        int mdx = (int) Math.ceil((double)Xamp/2);
        int mdy = (int) Math.ceil((double)Yamp/2);
        int lnx = Xamp - mdx;
        int lny = Yamp - mdy;
        double scal = amp*amp;
        System.out.println("Xamp: " + Xamp + " Yamp: " + Yamp + " amp: " + amp);
        System.out.println("maxX: " + maxX + " maxY: " + maxY);
        System.out.println("Xamp-mdx: " + (Xamp-mdx) + " Yamp-mdy: " + (Yamp-mdy));
        System.out.println("mdx: " + mdx + " mdy: " + mdy);
        System.out.println("lnx: " + lnx + " lny: " + lny);
        System.out.println("(maxX-lnx): " + (maxX-lnx) + " (maxY-lny): " + (maxY-lny));
        for(int i=0; i<Xamp*amp; i++){
            for(int j=0; j<Yamp*amp; j++){
//                System.err.println("i: " + i + " j: " + j);
                if(i<mdx && j<mdy){
//                    System.err.println("Cuadrante 1");
                    fMR[i][j] = real[i][j] * scal;
                    fMI[i][j] = imag[i][j] * scal;
                } else if(i<mdx && j>=(maxY-lny)){
                    fMR[i][j] = real[i][j+Yamp-maxY] * scal;
                    fMI[i][j] = imag[i][j+Yamp-maxY] * scal;
                } else if(i>=(maxX-lnx) && j<mdy){
                    fMR[i][j] = real[i+Xamp-maxX][j] * scal;
                    fMI[i][j] = imag[i+Xamp-maxX][j] * scal;
                } else if(i>=(maxX-lnx) && j>=(maxY-lny)){
                    fMR[i][j] = real[i+Xamp-maxX][j+Yamp-maxY] * scal;
                    fMI[i][j] = imag[i+Xamp-maxX][j+Yamp-maxY] * scal;
                } else{
                    fMR[i][j] = 0;
                    fMI[i][j] = 0;
                }
            }
        }
    }
    private static ImageProcessor getFloatM(double[][] fMR){
        float[][] F = new float[fMR.length][fMR[0].length];
        for(int i=0; i<fMR.length; i++){
            for(int j=0; j<fMR[0].length; j++){
                F[i][j] = new Double(fMR[i][j]).floatValue();
            }
        }
        return new FloatProcessor(F);
   }
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
    public static void main(String[] args) {
        // TODO code application logic here
        
//        int n = 5;
//        Complex[] c = new Complex[n];
//        for(int i=0; i<n; i++){
//            c[i] = new Complex(i+1, 0);
//        }
//        
//        double[] real = new double[n];
//        double[] imag = new double[n];
//        for(int i=0; i<n; i++){
//            real[i] = i+1;
//            imag[i] = 0;
//        }
//        System.out.println("\n------------------------ Valores Originales ------------------------");
//        for(int i=0; i<n; i++){
////            System.out.println("i: " + i + " val: " + c[i]);
//            System.out.println("val: " + real[i] + " + " + imag[i] + "i");
//        }
//
////        Complex[] cfft = transform(c);
//        transform(real, imag);
//        System.out.println("\n------------------------ Valores FFT ------------------------");
//        for(int i=0; i<n; i++){
////            System.out.println("i: " + i + " val: " + cfft[i]);
//            System.out.println("val: " + real[i] + " + " + imag[i] + "i");
//        }
//
//        ifft(real, imag);
//        System.out.println("\n------------------------ Valores FFT ------------------------");
//        for(int i=0; i<n; i++){
////            System.out.println("i: " + i + " val: " + cfft[i]);
//            System.out.println("val: " + real[i] + " + " + imag[i] + "i");
//        }

        
        
        new ImageJ();
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/Nanorulers Stack 100 images Datos para Raul.tif");
        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_PSFCHECK_561_DONUTS_33MS_5POWERL.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_DAMIAN_PSFCHECK_561_DONUTS.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/DanielMartinez_BacP24EGFP_150ms_STORM-crop.tif");
        imgTest.show();


        if (imgTest.isHyperStack()||imgTest.isComposite()||imgTest.getStackSize()>1) {
            imgTest.setDimensions(1,imgTest.getStackSize(),1);
        }
        if(imgTest.getBitDepth() != 32){
            ImageConverter iC = new ImageConverter(imgTest);
            iC.convertToGray32();
        }
        int amp = 3;
        ImageStack imgStck = new ImageStack(imgTest.getWidth()*amp, imgTest.getHeight()*amp);
//        System.out.println(imgTest.getNSlices());
        for( int i = 0; i<1; i++){
            imgTest.setSlice(i+1);
//            ImageProcessor imgProc = GeneralFFT.FourierInterpC(imgTest.getProcessor(), amp);
            ImageProcessor imgProc = FourierInterpD(imgTest.getProcessor(), amp);
            imgStck.addSlice("", imgProc, i);
        }
        ImagePlus at = new ImagePlus("", imgStck);
        at.show();
    }
}
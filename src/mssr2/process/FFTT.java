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

/**
 *
 * @author raul_
 */
public class FFTT {
    private static Complex[][] wktW;
    public static Complex[][] fft2d(Complex[][] imgO){
        Complex[][] imgF = new Complex[0][0];
        return imgF;
    }
    public static Complex[][] getComplexMatrix(ImageProcessor ip){
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[][] ipFA = ip.getFloatArray();
        Complex[][] ipCA = new Complex[width][height];
        wktW = new Complex[width][height];
        for(int x=0; x<width; x++){
            for(int y=0; y<height; y++){
                ipCA[x][y] = new Complex();
                int power = (x * y);
                double kth = -2 * power *  Math.PI / width;
                wktW[x][y] = new Complex(Math.cos(kth), Math.sin(kth));
            }
        }
        System.out.println("width: " + width);
        return ipCA;
    }
    public static void matrxMult(Complex[][] imgA){
        int width = imgA.length;
        int height = imgA[0].length;
        Complex[][] mT = new Complex[height][height];
        Complex[][] mF = new Complex[width][height];
        System.out.println(width);
        System.out.println(height);
        for(int x=0; x<height; x++){
            for(int y=0; y<height; y++){
                mT[x][y] = new Complex();
                for(int z=0; z<width; z++){
                    Complex that = imgA[z][y].times(wktW[x][z]);
                    mT[x][y] = mT[x][y].plus(that);
                }
            }
        }
    }
    public static void main(String[] args) {
        new ImageJ();
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/Nanorulers Stack 100 images Datos para Raul.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_PSFCHECK_561_DONUTS_33MS_5POWERL.tif");
        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_DAMIAN_PSFCHECK_561_DONUTS.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/DanielMartinez_BacP24EGFP_150ms_STORM-crop.tif");
        imgTest.show();


        for( int i = 0; i<1; i++){
            imgTest.setSlice(i+1);
//            ImageProcessor imgProc = GeneralFFT.FourierInterpC(imgTest.getProcessor(), amp);
            float[][] A = imgTest.getProcessor().getFloatArray();
            System.out.println(A.length);
            System.out.println(A[0].length);
            Complex[][] ipCA = getComplexMatrix(imgTest.getProcessor());
            matrxMult(ipCA);
//            matrxMult();
        }


//        if (imgTest.isHyperStack()||imgTest.isComposite()||imgTest.getStackSize()>1) {
//            imgTest.setDimensions(1,imgTest.getStackSize(),1);
//        }
//        if(imgTest.getBitDepth() != 32){
//            ImageConverter iC = new ImageConverter(imgTest);
//            iC.convertToGray32();
//        }
//        int amp = 5;
//        ImageStack imgStck = new ImageStack(imgTest.getWidth()*amp, imgTest.getHeight()*amp);
////        System.out.println(imgTest.getNSlices());
//        for( int i = 0; i<1; i++){
//            imgTest.setSlice(i+1);
////            ImageProcessor imgProc = GeneralFFT.FourierInterpC(imgTest.getProcessor(), amp);
//            ImageProcessor imgProc = FourierInterp(imgTest.getProcessor(), amp);
//            imgStck.addSlice("", imgProc, i);
//        }
//        ImagePlus at = new ImagePlus("", imgStck);
//        at.show();
    }
}

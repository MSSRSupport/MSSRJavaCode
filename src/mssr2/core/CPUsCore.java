/*
 * Copyright (C) 2021 raul_
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mssr2.core;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.Scaler;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Component;
import mssr2.process.CoreProcess;

/**
 *
 * @author raul_
 */
public class CPUsCore extends Thread{
    private ImagePlus imageBase;
    private ImagePlus ampImage;
    private ImagePlus xPad;
    private ImagePlus M;
    private ImagePlus weightAccum;
    private ImagePlus yAccum;
    public ImagePlus mssrdImageStack;
    private final int hs;
    private final int amp;
    private final int order;
    private final boolean meshing;
    private final int desf;
    private final ImageCalculator Call;
    public long timeElapsed;
    private final Component cSuper;

    public CPUsCore(Component C, ImagePlus imgBase, int amp, float psf, int order, boolean meshing){
        this.cSuper = C;
        //Definicion de parametros basicos
        this.imageBase = imgBase;
//        this.hs = (int) Math.round(0.4247*psf*amp);
        this.hs = (int) Math.round(0.5*psf*amp);
        this.amp = amp;
        this.order = order;
        this.meshing = meshing;
//        System.out.print("Meshing ");
//        System.out.println(this.prop);
        this.desf = (int) Math.ceil((float)this.amp/2);
        this.Call = new ImageCalculator();
    }
    public CPUsCore(Component C, ImageProcessor imgBase, int amp, float psf, int order, boolean meshing){
        this.cSuper = C;
        //Definicion de parametros basicos
        this.imageBase = new ImagePlus("", imgBase);
        this.hs = (int) Math.round(0.5*psf*amp);
        this.amp = amp;
        this.order = order;
        this.meshing = meshing;
//        System.out.print("Meshing ");
//        System.out.println(this.prop);
        this.desf = (int) Math.ceil((float)this.amp/2);
        this.Call = new ImageCalculator();
    }

    @Override
    public void run(){
        this.runAmp();
        System.out.println("\tAmplification end");
        this.imageBase = null;

        this.mOfMax();
        System.out.println("\tMatrix of max end");

        this.accums();
        System.out.println("\tAccums end");
        this.M = null;
        this.xPad = null;

        this.orden();
        System.out.println("\tOrder end");
        this.weightAccum = null;
        this.yAccum = null;
        if(MSSR.excludeOutL){
            this.excludeOutliers();
            System.out.println("\tExclude Outliers end");
        }
        if(MSSR.intensity_normalization){
            this.mssrdImageStack = Call.run("Multiply stack create 32-bit", this.ampImage, this.mssrdImageStack).duplicate();
        }
        this.ampImage = null;
        this.mssrdImageStack.setSlice(1);
    }
    private void runAmp(){
        //Conversion a 32bits
        if(this.imageBase.getBitDepth() != 32){
            ImageConverter iC = new ImageConverter(this.imageBase);
            iC.convertToGray32();
        }
        if(this.amp>1){
            if(MSSR.interpType == 1){// Bicubic
                this.ampImage = Scaler.resize(this.imageBase, this.imageBase.getWidth()*amp, this.imageBase.getHeight()*amp, 1, "bicubic");
            } else {// Fourier
                this.ampImage = CoreProcess.StackInterpFourier(this.imageBase, this.amp);
            }
            if(this.meshing){
                this.ampImage = CoreProcess.compArtMatrix(this.ampImage, this.desf, 1);
            }
        } else{
            this.ampImage = this.imageBase.duplicate();
        }
        this.xPad = CoreProcess.newStckImg(CoreProcess.padimgrrayMatrix(CoreProcess.getImgArray(this.ampImage), this.hs));
    }
    private void mOfMax(){
        int nSlices = this.ampImage.getNSlices();
        int width = this.ampImage.getWidth();
        int height = this.ampImage.getHeight();
        this.M = CoreProcess.newStckImg(new float[nSlices][width][height]);
        for(int i = -this.hs, porcentaje = 1; i<(this.hs+1);i++, porcentaje++){
            for(int j = -this.hs; j<(this.hs+1); j++){
                if((i!=0 || j!=0) && (Math.sqrt(Math.pow(i, 2) + Math.pow(j, 2)) <= this.hs)){
                    xPad.setRoi(hs + i, hs + j, width, height);
                    ImagePlus xThis = xPad.crop("stack");
                    ImagePlus maxT = Call.run("Diff stack create 32-bit", this.ampImage, xThis);
                    xThis = null;
                    this.M = Call.run("Max stack create 32-bit", this.M, maxT);
                }
            }
            if((int)(porcentaje*((float)100/(hs+hs+1))) % 10 == 0){
                System.out.println("Matrix of max " + (int)(porcentaje*((float)100/(hs+hs+1))) + "%");
            }
        }
//        System.out.println("M " + Arrays.toString(this.M.getPixel(80, 70)));
    }
    private void accums(){
        int nSlices = this.ampImage.getNSlices();
        int width = this.ampImage.getWidth();
        int height = this.ampImage.getHeight();
        this.weightAccum = CoreProcess.newStckImg(new float[nSlices][width][height]);
        this.yAccum = CoreProcess.newStckImg(new float[nSlices][width][height]);
        for(int i = -this.hs, porcentaje = 1; i<(this.hs+1);i++, porcentaje++){
            for(int j = -this.hs; j<(this.hs+1); j++){
                if((i!=0 || j!=0) && (Math.sqrt(Math.pow(i, 2) + Math.pow(j, 2)) <= this.hs)){
                    double spatialKernel = Math.exp(-(i*i + j*j) / (hs*hs));
                    xPad.setRoi(hs + i, hs + j, width, height);
                    ImagePlus xThis = xPad.crop("stack");
                    ImagePlus xDiffSq0 = Call.run("Divide stack create 32-bit", Call.run("Subtract stack create 32-bit", this.ampImage, xThis), this.M);
                    xDiffSq0 = Call.run("Multiply stack create 32-bit", xDiffSq0, xDiffSq0);
                    ImagePlus weightThis = xDiffSq0.duplicate();
                    xDiffSq0 = null;
                    IJ.run(weightThis, "Multiply...", "value="+String.valueOf(-1)+" stack");//intensityKernel
                    IJ.run(weightThis, "Exp", "stack");//intensityKernel
                    IJ.run(weightThis, "Multiply...", "value="+String.valueOf(spatialKernel)+" stack");//weightThis
                    this.weightAccum = Call.run("Add stack create 32-bit", this.weightAccum, weightThis);
                    this.yAccum = Call.run("Add stack create 32-bit", this.yAccum, Call.run("Multiply stack create 32-bit", xThis, weightThis));
                }
            }
            if((int)(porcentaje*((float)100/(hs+hs+1))) % 10 == 0){
                System.out.println("Accums " + (int)(porcentaje*((float)100/(hs+hs+1))) + "%");
            }
        }
    }
    private void orden(){
//        MS <- AMP - (yAccum / weightAccum)
        ImagePlus MS = Call.run("Subtract stack create 32-bit", this.ampImage, Call.run("Divide stack create 32-bit", this.yAccum, this.weightAccum));
        MS = CoreProcess.nonZerosNan(MS);
        this.mssrdImageStack = CoreProcess.maxOperations(MS, MS, "Divide");//I0 and I3
        ImagePlus x3 = CoreProcess.maxOperations(this.ampImage, this.ampImage, "Divide");
        if(this.order > 0){
            for(int i = 0; i < this.order; i++){
                ImagePlus I4 = Call.run("Subtract stack create 32-bit", x3, this.mssrdImageStack);
                ImagePlus I5 = CoreProcess.inverseSumOperation(I4);
                I5 = CoreProcess.maxOperations(I5, I5, "Divide");
                ImagePlus I6 = Call.run("Multiply stack create 32-bit", I5, this.mssrdImageStack);
                ImagePlus I7 = CoreProcess.maxOperations(I6, I6, "Divide");
                x3 = this.mssrdImageStack.duplicate();
                this.mssrdImageStack = I7.duplicate();
            }
        }
    }
    private void excludeOutliers(){
//        this.ampImage.show();
        for(int index = 0; index < this.mssrdImageStack.getNSlices(); index++){
            this.mssrdImageStack.setSlice(index+1);
            this.ampImage.setSlice(index+1);
            ImageStatistics statsMSSR = this.mssrdImageStack.getAllStatistics();
//            System.out.println(statsMSSR);
            ImageStatistics statsAMP = this.ampImage.getAllStatistics();
//            System.out.println(statsAMP);
            double[] histImageMSSR = statsMSSR.histogram();
            double cummCountsMSSR = 0;
            float valThresholdMSSR = 0;
            double[] histImageAMP = statsAMP.histogram();
            double cummCountsAMP = 0;
            float valThresholdAMP = 0;
            for(int i=0; i<histImageMSSR.length; i++){//Get min value over TH in MSSR image
                cummCountsMSSR = cummCountsMSSR + histImageMSSR[i];
                double ecdv = cummCountsMSSR/statsMSSR.pixelCount;
                if(ecdv > ((double)MSSR.thresholdOutL/100)){
                    valThresholdMSSR = (float) (statsMSSR.min + (i+1)*statsMSSR.binSize);
                    break;
                }
            }
            for(int i=0; i<histImageAMP.length; i++){//Get min value over TH in AMP image
                cummCountsAMP = cummCountsAMP + histImageAMP[i];
                double ecdv = cummCountsAMP/statsAMP.pixelCount;
                if(ecdv > ((double)MSSR.thresholdOutL/100)){
                    valThresholdAMP = (float) (statsAMP.min + (i+1)*statsAMP.binSize);
                    break;
                }
            }
//            System.out.println("valThresholdMSSR: " + valThresholdMSSR);
//            System.out.println("valThresholdAMP: " + valThresholdAMP);
            
            ImageProcessor ipMSSR = this.mssrdImageStack.getProcessor();
            ImageProcessor ipAMP = this.ampImage.getProcessor();
            float[][] faMSSR = ipMSSR.getFloatArray();
            float[][] faAMP = ipAMP.getFloatArray();
            for(int x=0; x<this.mssrdImageStack.getWidth(); x++){
                for(int y=0; y<this.mssrdImageStack.getHeight(); y++){
                    if(faMSSR[x][y] > valThresholdMSSR){
                        faMSSR[x][y] = valThresholdMSSR;
                    }
                    if(faAMP[x][y] > valThresholdAMP){
                        faAMP[x][y] = valThresholdAMP;
                    }
                }
            }

            ipMSSR.setFloatArray(faMSSR);
            ipAMP.setFloatArray(faAMP);
//            System.out.println("MSSR Max: " + this.mssrdImageStack.getAllStatistics().max);
//            System.out.println("AMP Max: " + this.ampImage.getAllStatistics().max);
        }
    }
}
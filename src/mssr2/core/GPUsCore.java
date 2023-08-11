package mssr2.core;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.Scaler;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Component;
import mssr2.process.CoreProcess;
import static mssr2.process.CoreProcess.getImgArray;
import static mssr2.process.CoreProcess.newStckImg;
import static mssr2.process.CoreProcess.padimgrrayMatrix;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;

public class GPUsCore extends Thread {
//    private final CLIJ2 clij2;
    public ImagePlus mssrdImageStack;
    private ImagePlus ampImage;
//    private float[][][] ampArr;
    private ImagePlus xPad;
    private ImagePlus M;
    private ImagePlus weightAccum;
    private ImagePlus yAccum;
    private ImagePlus imageBase;
    private final int hs;
    private final int amp;
    private final int order;
    private final boolean meshing;
    private final int desf;
    private final ImageCalculator Call;
    public long timeElapsed;
    private final Component cSuper;
    
    public GPUsCore(Component C, ImagePlus imgBase, int amp, float psf, int order, boolean prop){
        this.cSuper = C;
//        this.clij2 = clij2;
        //Definicion de parametros basicos
        this.imageBase = imgBase;
        this.hs = (int) Math.round(0.5*psf*amp);
        this.amp = amp;
        this.order = order;
        this.meshing = prop;
//        System.out.print("Meshing ");
//        System.out.println(this.prop);
        this.desf = (int) Math.ceil((float)this.amp/2);
        this.Call = new ImageCalculator();
    }
    public GPUsCore(Component C, ImageProcessor imgBase, int amp, float psf, int order, boolean prop){
        this.cSuper = C;
//        this.clij2 = clij2;
        //Definicion de parametros basicos
        this.imageBase = new ImagePlus("", imgBase);
        this.hs = (int) Math.round(0.5*psf*amp);
        this.amp = amp;
        this.order = order;
        this.meshing = prop;
//        System.out.print("Meshing ");
//        System.out.println(this.prop);
        this.desf = (int) Math.ceil((float)this.amp/2);
        this.Call = new ImageCalculator();
    }

    @Override
    public void run(){
        try {
            CoreProcess.clijGPU = new CLIJ2(new CLIJ(MSSR.gpuI));
            CoreProcess.clijGPU.clear();
            this.runAmp();
            System.out.println("\tAmplification end");
            this.imageBase = null;

            this.parMOfMax();
            System.out.println("\tMatrix of max end");

            this.parAccums();
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
            CoreProcess.clijGPU.clear();
            CoreProcess.clijGPU.close();
            CoreProcess.clijGPU = null;
        } catch (Exception e) {
            MSSR.resetData();
        }
    }
    private void runAmp(){
        //Conversion a 32bits
        if(this.imageBase.getBitDepth() != 32){
            ImageConverter iC = new ImageConverter(this.imageBase);
            iC.convertToGray32();
        }
        if(this.amp > 1){
            if(MSSR.interpType == 1){// Bicubic
                this.ampImage = Scaler.resize(this.imageBase, this.imageBase.getWidth()*amp, this.imageBase.getHeight()*amp, 1, "bicubic");
            } else {// Fourier
                this.ampImage = CoreProcess.StackInterpFourier(this.imageBase, this.amp);
            }
            if(this.meshing){
                this.parCompArtMatrix(1);
            }
        } else{
            this.ampImage = this.imageBase.duplicate();
        }
        this.xPad = CoreProcess.newStckImg(CoreProcess.padimgrrayMatrix(CoreProcess.getImgArray(this.ampImage), this.hs));
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
//    GPU
    private void parCompArtMatrix(float prop){
        CoreProcess.clijGPU.clear();
        ClearCLBuffer imgPad = CoreProcess.clijGPU.push(newStckImg(padimgrrayMatrix(getImgArray(this.ampImage), this.desf)));
        ClearCLBuffer imgT = CoreProcess.clijGPU.push(this.ampImage);
        ClearCLBuffer imgT2;
        ClearCLBuffer imgShift;
        ClearCLBuffer imgF;

        if(this.ampImage.getNSlices()>1){
            imgT2 = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), this.ampImage.getNSlices()}, NativeTypeEnum.Float);
            imgShift = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), this.ampImage.getNSlices()}, NativeTypeEnum.Float);
            imgF = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), this.ampImage.getNSlices()}, NativeTypeEnum.Float);
        } else{
            imgT2 = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            imgShift = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            imgF = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
        }

        CoreProcess.clijGPU.crop3D(imgPad, imgShift, 0, this.desf, 0);
        CoreProcess.clijGPU.addImagesWeighted(imgT, imgShift, imgT2, 1, prop);

        CoreProcess.clijGPU.crop3D(imgPad, imgShift, this.desf * 2, this.desf, 0);
        CoreProcess.clijGPU.addImagesWeighted(imgT2, imgShift, imgT, 1, prop);

        CoreProcess.clijGPU.crop3D(imgPad, imgShift, this.desf, 0, 0);
        CoreProcess.clijGPU.addImagesWeighted(imgT, imgShift, imgT2, 1, prop);

        CoreProcess.clijGPU.crop3D(imgPad, imgShift, this.desf, this.desf * 2, 0);
        CoreProcess.clijGPU.addImagesWeighted(imgT2, imgShift, imgT, 1, prop);

        CoreProcess.clijGPU.multiplyImageAndScalar(imgT, imgF, 1/(1 + (4*prop)));

        this.ampImage = CoreProcess.clijGPU.pull(imgF);
        CoreProcess.clijGPU.clear();
    }
    private void parMOfMax(){
        CoreProcess.clijGPU.clear();
        ClearCLBuffer AMP = CoreProcess.clijGPU.push(this.ampImage);
        ClearCLBuffer xPadGPU = CoreProcess.clijGPU.push(this.xPad);
        ClearCLBuffer MGPU;
//        ClearCLBuffer Mt;
        ClearCLBuffer xThis;
        ClearCLBuffer maxT;
        if(AMP.getDepth()>1){
            MGPU = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
//            Mt = clijGPU.create(new long[]{imgA.getWidth(), imgA.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            xThis = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            maxT = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
        } else{
            MGPU = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
//            Mt = clijGPU.create(new long[]{imgA.getWidth(), imgA.getHeight()}, NativeTypeEnum.Float);
            xThis = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            maxT = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
        }
        for(int i = -this.hs, porcentaje = 1; i<this.hs+1;i++, porcentaje++){
            for(int j = -this.hs; j<this.hs+1; j++){
                if((i!=0 || j!=0) && (Math.sqrt(Math.pow(i, 2) + Math.pow(j, 2)) <= this.hs)){
                    CoreProcess.clijGPU.crop3D(xPadGPU, xThis, this.hs + i, this.hs + j, 0);
                    CoreProcess.clijGPU.absoluteDifference(AMP, xThis, maxT);
                    CoreProcess.clijGPU.copy(MGPU, xThis);
//                    clijGPU.copy(M, Mt);
//                    clijGPU.maximumImages(Mt, maxT, M);
                    CoreProcess.clijGPU.maximumImages(xThis, maxT, MGPU);
                }
            }

            if((int)(porcentaje*((float)100/(this.hs+this.hs+1))) % 10 == 0){
                System.out.println("Matrix of max " + (int)(porcentaje*((float)100/(this.hs+this.hs+1))) + "%");
            }
        }
//        System.out.println(CoreProcess.clijGPU.reportMemory());
        this.M = CoreProcess.clijGPU.pull(MGPU);
        CoreProcess.clijGPU.clear();
    }
    private void parAccums(){
        CoreProcess.clijGPU.clear();
        ClearCLBuffer AMP = CoreProcess.clijGPU.push(this.ampImage);
        ClearCLBuffer xPadGPU = CoreProcess.clijGPU.push(this.xPad);
        ClearCLBuffer MGPU = CoreProcess.clijGPU.push(this.M);
        ClearCLBuffer weightAccumGPU;
        ClearCLBuffer weightAccumt;
        ClearCLBuffer yAccumGPU;
        ClearCLBuffer yAccumt;
        ClearCLBuffer xThis;
        ClearCLBuffer temporalImage;
        ClearCLBuffer temporalImage2;
        ClearCLBuffer multWeightThis;
//        System.out.println("-----------------");
//        System.out.println(CoreProcess.clijGPU.reportMemory());
        if(AMP.getDepth()>1){
            weightAccumGPU = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            weightAccumt = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            yAccumGPU = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            yAccumt = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            xThis = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            temporalImage = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            temporalImage2 = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
            multWeightThis = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight(), AMP.getDepth()}, NativeTypeEnum.Float);
        } else{
            weightAccumGPU = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            weightAccumt = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            yAccumGPU = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            yAccumt = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            xThis = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            temporalImage = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            temporalImage2 = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
            multWeightThis = CoreProcess.clijGPU.create(new long[]{this.ampImage.getWidth(), this.ampImage.getHeight()}, NativeTypeEnum.Float);
        }
        for(int i = -this.hs, porcentaje = 1; i<this.hs+1;i++, porcentaje++){
            for(int j = -this.hs; j<this.hs+1; j++){
                if((i!=0 || j!=0) && (Math.sqrt(Math.pow(i, 2) + Math.pow(j, 2)) <= this.hs)){
//                    System.out.println("i: "+ i +" j: "+ j);
                    double spatialKernel = Math.exp(-(i*i + j*j) / (this.hs*this.hs));
                    CoreProcess.clijGPU.crop3D(xPadGPU, xThis, this.hs + i, this.hs + j, 0);
                    CoreProcess.clijGPU.subtractImages(AMP, xThis, temporalImage);
                    CoreProcess.clijGPU.divideImages(temporalImage, MGPU, temporalImage2);
                    CoreProcess.clijGPU.multiplyImages(temporalImage2, temporalImage2, temporalImage);
                    CoreProcess.clijGPU.multiplyImageAndScalar(temporalImage, temporalImage2, -1);
                    CoreProcess.clijGPU.exponential(temporalImage2, temporalImage);
//                    ImagePlus IPintensityKernel = clijGPU.pull(negxDiffSq0);
//                    IJ.run(IPintensityKernel, "Exp", "stack");
//                    ClearCLBuffer intensityKernel = clijGPU.push(IPintensityKernel);
                    CoreProcess.clijGPU.multiplyImageAndScalar(temporalImage, temporalImage2, spatialKernel);

                    CoreProcess.clijGPU.copy(weightAccumGPU, weightAccumt);
                    CoreProcess.clijGPU.addImages(weightAccumt, temporalImage2, weightAccumGPU);

                    CoreProcess.clijGPU.copy(yAccumGPU, yAccumt);
                    CoreProcess.clijGPU.multiplyImages(xThis, temporalImage2, multWeightThis);
                    CoreProcess.clijGPU.addImages(yAccumt, multWeightThis, yAccumGPU);
//                    i: -30 j: 0
//                    i: 30 j: 0
                }
            }
            if((int)(porcentaje*((float)100/(this.hs+this.hs+1))) % 10 == 0){
                System.out.println("Accums " + (int)(porcentaje*((float)100/(this.hs+this.hs+1))) + "%");
            }
        }
        this.weightAccum = CoreProcess.clijGPU.pull(weightAccumGPU);
        this.yAccum = CoreProcess.clijGPU.pull(yAccumGPU);
        CoreProcess.clijGPU.clear();
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
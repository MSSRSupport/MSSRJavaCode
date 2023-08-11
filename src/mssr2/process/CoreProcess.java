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
package mssr2.process;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import mssr2.core.MSSR;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GetGPUProperties;
import net.haesleinhuepf.clijx.plugins.ForwardFFT;
import net.haesleinhuepf.clijx.plugins.InverseFFT;

/**
 *
 * @author raul_
 */
public class CoreProcess{
    public static ImageCalculator Call = new ImageCalculator();
    public static CLIJ2 clijGPU = null;
//   GPU
    public static float[][][] getImgArray(ImagePlus imgA){
        float[][][] imgT = new float[imgA.getNSlices()][imgA.getWidth()][imgA.getHeight()];
        for(int i = 0; i < imgA.getNSlices(); i++){
            imgA.setSlice(i+1);
            imgT[i] = imgA.getProcessor().getFloatArray();
        }
        return imgT;
    }
    public static float[][][] padimgrrayMatrix(float[][][] imgT, int hs){
        int nSlices = imgT.length;
        int width = imgT[0].length;
        int height = imgT[0][0].length;
        float[][][] padimgT = new float[nSlices][width + hs + hs][height + hs + hs];
        for(int z = 0; z < nSlices; z++){
            padimgT[z] = padimgrray(imgT, z, width, height, hs);
//            padimgT[z] = this.padimgrray(imgT, z, width, hs);
        }
        return padimgT;
    }
    public static float[][] padimgrray(float[][][] img, int z, int lnw, int lnh, int hs){
        float[][] padimgT = new float[lnw + hs + hs][lnh + hs + hs];
//        1
        for(int i = 0; i < hs; i++){
            for(int j = 0; j < hs; j++){
                padimgT[i][j] = img[z][hs-1-i][hs-1-j];
            }
        }
//        2
        for(int i = hs; i < hs+lnw; i++){
            for(int j = 0; j < hs; j++){
                padimgT[i][j] = img[z][i-hs][hs-1-j];
            }
        }
//        3
        for(int i = hs+lnw; i < hs+hs+lnw; i++){
            for(int j = 0; j < hs; j++){
                padimgT[i][j] = img[z][hs+lnw+lnw-1-i][hs-1-j];
            }
        }
//        4
        for(int i = 0; i < hs; i++){
            for(int j = hs; j < hs+lnh; j++){
                padimgT[i][j] = img[z][hs-1-i][j-hs];
            }
        }
//        5
        for(int i = hs; i < hs+lnw; i++){
            for(int j = hs; j < hs+lnh; j++){
                padimgT[i][j] = img[z][i-hs][j-hs];
            }
        }
//        6
        for(int i = hs+lnw; i < hs+hs+lnw; i++){
            for(int j = hs; j < hs+lnh; j++){
                padimgT[i][j] = img[z][hs+lnw+lnw-1-i][j-hs];
            }
        }
//        7
        for(int i = 0; i < hs; i++){
            for(int j = hs+lnh; j < hs+hs+lnh; j++){
                padimgT[i][j] = img[z][hs-1-i][hs+lnh+lnh-1-j];
            }
        }
//        8
        for(int i = hs; i < hs+lnw; i++){
            for(int j = hs+lnh; j < hs+hs+lnh; j++){
                padimgT[i][j] = img[z][i-hs][hs+lnh+lnh-1-j];
            }
        }
//        9
        for(int i = hs+lnw; i < hs+hs+lnw; i++){
            for(int j = hs+lnh; j < hs+hs+lnh; j++){
                padimgT[i][j] = img[z][hs+lnw+lnw-1-i][hs+lnh+lnh-1-j];
            }
        }
        return padimgT;
    }
    ////////////////////////
    ///// Por Corregir /////
    ////////////////////////
    public static ImagePlus compArtMatrix(ImagePlus imgA, int desf, float prop){
        ImagePlus imgT = newStckImg(padimgrrayMatrix(getImgArray(imgA), desf));
        imgT.setRoi(0, desf, imgA.getWidth(), imgA.getHeight());
        ImagePlus imgTHR = imgT.crop("stack");
        IJ.run(imgTHR, "Multiply...", "value="+String.valueOf(prop)+" stack");

        imgT.setRoi(desf * 2, desf, imgA.getWidth(), imgA.getHeight());
        ImagePlus imgTHL = imgT.crop("stack");
        IJ.run(imgTHL, "Multiply...", "value="+String.valueOf(prop)+" stack");

        imgT.setRoi(desf, 0, imgA.getWidth(), imgA.getHeight());
        ImagePlus imgTVU = imgT.crop("stack");
        IJ.run(imgTVU, "Multiply...", "value="+String.valueOf(prop)+" stack");

        imgT.setRoi(desf, desf * 2, imgA.getWidth(), imgA.getHeight());
        ImagePlus imgTVD = imgT.crop("stack");
        IJ.run(imgTVD, "Multiply...", "value="+String.valueOf(prop)+" stack");

        imgT.setRoi(desf, desf, imgA.getWidth(), imgA.getHeight());
        imgT = imgT.crop("stack");

        ImagePlus imgRL = Call.run("Add stack create 32-bit", imgTHR, imgTHL);
        ImagePlus imgUD = Call.run("Add stack create 32-bit", imgTVU, imgTVD);
        ImagePlus imgRLUD = Call.run("Add stack create 32-bit", imgRL, imgUD);
        ImagePlus imgF = Call.run("Add stack create 32-bit", imgT, imgRLUD);
        IJ.run(imgF, "Divide...", "value="+String.valueOf(1 + (4*prop))+" stack");
        return newStckImg(getImgArray(imgF));
    }
    public static ImagePlus nonZerosNan(ImagePlus imgT){
        float[][][] MSarr = getImgArray(imgT);
        int nSlices = imgT.getNSlices();
        int width = imgT.getWidth();
        int height = imgT.getHeight();
        for(int z = 0; z < nSlices; z++){
            for(int i = 0; i < width; i++){
                for(int j = 0; j < height; j++){
                    if(MSarr[z][i][j] < 0 || !Float.isFinite(MSarr[z][i][j])){
                        MSarr[z][i][j] = 0;
                    }
                }
            }
        }
        return newStckImg(MSarr);
    }
    public static ImagePlus maxOperations(ImagePlus imgO, ImagePlus base, String operation){
        ImagePlus imgT = imgO.duplicate();
//        System.out.println("---------------------------");
        for(int z = 0; z < imgT.getNSlices(); z++){
            imgT.setSlice(z+1);
            base.setSlice(z+1);
//            System.out.println("Img " + (z+1) + ": " + base.getProcessor().getStats().max);
            IJ.run(imgT, operation + "...", "value="+String.valueOf(base.getProcessor().getStats().max));//intensityKernel
        }
        return imgT;
    }
    public static ImagePlus inverseSumOperation(ImagePlus imgO){
        ImagePlus imgT = imgO.duplicate();
        IJ.run(imgT, "Multiply...", "value="+String.valueOf(-1)+" stack");
        for(int z = 0; z < imgT.getNSlices(); z++){
            imgT.setSlice(z+1);
            IJ.run(imgT, "Add...", "value="+String.valueOf(imgT.getProcessor().getStats().min * -1));//intensityKernel
        }
        return imgT;
    }
    public static ImagePlus newStckImg(float[][][] matImg){
        ImageStack imgStck = new ImageStack(matImg[0].length, matImg[0][0].length);
        for(int z = 0; z < matImg.length; z++){
            ImageProcessor imgProc = new FloatProcessor(matImg[0].length, matImg[0][0].length);
            imgProc.setFloatArray(matImg[z]);
            imgStck.addSlice("", imgProc, z);
        }
        return new ImagePlus("", imgStck);
    }
    public static ImagePlus temporalAnalysis(ImagePlus imgT, int typeTA){
        ImagePlus mssrdImage;
        switch (typeTA) {
            case 1:
                mssrdImage = TPM(imgT);
                mssrdImage.setTitle(MSSR.getNameT("TPM"));
                break;
            case 2:
                mssrdImage = ZProjector.run(imgT, "sd");
                mssrdImage = Call.run("Multiply create 32-bit", mssrdImage, mssrdImage);
                mssrdImage.setTitle(MSSR.getNameT("Var"));
                break;
            case 3:
                mssrdImage = ZProjector.run(imgT, "avg");
                mssrdImage.setTitle(MSSR.getNameT("Mean"));
                break;
            case 4:
                mssrdImage = CoeffVar(imgT);
                mssrdImage.setTitle(MSSR.getNameT("CoefficientVariation"));
                break;
            case 5:
                mssrdImage = TRACK(imgT, 2);
                mssrdImage.setTitle(MSSR.getNameT("SOFI2"));
                break;
            case 6:
                mssrdImage = TRACK(imgT, 3);
                mssrdImage.setTitle(MSSR.getNameT("SOFI3"));
                break;
            case 7:
                mssrdImage = TRACK(imgT, 4);
                mssrdImage.setTitle(MSSR.getNameT("SOFI4"));
                break;
            default:
                System.out.println("No hay TA");
                return imgT;
        }
        mssrdImage.resetDisplayRange();
        return mssrdImage;
//        mssrdImage.show();
    }
    public static ImagePlus TPM(ImagePlus imgT){
        ImagePlus sumTPM = newStckImg(new float[1][imgT.getWidth()][imgT.getHeight()]);
        ImagePlus mssrdImage = newStckImg(new float[1][imgT.getWidth()][imgT.getHeight()]);
        long startTime = System.nanoTime();
        for(int i = 1; i <= imgT.getNSlices(); i++){
            imgT.setSlice(i);
            sumTPM = Call.run("Add create 32-bit", sumTPM, imgT);
        }
        for(int i = 1; i <= imgT.getNSlices(); i++){
            imgT.setSlice(i);
            mssrdImage = Call.run("Add create 32-bit", mssrdImage, Call.run("Multiply create 32-bit", imgT, sumTPM));
        }
        System.out.println("Tiempo de TPM: " + ((System.nanoTime() - startTime)/ 1000000000) + " seg\n");
        return mssrdImage;
    }
    public static ImagePlus CoeffVar(ImagePlus imgBase){
        ImagePlus meanImage = ZProjector.run(imgBase, "avg");
        ImagePlus varImage = ZProjector.run(imgBase, "sd");
        varImage = Call.run("Multiply create 32-bit", varImage, varImage);
        ImagePlus CVImage = Call.run("Divide create 32-bit stack", varImage, meanImage);
        return CVImage;
    }
    public static ImagePlus TRACK(ImagePlus imgT, int ord){
        ImagePlus deltaI = Call.run("Difference create 32-bit stack", imgT, ZProjector.run(imgT, "avg"));
        if(ord == 2){
            ImagePlus Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            return ZProjector.run(Mult1, "avg");
        } else if(ord == 3){
            ImagePlus Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, reArrangeStack(deltaI, 3));
            return ZProjector.run(Mult1, "avg");
        } else if(ord == 4){
            ImagePlus imgF = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            imgF = Call.run("Multiply create 32-bit stack", imgF, reArrangeStack(deltaI, 3));
            imgF = Call.run("Multiply create 32-bit stack", imgF, reArrangeStack(deltaI, 4));
            imgF = ZProjector.run(imgF, "avg");

            ImagePlus imgforDif = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            imgforDif = ZProjector.run(imgforDif, "avg");
            ImagePlus Mult2 = Call.run("Multiply create 32-bit stack", reArrangeStack(deltaI, 3), reArrangeStack(deltaI, 4));
            Mult2 = ZProjector.run(Mult2, "avg");
            imgforDif = Call.run("Multiply create 32-bit stack", imgforDif, Mult2);

            ImagePlus Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 3));
            Mult1 = ZProjector.run(Mult1, "avg");
            Mult2 = Call.run("Multiply create 32-bit stack", reArrangeStack(deltaI, 2), reArrangeStack(deltaI, 4));
            Mult2 = ZProjector.run(Mult2, "avg");
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, Mult2);
            imgforDif = Call.run("Add create 32-bit stack", imgforDif, Mult1);

            Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 4));
            Mult1 = ZProjector.run(Mult1, "avg");
            Mult2 = Call.run("Multiply create 32-bit stack", reArrangeStack(deltaI, 2), reArrangeStack(deltaI, 3));
            Mult2 = ZProjector.run(Mult2, "avg");
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, Mult2);
            imgforDif = Call.run("Add create 32-bit stack", imgforDif, Mult1);
            imgF = Call.run("Difference create 32-bit stack", imgF, imgforDif);

            return imgF;
        }
        return null;
    }
    public static ImagePlus TRACK1(ImagePlus imgT, int ord){
//        ImagePlus Mult1 = Call.run("Multiply create 32-bit", subRangeStack(DifImage, 1, numImages-1), subRangeStack(DifImage, 2, numImages));
        ImagePlus deltaI = Call.run("Difference create 32-bit stack", imgT, ZProjector.run(imgT, "avg"));
        if(ord == 2){
            ImagePlus Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            return ZProjector.run(Mult1, "avg");
        } else if(ord == 3){
            ImagePlus Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, reArrangeStack(deltaI, 3));
            return ZProjector.run(Mult1, "avg");
        } else if(ord == 4){
            ImagePlus imgF = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            imgF = Call.run("Multiply create 32-bit stack", imgF, reArrangeStack(deltaI, 3));
            imgF = Call.run("Multiply create 32-bit stack", imgF, reArrangeStack(deltaI, 4));
            imgF = ZProjector.run(imgF, "avg");

            ImagePlus Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 2));
            Mult1 = ZProjector.run(Mult1, "avg");
            ImagePlus Mult2 = Call.run("Multiply create 32-bit stack", reArrangeStack(deltaI, 3), reArrangeStack(deltaI, 4));
            Mult2 = ZProjector.run(Mult2, "avg");
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, Mult2);
            imgF = Call.run("Subtract create 32-bit stack", imgF, Mult1);

            Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 3));
            Mult1 = ZProjector.run(Mult1, "avg");
            Mult2 = Call.run("Multiply create 32-bit stack", reArrangeStack(deltaI, 2), reArrangeStack(deltaI, 4));
            Mult2 = ZProjector.run(Mult2, "avg");
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, Mult2);
            imgF = Call.run("Subtract create 32-bit stack", imgF, Mult1);

            Mult1 = Call.run("Multiply create 32-bit stack", deltaI, reArrangeStack(deltaI, 4));
            Mult1 = ZProjector.run(Mult1, "avg");
            Mult2 = Call.run("Multiply create 32-bit stack", reArrangeStack(deltaI, 2), reArrangeStack(deltaI, 3));
            Mult2 = ZProjector.run(Mult2, "avg");
            Mult1 = Call.run("Multiply create 32-bit stack", Mult1, Mult2);
            imgF = Call.run("Difference create 32-bit stack", imgF, Mult1);

            return imgF;
        }
        return null;
    }
    public static ImagePlus TPMOld(ImagePlus imgT){
        int nSlices = imgT.getNSlices();
        int width = imgT.getWidth();
        int height = imgT.getHeight();
        ImagePlus mssrdImage = newStckImg(new float[1][width][height]);
        ImagePlus imgJ =  imgT.duplicate();
        long startTime = System.nanoTime();
        for(int i = 0; i < nSlices; i++){
            imgT.setSliceWithoutUpdate(i+1);
            for(int j = 0; j < nSlices; j++){
                imgJ.setSliceWithoutUpdate(j+1);
                mssrdImage = Call.run("Add create 32-bit", mssrdImage, Call.run("Multiply create 32-bit", imgT, imgJ));
            }
        }
        System.out.println("Tiempo de TPM: " + ((System.nanoTime() - startTime)/ 1000000000) + " seg\n");
        return mssrdImage;
    }
//    Batches
    public static ImagePlus subRangeStack(ImagePlus img, int ini, int end){
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight());
        for(int i=ini; i<=end; i++){
            img.setSlice(i);
            stack.addSlice("", img.getProcessor().duplicate());
        }
        return new ImagePlus("", stack);
    }
    public static ImagePlus reArrangeStack(ImagePlus img, int ini){
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight());
        int end = img.getNSlices();
        for(int i=ini; i<=end; i++){
            img.setSlice(i);
            stack.addSlice("", img.getProcessor().duplicate());
        }
        for(int i=1; i<ini; i++){
            img.setSlice(i);
            stack.addSlice("", img.getProcessor().duplicate());
        }
        return new ImagePlus("", stack);
    }
    public static ArrayList<ImagePlus> stack2stacks(ImagePlus imp, int numImagesPerFrame, int numImages, int NMod) {
        ImageStack stack = imp.getStack();
        int currentSlice = imp.getCurrentSlice();   // to reset ***
        ArrayList<ImagePlus> batchImages = new ArrayList<>();
        ColorModel cm = imp.createLut().getColorModel();
        for(int nI=0; nI<numImages; nI++) {
            ImageStack ims = new ImageStack(stack.getWidth(), stack.getHeight(), cm);
            for(int index=1; index<=numImagesPerFrame; index++) {
//                    System.out.println("" + nss + " " + index);
                imp.setSlice((nI*numImagesPerFrame)+index);
                ims.addSlice("", imp.getProcessor().duplicate());
            }
            batchImages.add(new ImagePlus(""+nI, ims));
//                this.batchImages.get(nss).show();
//                System.out.println("Ciclo");
        }
        if(NMod!=0){
            ImageStack ims = new ImageStack(stack.getWidth(), stack.getHeight(), cm);
            for(int index=(numImagesPerFrame*numImages)+1; index<=imp.getNSlices(); index++) {
                System.out.println("" + index);
                imp.setSlice(index);
                ims.addSlice("", imp.getProcessor().duplicate());
            }
            batchImages.add(new ImagePlus(""+(numImages), ims));
//                this.batchImages.get((this.batchImages.size()-1)).show();
        }
        imp.setSlice(currentSlice); // ***
        return batchImages;
    }
    public static ImagePlus staks2stack(ImagePlus[] batchMSSRD, int numImages, int w, int h){
        ImageStack ims = new ImageStack(w, h);
        for(int nI = 0; nI < numImages; nI++){
            for(int index = 1; index <= batchMSSRD[nI].getNSlices(); index++){
                batchMSSRD[nI].setSlice(index);
                ims.addSlice("", batchMSSRD[nI].getProcessor().duplicate());
            }
            batchMSSRD[nI] = null;
        }
        return new ImagePlus("MSSRD", ims);
    }
    public static ImagePlus TPMBatch(ImagePlus[] imgBatch, int width, int height){
        ImagePlus mssrdImage = newStckImg(new float[1][width][height]);
        for(int nI = 0; nI < imgBatch.length; nI++){
            System.out.println("Lote: " + (nI+1));
            for(int s = 1; s <= imgBatch[nI].getNSlices(); s++){
                ImagePlus imgRep = newStckImg(new float[1][width][height]);
                for(int nI2 = 0; nI2 < imgBatch.length; nI2++){
//                    Si la img rep es de distinta long se regenera
                    if(imgRep.getNSlices()!=imgBatch[nI2].getNSlices()){
                        imgRep = null;
                        ImageStack stack = new ImageStack(width, height);
                        imgBatch[nI].setSlice(s);
                        for(int d = 1; d <= imgBatch[nI2].getNSlices(); d++){
                            stack.addSlice("", imgBatch[nI].getProcessor().duplicate());
                        }
                        imgRep = new ImagePlus("", stack);
                        mssrdImage = Call.run("Add create 32-bit", mssrdImage, ZProjector.run(Call.run("Multiply create 32-bit", imgRep, imgBatch[nI2]), "sum"));
                    }
                }
            }
        }
        return mssrdImage;
    }
//    FFT
    public static ImagePlus StackInterpFourier(ImagePlus iP, int amp){
        ImageStack imgStck = new ImageStack(iP.getWidth()*amp, iP.getHeight()*amp);
        System.out.println(iP.getNSlices());
        for( int i = 0; i<iP.getNSlices(); i++){
            iP.setSlice(i+1);
            ImageProcessor imgProc = fftMSSR.FourierInterp(iP.getProcessor(), amp);
            imgStck.addSlice("", imgProc, i);
        }
        return new ImagePlus("", imgStck);
    }
}

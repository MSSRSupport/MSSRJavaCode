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

import mssr2.gui.MGUI;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import mssr2.core.CPUsCore;
import mssr2.core.GPUsCore;
import mssr2.core.MSSR;
//import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij2.CLIJ2;

/**
 *
 * @author raul_
 */
public class Core extends Thread {
    private final Component cSuper;
    private ImagePlus mssrdImageStack;
    private ImagePlus tMSSR;

    public Core(Component C){
        this.cSuper = C;
    }

    @Override
    public void run(){
//        boolean dimximg = (63000) >= (dims[0]*MGUI.ampVal * dims[1]*MGUI.ampVal);
        if(MSSR.dirs){
            MSSR.imgBase = null;
//            System.out.println("Directory!!");
            this.launchBatches();
        } else if(!MSSR.parallel){
            if(cpuValidation()){
                manageCPU(true, MSSR.dirs);
            } else{
                manageCPU(false, MSSR.dirs);
            }
        } else{
            if(gpuValidation()){
                manageGPU(true, MSSR.dirs);
            } else{
                manageGPU(false, MSSR.dirs);
            }
        }
    }
//    CPU
    public void manageCPU(boolean validation, boolean batches){
        long startTime = System.nanoTime();
        if(validation){
            System.out.println("In Stack");
            this.mssrdImageStack = launchCPU_Stack(MSSR.dirs);
        } else{
            System.out.println("One by one");
            this.mssrdImageStack = launchCPU_One(MSSR.dirs);
        }
        this.mssrdImageStack.setTitle(MSSR.getNameT(""));
        if(MSSR.typeATon){
            System.out.println("\tTemporal Analisis");
            this.tMSSR = CoreProcess.temporalAnalysis(this.mssrdImageStack, MSSR.typeTAMemory);
        }
        if(!batches){
            this.mssrdImageStack.resetDisplayRange();
            this.mssrdImageStack.setSlice(1);
            this.mssrdImageStack.show();
            if(MSSR.typeATon){
                this.tMSSR.show();
            }
            MSSR.resetData();
            long timeElapsed = System.nanoTime() - startTime;
            System.out.println("Execution time : " + timeElapsed / 1000000 + " ms");
            JOptionPane.showMessageDialog(this.cSuper, "Execution time: " + timeElapsed / 1000000 + " ms\n" +
                    "#Frames: " + this.mssrdImageStack.getNSlices());
        }
    }
    public ImagePlus launchCPU_One(boolean batches){
        ImageStack imSF = new ImageStack(MSSR.imgBase.getWidth()*MSSR.ampVal, MSSR.imgBase.getHeight()*MSSR.ampVal);
        CPUsCore cpuP;
        for(int index = 1; index <= MSSR.imgBase.getNSlices(); index++){
            System.out.println("Slice #" + index);
            MSSR.imgBase.setSlice(index);
            cpuP = new CPUsCore(this.cSuper, MSSR.imgBase.getProcessor().duplicate(), MSSR.ampVal, MSSR.psfVal, MSSR.ordVal, MSSR.meshSt);
            cpuP.start();
            try {
                cpuP.join();
                imSF.addSlice(cpuP.mssrdImageStack.getProcessor());
            } catch (InterruptedException ex) {
                MSSR.resetData();
                Logger.getLogger(MGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        MSSR.imgBase = null;
        return new ImagePlus(MSSR.getNameT(""), imSF);
    }
    public ImagePlus launchCPU_Stack(boolean batches){
        CPUsCore cpuP = new CPUsCore(this.cSuper, MSSR.imgBase.duplicate(), MSSR.ampVal, MSSR.psfVal, MSSR.ordVal, MSSR.meshSt);
        cpuP.start();
        try {
            cpuP.join();
            return cpuP.mssrdImageStack;
        } catch (InterruptedException ex) {
            MSSR.resetData();
            Logger.getLogger(MGUI.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
//    GPU
    public void manageGPU(boolean validation, boolean batches){
        long startTime = System.nanoTime();
        if(validation){
            System.out.println("In Stack");
            this.mssrdImageStack = launchGPU_Stack(MSSR.dirs);
        } else{
            System.out.println("One by one");
            this.mssrdImageStack = launchGPU_One(MSSR.dirs);
        }
        this.mssrdImageStack.setTitle(MSSR.getNameT(""));
        if(MSSR.typeATon){
            System.out.println("\tTemporal Analisis");
            this.tMSSR = CoreProcess.temporalAnalysis(this.mssrdImageStack, MSSR.typeTAMemory);
        }
        if(!batches){
            this.mssrdImageStack.resetDisplayRange();
            this.mssrdImageStack.setSlice(1);
            this.mssrdImageStack.show();
            if(MSSR.typeATon){
                this.tMSSR.show();
            }
            MSSR.resetData();
            long timeElapsed = System.nanoTime() - startTime;
            System.out.println("Execution time : " + timeElapsed / 1000000 + " ms");
            JOptionPane.showMessageDialog(this.cSuper, "Execution time: " + timeElapsed / 1000000 + " ms\n" +
                    "#Frames: " + this.mssrdImageStack.getNSlices());
        }
    }
    public ImagePlus launchGPU_One(boolean batches){
        ImageStack imSF = new ImageStack(MSSR.imgBase.getWidth()*MSSR.ampVal, MSSR.imgBase.getHeight()*MSSR.ampVal);
        GPUsCore gpuP;
        for(int index = 1; index <= MSSR.imgBase.getNSlices(); index++){
            System.out.println("Slice #" + index);
            MSSR.imgBase.setSlice(index);
            gpuP = new GPUsCore(this.cSuper, MSSR.imgBase.getProcessor().duplicate(), MSSR.ampVal, MSSR.psfVal, MSSR.ordVal, MSSR.meshSt);
            gpuP.start();
            try {
                gpuP.join();
                imSF.addSlice(gpuP.mssrdImageStack.getProcessor());
            } catch (InterruptedException ex) {
                MSSR.resetData();
                Logger.getLogger(MGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        MSSR.imgBase = null;
        return new ImagePlus(MSSR.getNameT(""), imSF);
    }
//    public ImagePlus launchGPU_SubStacks(){
//        long startTime = System.nanoTime();
//        int[] dims = MSSR.imgBase.getDimensions();//(width, height, nChannels, nSlices, nFrames)
//        int validation = dims[0]*dims[1]*MGUI.ampVal;
//        int nItP = 1300000 / validation;
//        int nCiclosCompletos = dims[3]/nItP;
//        int nCiclosIncompletos = dims[3] - (nItP*nCiclosCompletos);
//
//        CoreProcess.clijGPU = new CLIJ2(new CLIJ(MSSR.gpuI));
//        CoreProcess.clijGPU.clear();
//        System.out.println(CoreProcess.clijGPU.getGPUName());
//        GPUsCore gpuP;
//
////        img2 = new Concatenator().concatenate(imgTest, img2, false);
////        this.mssrdImageStack = new ImagePlus();
//        for(int index = 0; index < nCiclosCompletos; index++){
//            ImagePlus SubSet = new Duplicator().run(MSSR.imgBase, (1+(nItP*index)), (nItP*(index+1)));
//            gpuP = new GPUsCore(this.cSuper, SubSet.duplicate(), MGUI.ampVal, MGUI.psfVal, MGUI.ordVal, MSSR.meshSt);
//            gpuP.start();
//            try {
//                gpuP.join();
//                if(index == 0){
//                    this.mssrdImageStack = gpuP.mssrdImageStack.duplicate();
//                }
//                else{
//                    this.mssrdImageStack = new Concatenator().concatenate(this.mssrdImageStack, gpuP.mssrdImageStack, false);
//                }
//                System.out.println(Arrays.toString(this.mssrdImageStack.getDimensions()));
//            } catch (InterruptedException ex) {
//                MSSR.resetData();
//                Logger.getLogger(MGUI.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        this.mssrdImageStack.setDimensions(1,this.mssrdImageStack.getStackSize(),1);
//        if(nCiclosIncompletos > 0){
//            ImagePlus SubSet = new Duplicator().run(MSSR.imgBase, (1+(nItP*nCiclosCompletos)), dims[3]);
//            gpuP = new GPUsCore(this.cSuper, SubSet.duplicate(), MGUI.ampVal, MGUI.psfVal, MGUI.ordVal, MSSR.meshSt);
//            gpuP.start();
//            try {
//                gpuP.join();
//                this.mssrdImageStack = new Concatenator().concatenate(this.mssrdImageStack, gpuP.mssrdImageStack, false);
//            } catch (InterruptedException ex) {
//                MSSR.resetData();
//                Logger.getLogger(MGUI.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        MSSR.imgBase = null;
//        if(MSSR.typeSt != 0){
//            System.out.println("\tTemporal Analisis");
//            CoreProcess.temporalAnalysis(this.mssrdImageStack, MSSR.typeSt);
//        }
//        MSSR.imgBase = null;
//        this.mssrdImageStack.setSlice(1);
//        this.mssrdImageStack.resetDisplayRange();
////        System.err.println("Show");
////        System.err.println(Arrays.toString(this.mssrdImageStack.getDimensions()));
//        this.mssrdImageStack.show();
//        MSSR.resetData();
//        long timeElapsed = System.nanoTime() - startTime;
//        System.out.println("Execution time : " + timeElapsed / 1000000 + " miliseg");
//        JOptionPane.showMessageDialog(this.cSuper, "Execution time: " + timeElapsed / 1000000 + " miliseg\n" +
//                "#Frames: " + this.mssrdImageStack.getNSlices());
//    }
    public ImagePlus launchGPU_Stack(boolean batches){
//        System.out.println(CoreProcess.clijGPU.getGPUName());
        GPUsCore gpuP = new GPUsCore(this.cSuper, MSSR.imgBase.duplicate(), MSSR.ampVal, MSSR.psfVal, MSSR.ordVal, MSSR.meshSt);
        gpuP.start();
        try {
            gpuP.join();
            return gpuP.mssrdImageStack;
        } catch (InterruptedException ex) {
            MSSR.resetData();
            Logger.getLogger(MGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
//    Dirs
    public void launchBatches(){
        File dirSave = new File(MSSR.pathSaveBatches);
        if(!dirSave.exists()){
            System.out.println("----- Creating directory -----");
            dirSave.mkdir();
        }
        long startTime = System.nanoTime();
        if(!MSSR.parallel){
            for(String filesList : MSSR.filesList) {
                String imgBatch = MSSR.pathBatches + "\\" + filesList;
                MSSR.imgTitle = filesList;
                System.out.println("\nImage to analyze: " + imgBatch);

                MSSR.imgBase = IJ.openImage(imgBatch);
                if(cpuValidation()){
                    manageCPU(true, MSSR.dirs);
                } else{
                    manageCPU(false, MSSR.dirs);
                }
                IJ.saveAsTiff(this.mssrdImageStack, MSSR.pathSaveBatches+ "\\" + this.mssrdImageStack.getTitle());
                if(MSSR.typeTAMemory != 0){
                    IJ.saveAsTiff(this.tMSSR, MSSR.pathSaveBatches+ "\\" + this.tMSSR.getTitle());
                }
            }
        } else{
            for(String filesList : MSSR.filesList) {
                String imgBatch = MSSR.pathBatches + "\\" + filesList;
                MSSR.imgTitle = filesList;
                System.out.println("Image to analyze: " + imgBatch);
                
                MSSR.imgBase = IJ.openImage(imgBatch);
                if(gpuValidation()){
                    manageGPU(true, MSSR.dirs);
                } else{
                    manageGPU(false, MSSR.dirs);
                }
                IJ.saveAsTiff(this.mssrdImageStack, MSSR.pathSaveBatches+ "\\" + this.mssrdImageStack.getTitle());
                if(MSSR.typeTAMemory != 0){
                    IJ.saveAsTiff(this.tMSSR, MSSR.pathSaveBatches+ "\\" + this.tMSSR.getTitle());
                }
            }
        }
        long timeElapsed = System.nanoTime() - startTime;
        int len = MSSR.filesList.length;
        MSSR.resetData();
        System.out.println("Execution time : " + timeElapsed / 1000000000 + " seg #Images: " + len);
        JOptionPane.showMessageDialog(this.cSuper, "Execution time: " + timeElapsed / 1000000000 + " seg\n" +
                "#Images: " + len);
    }
    private boolean cpuValidation(){
        int[] dims = MSSR.imgBase.getDimensions();
        System.out.println(Arrays.toString(dims));
        long validation = dims[0]*dims[1]*dims[2]*dims[3]*dims[4]*MSSR.ampVal*MSSR.ampVal/10;
        long MemoryRunT = Runtime.getRuntime().freeMemory()/10;
        return validation<=MemoryRunT && validation>0;
    }
    private boolean gpuValidation(){
        CoreProcess.clijGPU = new CLIJ2(new CLIJ(MSSR.gpuI));
        CoreProcess.clijGPU.clear();
        long MemoryGPU = CoreProcess.clijGPU.getCLIJ().getGPUMemoryInBytes()/10;
        System.out.println("MemoryGPU: " + MemoryGPU);
        CoreProcess.clijGPU.clear();
        CoreProcess.clijGPU.close();
        CoreProcess.clijGPU = null;
        
        int[] dims = MSSR.imgBase.getDimensions();
        long dimImgAmp = dims[0]*dims[1]*dims[2]*dims[3]*dims[4]*MSSR.ampVal*MSSR.ampVal;
        int hst = (int) Math.round(0.4247*MSSR.psfVal*MSSR.ampVal) * 2;
        long dimImgpadd = (dims[0]+hst)*(dims[1]+hst)*dims[2]*dims[3]*dims[4]*MSSR.ampVal*MSSR.ampVal/10;
        long validation = (long) Math.ceil((dimImgAmp+dimImgpadd)*3.8);
        return validation<MemoryGPU && validation>0;
    }
}

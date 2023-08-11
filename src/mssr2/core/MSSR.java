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

//import ij.IJ;
import mssr2.gui.MGUI;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import mssr2.process.CoreProcess;

/**
 *
 * @author Raul
 */
public class MSSR implements PlugIn{
    public static boolean instance = false;
    public static String[] ImgsNames = null; //Lista de nombres de imagenes abiertas
    public static String ampSt = "Integer value > 1"; //Valor de amplificacion
    public static int ampVal;
    public static String psfSt = "Integer value > 0"; //Valor de fwhm
    public static float psfVal;
    public static String newPsfSt = "Integer value > 0"; //
    public static int ordVal;
    public static String orderSt = "Integer value >= 0";
    public static boolean meshSt = true;
    public static boolean parallel = false;
    public static int numCPUs;
    public static ArrayList<String> GPUsNames = new ArrayList<>();
    public static int gpuI = 0;
    public static int typeTAMemory = 0;
    public static boolean typeATon = false;
    public static MGUI central = null;
    public static int interpType = 1;
    public static boolean intensity_normalization = true;

    public static ImagePlus imgBase;
    public static String imgTitle;
    public static int imgArrPos = 0;

    public static boolean excludeOutL = false;
    public static float thresholdOutL = (float) 99.7;

    public static boolean dirs = false;
    public static String pathBatches = ".";
    public static String pathSaveBatches = "";
    public static String[] filesList = null;

    public static boolean forceImg = false;
    public static boolean isCLIJon = false;
    @Override
    public void run(String string) {
//        System.out.println("MSSR.instance: "+MSSR.instance);
        try {
            Class.forName( "net.haesleinhuepf.clij.CLIJ" );
            Class.forName( "net.haesleinhuepf.clij2.CLIJ2" );
            isCLIJon = true;
//            System.out.println("Yes");
        } catch( ClassNotFoundException e ) {
//            JOptionPane.showMessageDialog(null, "Please activate the CLIJ and CLIJ2 update site");
//            return;
            isCLIJon = false;
        }
//        if(!MSSR.instance){
//            MSSR.instance = true;
//        } else{
//            JOptionPane.showMessageDialog(null, "Wait for the previous process to finish.");
//            return;
//        }
        if(MSSR.central != null){
            JOptionPane.showMessageDialog(null, "Wait for the previous process to finish.");
            return;
        }
        if(CoreProcess.clijGPU != null){
            CoreProcess.clijGPU.clear();
            CoreProcess.clijGPU.close();
            CoreProcess.clijGPU = null;
        }
        MSSR.imgBase = null;
        MSSR.numCPUs = Runtime.getRuntime().availableProcessors() - 2;
        if(MSSR.numCPUs < 1){
            MSSR.numCPUs = 1;
        }
//        System.out.println("Iniciando");

        MSSR.central = new MGUI();
        MSSR.central.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
//                System.out.println("Test Close central");
                MSSR.central.dispose();
                MSSR.resetData();
            }
        });
        MSSR.central.setVisible(true);
    }
    public static boolean iniGPU(){
        return true;
    }
    public static void stopGPU(){
        if(CoreProcess.clijGPU != null){
//            System.out.println(CoreProcess.clijGPU.reportMemory());
            CoreProcess.clijGPU.clear();
            CoreProcess.clijGPU.close();
            CoreProcess.clijGPU = null;
        }
    }
    public static void resetData(){
        MSSR.instance = false;
        MSSR.central = null;
        MSSR.stopGPU();
        MSSR.dirs = false;
        MSSR.forceImg = false;
        MSSR.filesList = null;
    }
    public static String getNameT(String TA){
        String name = MSSR.imgTitle+"_MSSRa"+MSSR.ampSt+"f"+MSSR.psfSt+"o"+MSSR.orderSt;
        if(MSSR.interpType == 1){
            name = name +"_"+ "bicubic";
        } else {
            name = name +"_"+ "fourier";
        }
        if(TA==""){
            return name+".tif";
        }
        return name+"_"+TA+".tif";
    }
}

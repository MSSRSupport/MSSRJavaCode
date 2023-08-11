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
package mssr2.gui;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import mssr2.core.MSSR;
import ij.gui.GUI;
import mssr2.info.NGPUs;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.*;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import mssr2.process.Core;

/**
 *
 * @author raul_
 */
public class MGUI extends JFrame implements ActionListener, FocusListener, ChangeListener{
    private final JPanel pnl;
    private HintTextField amoTxtField;
    private JButton comptFWHMButton;
    private HintTextField fwhmTxtField;
    private HintTextField orderTxtField;
    
    private ButtonGroup groupInterp;
    private JRadioButton interBicRadButton;
    private JRadioButton interFouRadButton;
    
    private JCheckBox meshingChkBx;
    
    private JCheckBox parallelChkBx;
    private JComboBox<String> GPUListCombBox;
    
    private JCheckBox TempAnCheckB;
    private ButtonGroup groupTA;
    private JRadioButton tpmRadButton;
    private JRadioButton varRadButton;
    private JRadioButton meanRadButton;
    private JRadioButton coeffVarRadButton;
    private JRadioButton SOFIRadButton;
    private JSlider SOFIype;

    private JCheckBox excludeOLChkBx;
    private JTextField excludeOLTxtField;
    private JLabel excludeOLpercentLabel;
    
    private JCheckBox inteNormChkBx;
    
    private JButton selectImageButton;
    private JButton dirBatchButton;
    private TextArea infoImageBatchTxtArea;
    private JLabel selectImageBatchLabel;
    private JButton cleanButton;
    
    private JButton cancel;
    private JButton ok;
    
    public static int extraParallel = 0;
    public static int extraTA = 0;
    public static int extrainfo = 0;

    public MGUI() {
        super("MSSR");
        super.setSize(250, 480+extraParallel+extraTA);
        super.setResizable(false);
        super.setAlwaysOnTop(true);
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.pnl = new JPanel();
        pnl.setLayout(null);
        MSSR.forceImg = false;
        MSSR.dirs = false;
        //####################################################
        this.addComponentsParameters();
        this.addComponentsAdvanceConfig();
        this.addComponentsFinalButtons();
        //####################################################
        this.add(pnl);
        GUI.center(this);
//        this.setVisible(true);
        pnl.requestFocusInWindow();
    }
    private void addComponentsParameters(){
//#################################################### AMP
        JLabel ampl = new JLabel("AMP", JLabel.CENTER);
        pnl.add(ampl);
        ampl.setBounds(10, 5, 100, 25);//(10, 5, 100, 25)
        this.amoTxtField = new HintTextField("Integer value >= 1");
        if(!"Integer value > 1".equals(MSSR.ampSt)){
            this.amoTxtField.setText(MSSR.ampSt);
            this.amoTxtField.setForeground(Color.black);
        }
        pnl.add(amoTxtField);
        amoTxtField.setBounds(120, 5, 105, 25);//(10, 5, 100, 25)

//#################################################### ComputePSF
        this.comptFWHMButton = new JButton("Compute FWHM of PSF");
//        this.slImg = new JButton("Batch Analysis");
        this.comptFWHMButton.addActionListener(this);
        pnl.add(this.comptFWHMButton);
        this.comptFWHMButton.setBounds(115-60, 35, 110+60, 25);

//#################################################### PSF
        JLabel psfl = new JLabel("FWHM of PSF", JLabel.CENTER);
        pnl.add(psfl);
        psfl.setBounds(10, 65, 100, 25);
        this.fwhmTxtField = new HintTextField("Float value > 0");
        if(!"Integer value > 0".equals(MSSR.psfSt)){
            this.fwhmTxtField.setText(MSSR.psfSt);
            this.fwhmTxtField.setForeground(Color.black);
        }
        pnl.add(fwhmTxtField);
        fwhmTxtField.setBounds(120, 65, 105, 25);
//#################################################### Orden
        JLabel orderl = new JLabel("Order", JLabel.CENTER);
        pnl.add(orderl);
        orderl.setBounds(10, 95, 100, 25);
        this.orderTxtField = new HintTextField("Integer value >= 0");
        if(!"Integer value >= 0".equals(MSSR.orderSt)){
            this.orderTxtField.setText(MSSR.orderSt);
            this.orderTxtField.setForeground(Color.black);
        }
        pnl.add(orderTxtField);
        orderTxtField.setBounds(120, 95, 105, 25);
    }
    private void addComponentsAdvanceConfig(){
//#################################################### Interpolation
        JLabel interpType = new JLabel("Interpolation Type", JLabel.CENTER);
        pnl.add(interpType);
        interpType.setBounds(0, 130, 150, 25);
        if(MSSR.interpType == 1){
            this.interBicRadButton = new JRadioButton("Bicubic", true);
            this.interFouRadButton = new JRadioButton("Fourier", false);
        } else{
            this.interBicRadButton = new JRadioButton("Bicubic", false);
            this.interFouRadButton = new JRadioButton("Fourier", true);
        }
        this.interBicRadButton.setVisible(true);
        this.interFouRadButton.setVisible(true);
        
        this.interBicRadButton.addActionListener(this);
        this.interFouRadButton.addActionListener(this);
        
        this.groupInterp = new ButtonGroup();
        this.groupInterp.add(this.interBicRadButton);
        this.groupInterp.add(this.interFouRadButton);
        
        pnl.add(this.interBicRadButton);
        pnl.add(this.interFouRadButton);
        this.interBicRadButton.setBounds(10, 150, 100, 25);
        this.interFouRadButton.setBounds(110, 150, 100, 25);

//#################################################### Meshing
        this.meshingChkBx = new JCheckBox("Minimize Meshing", MSSR.meshSt);
        this.meshingChkBx.addActionListener(this);
        pnl.add(this.meshingChkBx);
        this.meshingChkBx.setBounds(0, 185, 230, 25);

//#################################################### GPUs
        this.parallelChkBx = new JCheckBox("GPU Computing", MSSR.parallel);
        if(!MSSR.isCLIJon){
            this.parallelChkBx.setToolTipText("Install CLIJ and CLIJ2");
        }
        this.parallelChkBx.setEnabled(true);
        this.parallelChkBx.addActionListener(this);
        pnl.add(this.parallelChkBx);
        this.parallelChkBx.setBounds(0, 215, 230, 25);
        this.parallelChkBx.setEnabled(MSSR.isCLIJon);
        
        if(MSSR.parallel){
//            extraParallel = 30;
            String[] gpusN = new String[MSSR.GPUsNames.size()];
            gpusN = MSSR.GPUsNames.toArray(gpusN); 
            this.GPUListCombBox = new JComboBox<>(gpusN);
            this.GPUListCombBox.setSelectedIndex(MSSR.gpuI);
        } else{
            this.GPUListCombBox = new JComboBox<>();
        }
        this.GPUListCombBox.setVisible(MSSR.parallel);
//        this.cB.setEnabled(MSSR.parallel);
        this.GPUListCombBox.addActionListener(this);
        pnl.add(this.GPUListCombBox);
        this.GPUListCombBox.setBounds(0, 245, 230, 25);

//#################################################### Temporal A
        if(MSSR.typeATon){
//            extraTA = 120;
            switch (MSSR.typeTAMemory) {
                case 1:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", true);
                    this.varRadButton = new JRadioButton("Var", false);
                    this.meanRadButton = new JRadioButton("Mean", false);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
                    this.SOFIRadButton = new JRadioButton("SOFI", false);
                    this.SOFIype = new JSlider(2,4,2);
                    this.SOFIype.setEnabled(false);
                    break;
                case 2:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", false);
                    this.varRadButton = new JRadioButton("Var", true);
                    this.meanRadButton = new JRadioButton("Mean", false);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
                    this.SOFIRadButton = new JRadioButton("SOFI", false);
                    this.SOFIype = new JSlider(2,4,2);
                    this.SOFIype.setEnabled(false);
                    break;
                case 3:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", false);
                    this.varRadButton = new JRadioButton("Var", false);
                    this.meanRadButton = new JRadioButton("Mean", true);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
                    this.SOFIRadButton = new JRadioButton("SOFI", false);
                    this.SOFIype = new JSlider(2,4,2);
                    this.SOFIype.setEnabled(false);
                    break;
                case 4:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", false);
                    this.varRadButton = new JRadioButton("Var", false);
                    this.meanRadButton = new JRadioButton("Mean", false);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", true);
                    this.SOFIRadButton = new JRadioButton("SOFI", false);
                    this.SOFIype = new JSlider(2,4,2);
                    this.SOFIype.setEnabled(false);
                    break;
                case 5:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", false);
                    this.varRadButton = new JRadioButton("Var", false);
                    this.meanRadButton = new JRadioButton("Mean", false);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
                    this.SOFIRadButton = new JRadioButton("SOFI", true);
                    this.SOFIype = new JSlider(2,4,2);
                    this.SOFIype.setEnabled(true);
                    break;
                case 6:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", false);
                    this.varRadButton = new JRadioButton("Var", false);
                    this.meanRadButton = new JRadioButton("Mean", false);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
                    this.SOFIRadButton = new JRadioButton("SOFI", true);
                    this.SOFIype = new JSlider(2,4,3);
                    this.SOFIype.setEnabled(true);
                    break;
                default:
                    this.TempAnCheckB = new JCheckBox("Temporal Analysis", true);
                    this.tpmRadButton = new JRadioButton("TPM", false);
                    this.varRadButton = new JRadioButton("Var", false);
                    this.meanRadButton = new JRadioButton("Mean", false);
                    this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
                    this.SOFIRadButton = new JRadioButton("SOFI", true);
                    this.SOFIype = new JSlider(2,4,4);
                    this.SOFIype.setEnabled(true);
                    break;
            }
            this.tpmRadButton.setVisible(true);
            this.varRadButton.setVisible(true);
            this.meanRadButton.setVisible(true);
            this.coeffVarRadButton.setVisible(true);
            this.SOFIRadButton.setVisible(true);
            this.SOFIype.setVisible(true);
        } else{
            this.TempAnCheckB = new JCheckBox("Temporal Analysis", false);
            this.tpmRadButton = new JRadioButton("TPM", true);
            this.varRadButton = new JRadioButton("Var", false);
            this.meanRadButton = new JRadioButton("Mean", false);
            this.coeffVarRadButton = new JRadioButton("Coefficient Variation", false);
            this.SOFIRadButton = new JRadioButton("SOFI", false);
            this.SOFIype = new JSlider(2,4,2);
            this.SOFIype.setEnabled(false);

            this.tpmRadButton.setVisible(false);
            this.varRadButton.setVisible(false);
            this.meanRadButton.setVisible(false);
            this.coeffVarRadButton.setVisible(false);
            this.SOFIRadButton.setVisible(false);
            this.SOFIype.setVisible(false);
        }
        this.SOFIype.setMajorTickSpacing(1);
        this.SOFIype.setPaintLabels(true);

        this.groupTA = new ButtonGroup();

        this.TempAnCheckB.addActionListener(this);
        this.tpmRadButton.addActionListener(this);
        this.varRadButton.addActionListener(this);
        this.meanRadButton.addActionListener(this);
        this.coeffVarRadButton.addActionListener(this);
        this.SOFIRadButton.addActionListener(this);
        this.SOFIype.addChangeListener(this);

        this.groupTA.add(this.tpmRadButton);
        this.groupTA.add(this.varRadButton);
        this.groupTA.add(this.meanRadButton);
        this.groupTA.add(this.coeffVarRadButton);
        this.groupTA.add(this.SOFIRadButton);
        
        pnl.add(this.TempAnCheckB);
        pnl.add(this.tpmRadButton);//Radio buttons
        pnl.add(this.varRadButton);
        pnl.add(this.meanRadButton);
        pnl.add(this.coeffVarRadButton);
        pnl.add(this.SOFIRadButton);
        pnl.add(this.SOFIype);

        this.TempAnCheckB.setBounds(0, 245+extraParallel, 230, 25);
        this.tpmRadButton.setBounds(10, 275+extraParallel, 100, 25);
        this.varRadButton.setBounds(120, 275+extraParallel, 100, 25);
        this.meanRadButton.setBounds(10, 305+extraParallel, 100, 25);
        this.SOFIRadButton.setBounds(120, 305+extraParallel, 100, 25);
        this.SOFIype.setBounds(20, 335+extraParallel, 150, 25);
        this.coeffVarRadButton.setBounds(10, 365+extraParallel, 200, 25);

//#################################################### Exclude OutLiners
        this.excludeOLChkBx = new JCheckBox("Ignore Outliers", MSSR.excludeOutL);
        this.excludeOLTxtField = new JTextField("" + (100 - MSSR.thresholdOutL));
        this.excludeOLTxtField.setEnabled(MSSR.excludeOutL);
        this.excludeOLpercentLabel = new JLabel("%", JLabel.CENTER);
        this.excludeOLpercentLabel.setEnabled(MSSR.excludeOutL);
        
        this.excludeOLChkBx.addActionListener(this);
        this.excludeOLTxtField.addFocusListener(this);
        
        pnl.add(this.excludeOLChkBx);
        pnl.add(this.excludeOLTxtField);
        pnl.add(this.excludeOLpercentLabel);
        this.excludeOLChkBx.setVisible(false);
        this.excludeOLTxtField.setVisible(false);
        this.excludeOLpercentLabel.setVisible(false);
        
        this.excludeOLChkBx.setBounds(0, 280+extraParallel+extraTA, 110, 25);
        this.excludeOLTxtField.setBounds(115, 280+extraParallel+extraTA, 35, 25);
        this.excludeOLpercentLabel.setBounds(150, 280+extraParallel+extraTA, 15, 25);

//#################################################### Intensity Normalization
        this.inteNormChkBx = new JCheckBox("Intensity Normalization", MSSR.intensity_normalization);
        this.inteNormChkBx.addActionListener(this);
        pnl.add(this.inteNormChkBx);
        this.inteNormChkBx.setBounds(0, 310+extraParallel+extraTA, 200, 25);
    }
    private void addComponentsFinalButtons(){
//#################################################### Force Image

        this.selectImageButton = new JButton("Select Image");
//        this.slImg = new JButton("Batch Analysis");
        this.selectImageButton.addActionListener(this);
        pnl.add(this.selectImageButton);
        this.selectImageButton.setBounds(10, 275+70+extraParallel+extraTA, 210, 25);

//#################################################### Directory

        this.dirBatchButton = new JButton("Batch Analysis");
        this.dirBatchButton.addActionListener(this);
        pnl.add(this.dirBatchButton);
        this.dirBatchButton.setBounds(10, 305+70+extraParallel+extraTA, 210, 25);
        
//#################################################### Info

        this.selectImageBatchLabel = new JLabel("");
        this.selectImageBatchLabel.setVisible(false);
        pnl.add(this.selectImageBatchLabel);
        this.selectImageBatchLabel.setBounds(70, 335+70+extraParallel+extraTA, 140, 25);

        this.infoImageBatchTxtArea = new TextArea("");
        this.infoImageBatchTxtArea.setEditable(false);
        this.infoImageBatchTxtArea.setVisible(false);
        pnl.add(this.infoImageBatchTxtArea);
        this.infoImageBatchTxtArea.setBounds(0, 365+70+extraParallel+extraTA, 235, 40);

        this.cleanButton = new JButton("");
        this.cleanButton.setVisible(false);
        this.cleanButton.addActionListener(this);
        pnl.add(this.cleanButton);
        this.cleanButton.setBounds(10, 410+70+extraParallel+extraTA, 210, 25);

//#################################################### Cancel \ Ok

        this.cancel = new JButton("Cancel");
        this.cancel.addActionListener(this);
        pnl.add(this.cancel);//Cance Ok buttons
        this.cancel.setBounds(10, 335+70+extraParallel+extraTA, 100, 25);

        this.ok = new JButton("Ok");
        this.ok.addActionListener(this);
        pnl.add(this.ok);
        this.ok.setBounds(120, 335+70+extraParallel+extraTA, 100, 25);
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if(this.cancel == e.getSource()){
            System.out.println("Bye!");
            this.dispose();
            MSSR.resetData();
        } else if(this.tpmRadButton == e.getSource()){
            MSSR.typeTAMemory = 1;
            this.SOFIype.setEnabled(false);
        } else if(this.varRadButton == e.getSource()){
            MSSR.typeTAMemory = 2;
            this.SOFIype.setEnabled(false);
        } else if(this.meanRadButton == e.getSource()){
            MSSR.typeTAMemory = 3;
            this.SOFIype.setEnabled(false);
        } else if(this.coeffVarRadButton == e.getSource()){
            MSSR.typeTAMemory = 4;
            this.SOFIype.setEnabled(false);
        } else if(this.SOFIRadButton == e.getSource()){
            MSSR.typeTAMemory = 3+this.SOFIype.getValue();
            this.SOFIype.setEnabled(true);
        } else if(this.interBicRadButton == e.getSource()){
            MSSR.interpType = 1;
        } else if(this.interFouRadButton == e.getSource()){
            MSSR.interpType = 2;
        } else if(this.excludeOLChkBx == e.getSource()){
            MSSR.excludeOutL = this.excludeOLChkBx.isSelected();
            this.excludeOLTxtField.setEnabled(MSSR.excludeOutL);
            this.excludeOLpercentLabel.setEnabled(MSSR.excludeOutL);
        } else if(this.inteNormChkBx == e.getSource()){
            MSSR.intensity_normalization = this.inteNormChkBx.isSelected();
        } else if(this.TempAnCheckB == e.getSource()){
            MSSR.typeATon = this.TempAnCheckB.isSelected();
            this.tpmRadButton.setVisible(MSSR.typeATon);
            this.varRadButton.setVisible(MSSR.typeATon);
            this.meanRadButton.setVisible(MSSR.typeATon);
            this.coeffVarRadButton.setVisible(MSSR.typeATon);
            this.SOFIRadButton.setVisible(MSSR.typeATon);
            this.SOFIype.setVisible(MSSR.typeATon);
            if(!MSSR.typeATon){
                extraTA = 0;
//                MSSR.typeTAMemory = 0;
//                this.groupTA.clearSelection();
                this.SOFIype.setEnabled(false);
            } else{
                extraTA = 120;
                if(MSSR.typeTAMemory == 2){
                    this.varRadButton.setSelected(MSSR.typeATon);
                } else if(MSSR.typeTAMemory == 3){
                    this.meanRadButton.setSelected(MSSR.typeATon);
                } else if(MSSR.typeTAMemory == 4){
                    this.coeffVarRadButton.setSelected(MSSR.typeATon);
                } else if(MSSR.typeTAMemory >= 5){
                    this.SOFIRadButton.setSelected(MSSR.typeATon);
                    this.SOFIype.setEnabled(true);
                    this.SOFIype.setValue(MSSR.typeTAMemory-3);
                } else{
                    MSSR.typeTAMemory = 1;
                    this.tpmRadButton.setSelected(MSSR.typeATon);
                }
            }
            this.modButtons();
        } else if(this.parallelChkBx == e.getSource()){
            MSSR.parallel = this.parallelChkBx.isSelected();
            if(MSSR.GPUsNames.isEmpty()){
                NGPUs nGpus = new NGPUs();
                nGpus.start();
                try {
                    nGpus.join();
//                    System.out.println("In development join");
                } catch (InterruptedException ex) {
                    Logger.getLogger(MSSR.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
//            System.out.println("Termine hilo");
//            System.out.println("MSSR.GPUsNames size " + MSSR.GPUsNames.size());
            
            if("No GPUs".equals(MSSR.GPUsNames.get(0))){
//            if("".equals(MSSR.GPUsNames.get(0))){
               MSSR.parallel = false;
               this.parallelChkBx.setSelected(false);
               this.parallelChkBx.setEnabled(false);
               return;
            }
            if(this.GPUListCombBox.getItemCount() == 0){
                System.out.println("GPUS:");
                for(int i = 0; i < MSSR.GPUsNames.size(); i++){
                    System.out.println("< " + MSSR.GPUsNames.get(i) + " >");
                    this.GPUListCombBox.addItem(MSSR.GPUsNames.get(i));
                }
                this.GPUListCombBox.setSelectedIndex(MSSR.gpuI);
            }
//            System.out.println("Si llegue");
            if(MSSR.parallel || this.parallelChkBx.isSelected()){
                extraParallel = 30;
            } else{
                extraParallel = 0;
            }
//            System.out.println("extraParallel: " + extraParallel);
            this.modButtons();
            this.GPUListCombBox.setVisible(this.parallelChkBx.isSelected());
        } else if(this.GPUListCombBox == e.getSource()){
            MSSR.gpuI = this.GPUListCombBox.getSelectedIndex();
        } else if(this.ok == e.getSource()){
            System.out.println("No me presiones!");
            try {
                MSSR.ampVal = Integer.parseInt(this.amoTxtField.getText());
                MSSR.psfVal = Float.parseFloat(this.fwhmTxtField.getText());
                MSSR.ordVal = Integer.parseInt(this.orderTxtField.getText());
                MSSR.ampSt = this.amoTxtField.getText();
                MSSR.psfSt = this.fwhmTxtField.getText();
                MSSR.orderSt = this.orderTxtField.getText();
                MSSR.meshSt = this.meshingChkBx.isSelected();
                if( MSSR.ampVal < 1){
                    JOptionPane.showMessageDialog(this, "AMP must be greater than or equal to 1");
                    return;
                }
                if( MSSR.psfVal <= 0){
                    JOptionPane.showMessageDialog(this, "PSF must be greater than 0");
                    return;
                }
                if( MSSR.ordVal < 0){
                    JOptionPane.showMessageDialog(this, "Order must be greater than or equal to 0");
                    return;
                }
                if((int) Math.round(0.5*MSSR.psfVal*MSSR.ampVal) < 1){
                    JOptionPane.showMessageDialog(this, "Check your parameters, it is necessary that AMP * FWHM > 1");
                    return;
                }
                this.launch();
            } catch (NumberFormatException ex) {
                System.out.println(ex);
                JOptionPane.showMessageDialog(this, "Only numbers are supported.");
            }
        } else if(this.selectImageButton == e.getSource()){
            MSSR.central.setEnabled(false);
            SelectUImage sUI = new SelectUImage();
            sUI.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
//                    System.out.println("Test Close");
                    MSSR.central.setEnabled(true);
                    sUI.dispose();
                }
            });
            sUI.setVisible(true);
        } else if(this.cleanButton == e.getSource()){
            MSSR.forceImg = false;
            MSSR.dirs = false;
            extrainfo = 0;
            MSSR.pathBatches = "";
            this.infoImageBatchTxtArea.setText("");
            this.TempAnCheckB.setEnabled(true);
            this.modButtons();
        } else if(this.dirBatchButton == e.getSource()){
            JFileChooser fileChooser = new JFileChooser(new java.io.File( MSSR.pathBatches ));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showOpenDialog(this);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                infoImageBatchTxtArea.setText(file.getPath());
                MSSR.dirs = true;
                MSSR.pathBatches = file.getAbsolutePath();
                MSSR.pathSaveBatches = MSSR.pathBatches +"\\MSSR";
                extrainfo = 105;
                this.modButtons();
            } else{
                extrainfo = 0;
            }
        } else if(this.comptFWHMButton == e.getSource()){
            MSSR.central.setEnabled(false);
            ComputePSF cpPSF = new ComputePSF();
            cpPSF.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
//                    System.out.println("Test Close");
                    MSSR.central.setEnabled(true);
                    cpPSF.dispose();
                }
            });
        }
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        MSSR.typeTAMemory = 3+this.SOFIype.getValue();
        System.out.println(MSSR.typeTAMemory);
//        MSSR.thresholdOutL = 100 - this.excPercent.getValue();
//        this.percentOLLabel.setText(""+ this.excPercent.getValue() + "%");
    }
    public void launch(){
        if(!MSSR.dirs){
            if(MSSR.imgBase == null){
                try{
                    MSSR.imgBase = WindowManager.getCurrentImage();
//                    MSSR.imgBase = WindowManager.getImage(MSSR.imgTitle);
                } catch (HeadlessException e) {
                    MSSR.imgBase = null;
                    JOptionPane.showMessageDialog(this, "Something went wrong. No image to analyze?\nUse the \"Select Image\" Button\t to force the image.");
                    return;
                }
            }
            if(MSSR.imgBase == null){
                JOptionPane.showMessageDialog(this, "Something went wrong. No image to analyze?\nUse the \"Select Image\" Button\t to force the image.");
                return;
            }
            MSSR.imgTitle = MSSR.imgBase.getShortTitle();
            if (MSSR.imgBase.isHyperStack()||MSSR.imgBase.isComposite()||MSSR.imgBase.getStackSize()>1) {
                MSSR.imgBase.setDimensions(1,MSSR.imgBase.getStackSize(),1);
            }
            System.out.println("--- Values ---");
            System.out.println("-- Image to analize: "+MSSR.imgTitle+" --");
        } else{
            MSSR.filesList = new File(MSSR.pathBatches).list((File file, String s) ->
                    s.toLowerCase().endsWith(".tif") || s.toLowerCase().endsWith(".tiff"));
            if(MSSR.filesList.length == 0){
                JOptionPane.showMessageDialog(this, "Your directory has no \".tif\" or \".tiff\" files");
                return;
            }
        }
        if(MSSR.parallel){
            MSSR.gpuI = this.GPUListCombBox.getSelectedIndex();
        }
        Core cP = new Core(this);
        JOptionPane.showMessageDialog(this, "Please wait, this process will take a few minutes.\nClose this window to continue.");
        this.dispose();
        System.out.println("Amplification: " + MSSR.ampSt);
        if(MSSR.interpType == 1){// Bicubic
            System.out.println("Interpolation: Bicubic");
        } else{
            System.out.println("Interpolation: Fourier");
        }
        System.out.println("FWHM: " + MSSR.psfSt);
        System.out.println("Order: " + MSSR.orderSt);
        System.out.println("Meshing: " + MSSR.meshSt);
        if(MSSR.parallel){
            System.out.println("GPU: " + MSSR.GPUsNames.get(MSSR.gpuI));
        }
        if(MSSR.excludeOutL){
            System.out.println("Ignore Outliers: " + ((float) MSSR.thresholdOutL) + "%");
        }
        System.out.println("Intensity Normalization: " + MSSR.intensity_normalization);
        if(MSSR.typeATon){
            if(MSSR.typeTAMemory==1){
                System.out.println("Temporal Analisis: TPM");
            } else if(MSSR.typeTAMemory==2){
                System.out.println("Temporal Analisis: Variance");
            } else if(MSSR.typeTAMemory==3){
                System.out.println("Temporal Analisis: Mean");
            } else if(MSSR.typeTAMemory==4){
                System.out.println("Temporal Analisis: Coefficient Variation");
            } else if(MSSR.typeTAMemory==5){
                System.out.println("Temporal Analisis: SOFI 2");
            } else if(MSSR.typeTAMemory==6){
                System.out.println("Temporal Analisis: SOFI 3");
            } else if(MSSR.typeTAMemory==7){
                System.out.println("Temporal Analisis: SOFI 4");
            }
        } else{
            MSSR.typeTAMemory = 0;
            this.groupTA.clearSelection();
            System.out.println("No Temporal Analisis");
        }
        System.out.println("--------------");
        cP.start();
    }
    public void modButtons(){
        super.setSize(250, 480+extraParallel+extraTA+extrainfo);
        this.TempAnCheckB.setBounds(0, 245+extraParallel, 230, 25);
        this.tpmRadButton.setBounds(10, 275+extraParallel, 100, 25);
        this.varRadButton.setBounds(120, 275+extraParallel, 100, 25);
        this.meanRadButton.setBounds(10, 305+extraParallel, 100, 25);
        this.SOFIRadButton.setBounds(120, 305+extraParallel, 100, 25);
        this.SOFIype.setBounds(20, 335+extraParallel, 150, 25);
        this.coeffVarRadButton.setBounds(10, 365+extraParallel, 200, 25);
        ///
        this.excludeOLChkBx.setBounds(0, 280+extraParallel+extraTA, 110, 25);
        this.excludeOLTxtField.setBounds(115, 280+extraParallel+extraTA, 35, 25);
        this.excludeOLpercentLabel.setBounds(150, 280+extraParallel+extraTA, 15, 25);
        ///
        this.inteNormChkBx.setBounds(0, 310+extraParallel+extraTA, 200, 25);
        ///
        this.selectImageButton.setBounds(10, 275+70+extraParallel+extraTA, 210, 25);
        this.dirBatchButton.setBounds(10, 305+70+extraParallel+extraTA, 210, 25);
        this.selectImageBatchLabel.setBounds(70, 335+70+extraParallel+extraTA, 140, 25);
        this.infoImageBatchTxtArea.setBounds(0, 365+70+extraParallel+extraTA, 235, 40);
        this.cleanButton.setBounds(10, 410+70+extraParallel+extraTA, 210, 25);
        this.cancel.setBounds(10, 335+70+extraParallel+extraTA+extrainfo, 100, 25);
        this.ok.setBounds(120, 335+70+extraParallel+extraTA+extrainfo, 100, 25);
        if(MSSR.forceImg){
            this.dirBatchButton.setEnabled(false);
            this.selectImageBatchLabel.setText("Image Selected:");
            this.selectImageBatchLabel.setVisible(true);
            this.infoImageBatchTxtArea.setText(MSSR.imgTitle);
            this.infoImageBatchTxtArea.setVisible(true);
            this.cleanButton.setText("Clean Selection");
            this.cleanButton.setVisible(true);
        } else if(MSSR.dirs){
            this.selectImageButton.setEnabled(false);
            this.selectImageBatchLabel.setText("Path Selected:");
            this.selectImageBatchLabel.setVisible(true);
            this.infoImageBatchTxtArea.setVisible(true);
            this.cleanButton.setText("Clean Selection");
            this.cleanButton.setVisible(true);
        } else{
            this.selectImageButton.setEnabled(true);
            this.dirBatchButton.setEnabled(true);
            this.selectImageBatchLabel.setVisible(false);
            this.infoImageBatchTxtArea.setVisible(false);
            this.cleanButton.setVisible(false);
        }
    }
    public void editPSFValue(String str){
            this.fwhmTxtField.setText(str);
            this.fwhmTxtField.setForeground(Color.black);
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        MSSR.thresholdOutL = (float) 100 - Float.parseFloat(this.excludeOLTxtField.getText());
    }
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println(Runtime.getRuntime().freeMemory());
        new ImageJ();
        new MSSR().run("");
        System.out.println(Runtime.getRuntime().freeMemory());
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/Nanorulers Stack 100 images Datos para Raul.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/NanoReglas/AZ5A48KB_Atto655-1_50x50.tif");
        System.out.println(Runtime.getRuntime().freeMemory());
        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_PSFCHECK_561_DONUTS_33MS_5POWERL.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/pos_0/DeInteres/experiment-181_sample-1-1_posXY0_channels_t0_posZ0.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/raw7_100_DAMIAN_PSFCHECK_561_DONUTS.tif");
//        ImagePlus imgTest = IJ.openImage("D:/MSSR_Develop/Debug/DanielMartinez_BacP24EGFP_150ms_STORM-crop.tif");
        imgTest.show();
        System.out.println(Runtime.getRuntime().freeMemory());
        System.out.println(Runtime.getRuntime().freeMemory());
        
//        int amp = 20;
//        ImageProcessor imgProc = fftMSSR.FourierInterp(imgTest.getProcessor(), amp);
//        ImagePlus imgFourier = new ImagePlus("Fourier.tif", imgProc);
//        ImagePlus imgBicubic = Scaler.resize(new ImagePlus("Bicubic", imgTest.getProcessor()), imgTest.getWidth()*amp, imgTest.getHeight()*amp, 1, "bicubic");
//        imgBicubic.setTitle("Bicubic.tif");
//        imgFourier.show();
//        imgBicubic.show();
    }
}

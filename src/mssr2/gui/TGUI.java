/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mssr2.gui;

import ij.ImagePlus;
import ij.gui.GUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
//import java.awt.event.WindowListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import static mssr2.process.CoreProcess.TPM;
import static mssr2.process.CoreProcess.TRACK;
import static mssr2.process.CoreProcess.CoeffVar;
import ij.WindowManager;
import ij.plugin.ZProjector;
import ij.process.ImageConverter;
import java.awt.HeadlessException;
import javax.swing.JOptionPane;
import static mssr2.process.CoreProcess.Call;

/**
 *
 * @author raul_
 */
public class TGUI extends JFrame implements ActionListener {
    private final ButtonGroup grTA;
    private final JRadioButton Mean;
    private final JRadioButton Var;
    private final JRadioButton TPM;
    private final JRadioButton CV;
    private final JRadioButton SOFI;

    private final JSlider SOFIype;

    private final JButton ok;
//    private final 

    public TGUI(){
        super("Temporal Analysis");
        super.setSize(300, 300);
        super.setResizable(false);
        super.setAlwaysOnTop(true);
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Test Close central");
                dispose();
            }
        });
        JPanel pnl = new JPanel();
        pnl.setLayout(null);
//############################### Select Temporal Analysis
        JLabel Title = new JLabel("Select a temporal analysis");
        pnl.add(Title);
        Title.setBounds(10, 5, super.getWidth(), 25);

        this.grTA = new ButtonGroup();
        
        this.Mean = new JRadioButton("Mean Projection", true);
        this.Mean.addActionListener(this);
        this.grTA.add(this.Mean);
        pnl.add(this.Mean);
        this.Mean.setBounds(10, 30, super.getWidth(), 25);

        this.Var = new JRadioButton("Variance Projection", false);
        this.Var.addActionListener(this);
        this.grTA.add(this.Var);
        pnl.add(this.Var);
        this.Var.setBounds(10, 55, super.getWidth(), 25);

        this.TPM = new JRadioButton("TPM", false);
        this.TPM.addActionListener(this);
        this.grTA.add(this.TPM);
        pnl.add(this.TPM);
        this.TPM.setBounds(10, 80, super.getWidth(), 25);

        this.CV = new JRadioButton("Coefficient Variation", false);
        this.CV.addActionListener(this);
        this.grTA.add(this.CV);
        pnl.add(this.CV);
        this.CV.setBounds(10, 105, super.getWidth(), 25);

        this.SOFI = new JRadioButton("SOFI", false);
        this.SOFI.addActionListener(this);
        this.grTA.add(this.SOFI);
        pnl.add(this.SOFI);
        this.SOFI.setBounds(10, 130, super.getWidth(), 25);

//############################### Select TRACK Type
        this.SOFIype = new JSlider(2,4,2);
        this.SOFIype.setMajorTickSpacing(1);
        this.SOFIype.setPaintLabels(true);
        this.SOFIype.setEnabled(false);
        pnl.add(this.SOFIype);
        this.SOFIype.setBounds(20, 165, 150, 25);

//############################### End
        this.ok = new JButton("OK");
        this.ok.addActionListener(this);
        pnl.add(this.ok);//Cance Ok buttons
        this.ok.setBounds(100, 220, 100, 25);

//############################### End
        this.add(pnl);
        pnl.requestFocusInWindow();
        GUI.center(this);
        show();
        pnl.requestFocusInWindow();
    }

    public void launch(){
        try{
            ImagePlus imgBase = WindowManager.getCurrentImage();
            if(imgBase == null){
                JOptionPane.showMessageDialog(this, "Something went wrong. No image to analyze?");
                return;
            }
            this.dispose();
            String imgName = imgBase.getShortTitle();
            if(imgBase.getBitDepth() != 32){
                ImageConverter iC = new ImageConverter(imgBase);
                iC.convertToGray32();
            }
            if(this.Mean.isSelected()){
                System.out.println("Performing Temporal Analysis: Mean");
                ImagePlus meanImage = ZProjector.run(imgBase, "avg");
                meanImage.setTitle(this.getNameT(imgName, "Mean"));
                meanImage.resetDisplayRange();
                meanImage.show();
            } if(this.Var.isSelected()){
                System.out.println("Performing Temporal Analysis: Variance");
                ImagePlus varImage = ZProjector.run(imgBase, "sd");
                varImage = Call.run("Multiply create 32-bit", varImage, varImage);
                varImage.setTitle(this.getNameT(imgName, "Variance"));
                varImage.resetDisplayRange();
                varImage.show();
            } if(this.TPM.isSelected()){
                System.out.println("Performing Temporal Analysis: TPM");
                ImagePlus TPMImage = TPM(imgBase);
                TPMImage.setTitle(this.getNameT(imgName, "TPM"));
                TPMImage.resetDisplayRange();
                TPMImage.show();
            } if(this.CV.isSelected()){
                System.out.println("Performing Temporal Analysis: Coefficient Variation");
                ImagePlus CVImage = CoeffVar(imgBase);
                CVImage.setTitle(this.getNameT(imgName, "Coefficient Variation"));
                CVImage.resetDisplayRange();
                CVImage.show();
            } if(this.SOFI.isSelected()){
                System.out.println("Performing Temporal Analysis: SOFI");
                System.out.println(this.SOFIype.getValue());
                ImagePlus TRACKImage = TRACK(imgBase, this.SOFIype.getValue());
                TRACKImage.setTitle(this.getNameT(imgName, "SOFI" + this.SOFIype.getValue()));
                TRACKImage.resetDisplayRange();
                TRACKImage.show();
            }
        } catch (HeadlessException e) {
            JOptionPane.showMessageDialog(this, "Something went wrong. No image to analyze?");
            return;
        }
//        ImagePlus T = TPM(null);
    }
    private String getNameT(String imgName, String TA){
        return imgName+"_"+TA+".tif";
    }
    @Override
    public void actionPerformed(ActionEvent e) {
//        System.err.println(e.getActionCommand());
        this.SOFIype.setEnabled(this.SOFI.isSelected());
        if(this.Mean == e.getSource()){
            System.out.println("Temporal Analysis: Mean");
        } if(this.Var == e.getSource()){
            System.out.println("Temporal Analysis: Variance");
        } if(this.TPM == e.getSource()){
            System.out.println("Temporal Analysis: TPM");
        } if(this.CV == e.getSource()){
            System.out.println("Temporal Analysis: Coefficient Variation");
        } if(this.SOFI == e.getSource()){
            System.out.println("Temporal Analysis: SOFI");
        } if(this.ok == e.getSource()){
            System.out.println("Vale!");
            this.launch();
        }
    }
    
}

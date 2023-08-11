/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mssr2.gui;

import ij.gui.GUI;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mssr2.core.MSSR;

/**
 *
 * @author raul_
 */
public class ComputePSF extends JFrame implements ActionListener {
    private final HintTextField IndxRefracMedui;
    private final HintTextField OilRefracIndx;
    private final HintTextField NAobjetive;
    private final HintTextField waveLenghtEmission;
    private final HintTextField pxSize;
    private final JButton compute;

    public ComputePSF(){
        super("Compute FWHM in pixels");
        super.setSize(325, 220);
        super.setResizable(false);
        super.setAlwaysOnTop(true);
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel pnl = new JPanel();
        pnl.setLayout(null);

        JLabel IndxRefracMeduiJL = new JLabel("Refractive index of the medium", JLabel.CENTER);
        pnl.add(IndxRefracMeduiJL);
        IndxRefracMeduiJL.setBounds(5, 5, 195, 25);//(10, 5, 100, 25)
        this.IndxRefracMedui = new HintTextField("1.33");
        this.IndxRefracMedui.setForeground(Color.black);
        pnl.add(IndxRefracMedui);
        IndxRefracMedui.setBounds(200, 5, 100, 25);//(10, 5, 100, 25)

        JLabel OilRefracIndxJL = new JLabel("Refractive index of the oil", JLabel.CENTER);
        pnl.add(OilRefracIndxJL);
        OilRefracIndxJL.setBounds(5, 35, 195, 25);//(10, 5, 100, 25)
        this.OilRefracIndx = new HintTextField("1.515");
        this.OilRefracIndx.setForeground(Color.black);
        pnl.add(OilRefracIndx);
        OilRefracIndx.setBounds(200, 35, 100, 25);//(10, 5, 100, 25)

        JLabel NAobjetiveJL = new JLabel("Numeric Aperture of the objetive", JLabel.CENTER);
        pnl.add(NAobjetiveJL);
        NAobjetiveJL.setBounds(5, 65, 195, 25);//(10, 5, 100, 25)
        this.NAobjetive = new HintTextField("1.4");
        this.NAobjetive.setForeground(Color.black);
        pnl.add(NAobjetive);
        NAobjetive.setBounds(200, 65, 100, 25);//(10, 5, 100, 25)

        JLabel waveLenghtEmissionJL = new JLabel("Emission Wavelength (nm)", JLabel.CENTER);
        pnl.add(waveLenghtEmissionJL);
        waveLenghtEmissionJL.setBounds(5, 95, 195, 25);//(10, 5, 100, 25)
        this.waveLenghtEmission = new HintTextField("610");
        this.waveLenghtEmission.setForeground(Color.black);
        pnl.add(waveLenghtEmission);
        waveLenghtEmission.setBounds(200, 95, 100, 25);//(10, 5, 100, 25)

        JLabel pxSizeJL = new JLabel("Pixel Size (nm)", JLabel.CENTER);
        pnl.add(pxSizeJL);
        pxSizeJL.setBounds(5, 125, 195, 25);//(10, 5, 100, 25)
        this.pxSize = new HintTextField("150");
        this.pxSize.setForeground(Color.black);
        pnl.add(pxSize);
        pxSize.setBounds(200, 125, 100, 25);//(10, 5, 100, 25)

        this.compute = new JButton("Compute");
//        this.slImg = new JButton("Batch Analysis");
        this.compute.addActionListener(this);
        pnl.add(this.compute);
        this.compute.setBounds(105, 155, 100, 25);

        this.add(pnl);
        pnl.requestFocusInWindow();
        GUI.center(this);
        show();
        pnl.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try{
            Float indxMedium = Float.parseFloat(this.IndxRefracMedui.getText());
            Float indxOil = Float.parseFloat(this.OilRefracIndx.getText());
            Float NAval = Float.parseFloat(this.NAobjetive.getText());
            Float wl = Float.parseFloat(this.waveLenghtEmission.getText());
            Float pxS = Float.parseFloat(this.pxSize.getText());
            if(indxMedium>0 && indxOil>0 && NAval>0 && wl>0 && pxS>0){
                Float newNA = (indxMedium / indxOil) * NAval;
                Double psf = 0.5746*wl/newNA;
                Double psfS = psf / pxS;
                System.out.print(Math.round(100*psfS));
                Long psfStemp = Math.round(100*psfS);
                psfS = psfStemp.doubleValue() / 100;
                MSSR.newPsfSt = String.valueOf(psfS);
                MSSR.central.editPSFValue(MSSR.newPsfSt);
                MSSR.central.setEnabled(true);
                this.dispose();
            }
        } catch (NumberFormatException ex){
            
        }
    }
    
}

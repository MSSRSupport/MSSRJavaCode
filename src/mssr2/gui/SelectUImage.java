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
import ij.WindowManager;
import ij.gui.GUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mssr2.core.MSSR;

/**
 *
 * @author raul_
 */
public class SelectUImage extends JFrame implements ActionListener {
    public JComboBox<String> cBI;
    public SelectUImage(){
        super("Select an image from open images");
        MSSR.ImgsNames = WindowManager.getImageTitles();
        if(MSSR.ImgsNames.length > 0){
            super.setSize(410, 200);
        } else{
            super.setSize(410, 100);
        }
        super.setResizable(false);
        super.setAlwaysOnTop(true);
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel pnl = new JPanel();
        pnl.setLayout(null);

        if(MSSR.ImgsNames.length > 0){
            JLabel txtExp1 = new JLabel("If you press \"select image\", the selected image will be forced");
            JLabel txtExp2 = new JLabel("to be the image to analyze.");
            JLabel txtExp3 = new JLabel("if you want to reset the section, use de clean button.");
            pnl.add(txtExp1);
            pnl.add(txtExp2);
            pnl.add(txtExp3);
            txtExp1.setBounds(25, 5, 3880, 25);
            txtExp2.setBounds(125, 25, 200, 25);
            txtExp3.setBounds(45, 45, 360, 25);
            cBI = new JComboBox<>(MSSR.ImgsNames);
            pnl.add(cBI);
            cBI.setBounds(5, 75, 400, 25);
            JButton slB;
            slB = new JButton("Select Image");
            slB.addActionListener(this);
            pnl.add(slB);
            slB.setBounds(130, 105, 150, 25);
        } else{
            cBI = new JComboBox<>();
            JLabel txtExp = new JLabel("No Images");
            pnl.add(txtExp);
            txtExp.setBounds(175, 10, 230, 25);
            JButton slB;
            slB = new JButton("Cancel");
            slB.addActionListener(this);
            pnl.add(slB);
            slB.setBounds(130, 40, 150, 25);
        }

        this.add(pnl);
        pnl.requestFocusInWindow();
        GUI.center(this);
//        show();
        pnl.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
//        System.out.println(IJ.currentMemory());
        if("Select Image".equals(ae.getActionCommand())){
            IJ.selectWindow((String) this.cBI.getSelectedItem());
            MSSR.imgBase = WindowManager.getImage((String) this.cBI.getSelectedItem());
            MSSR.imgTitle = (String) this.cBI.getSelectedItem();
            MSSR.central.setEnabled(true);
            MSSR.forceImg = true;
            MSSR.central.extrainfo = 105;
            MSSR.central.modButtons();
            this.dispose();
        } else{
            MSSR.imgBase = null;
            MSSR.imgTitle = "";
            MSSR.central.setEnabled(true);
            MSSR.forceImg = false;
            MSSR.central.extrainfo = 0;
            MSSR.central.modButtons();
            this.dispose();
        }
    }
}
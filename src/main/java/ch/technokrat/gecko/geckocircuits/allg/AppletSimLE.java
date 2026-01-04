/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.allg;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;

import javax.swing.JApplet;
import javax.swing.JButton;


/**
 * Legacy applet launcher for GeckoCIRCUITS.
 *
 * This class extends JApplet to support legacy applet deployments. While applets
 * have been deprecated since Java 9, this functionality is maintained for backward
 * compatibility with existing deployments that may still rely on applet mode.
 *
 * Modern applications should use the standalone application mode (MainWindow) instead.
 */
@SuppressWarnings("removal")  // JApplet is deprecated but required for legacy applet support
public class AppletSimLE extends JApplet {


    public void init () {
        
        JButton butCircuitSim= new JButton("System Simulator");
        butCircuitSim.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent ae) {
                MainWindow win= new MainWindow();
                win.setSize(700,530);
                win.setLocation(300,200);
                win.setVisible(true);
            }
        });
        butCircuitSim.setBackground(Color.orange);
        this.setBackground(Color.white);
        this.getContentPane().add(butCircuitSim);
    }
}



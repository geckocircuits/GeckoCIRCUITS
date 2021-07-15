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

import ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;

import javax.swing.JApplet;
import javax.swing.JButton;


public class AppletSimLE extends JApplet {


    public void init () {
        
        JButton butCircuitSim= new JButton("System Simulator");
        butCircuitSim.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent ae) {
                Fenster win= new Fenster();
                win.setSize(700,530);
//                win.aktualisiereDividerSplitPane(700);
                win.setLocation(300,200);
                win.setVisible(true);
            }
        });
        butCircuitSim.setBackground(Color.orange);
        this.setBackground(Color.white);
        this.getContentPane().add(butCircuitSim);
        //---------------------------------
    }



}



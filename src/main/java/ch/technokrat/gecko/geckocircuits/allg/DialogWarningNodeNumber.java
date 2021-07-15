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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.ImageIcon;
import javax.swing.JDialog; 
import java.net.URL;



public class DialogWarningNodeNumber extends JDialog implements WindowListener, ActionListener {



    public DialogWarningNodeNumber () {
        super.setModal(true);
        try { this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL,"gecko.gif"))).getImage()); } catch (Exception e) {}
        this.addWindowListener(this);
        this.setTitle(" Warning: Node Number");
        this.baueGUI();
        this.setSize(280,200);
        setLocationRelativeTo(GeckoSim._win);
        this.setResizable(false);
        this.setVisible(true);
    }



    private void baueGUI () {
        Container con= this.getContentPane();
        con.setLayout(new BorderLayout());
        //------------------
        JButton knOK= GuiFabric.getJButton(I18nKeys.OK);
        knOK.setActionCommand("OK");
        knOK.addActionListener(this);
        //
        JPanel px= new JPanel();
        px.add(knOK);
        //------------------
        JPanel jpM= new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        //
        JTextArea jtx= new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx.setText("\n*** WARNING ***\n\nThe node number of the model has been changed."
            + "Proceeding with the simulation might give incorrect results.\n\n");
        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(this.getBackground());
        jtx.setEditable(false);
        //
        jpM.add(jtx, BorderLayout.CENTER);
        //------------------
        con.add(jpM, BorderLayout.CENTER);
        con.add(px, BorderLayout.SOUTH);
    }





    public void actionPerformed (ActionEvent ae) {
        if (ae.getActionCommand().equals("OK")) {
            this.dispose();
        }
    }



    //------------------------------------------------
    public void windowDeactivated (WindowEvent we) {}
    public void windowActivated (WindowEvent we) {}
    public void windowDeiconified (WindowEvent we) {}
    public void windowIconified (WindowEvent we) {}
    public void windowClosed (WindowEvent we) {}
    public void windowClosing (WindowEvent we) { this.dispose(); }
    public void windowOpened (WindowEvent we) {}
    //------------------------------------------------

}


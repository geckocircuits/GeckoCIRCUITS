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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

public class DialogViewPowerModule extends JDialog implements WindowListener, ActionListener {

    
    public DialogViewPowerModule(AbstractCircuitBlockInterface elementTH, Container c) {
        super.setModal(true);
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
        this.addWindowListener(this);
        //------------------------
        this.setTitle(" " + ((ThermMODUL) (elementTH)).getDateiname());
        JTabbedPane tabber = new JTabbedPane();
        tabber.addTab("RthCth-Network Model", c);
        tabber.addTab("3D Structure", this.baueGUI());
        //
        JButton jbOK = GuiFabric.getJButton(I18nKeys.OK);
        jbOK.setActionCommand("OK");
        jbOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                schliesseFenster();
            }
        });
        JPanel jpOK = new JPanel();
        jpOK.add(jbOK);
        //
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(tabber, BorderLayout.CENTER);
        con.add(jpOK, BorderLayout.SOUTH);
        //
        this.pack();
        this.setVisible(true);
        //------------------------
    }

    private JPanel baueGUI() {
        //------------------------
        // Grafische Beschreibung des PowerModule -->
        //
        JPanel pM = new JPanel();
        pM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        pM.setLayout(new BorderLayout());
        Image imgMx = null;
        try {
            imgMx = (new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "modulIntern.png"))).getImage();
        } catch (Exception e) {
            System.out.println(e);
        }
        final Image imgM = imgMx;
        JComponent jc1 = new JComponent() {
            public void paint(Graphics g) {
                g.drawImage(imgM, 0, 0, this);
            }
        };
        pM.add(jc1, BorderLayout.CENTER);
        return pM;
        //------------------------
    }

    //------------------------------------------------
    public void windowDeactivated(WindowEvent we) {
        //this.requestFocus(); 
    }

    public void windowActivated(WindowEvent we) {
    }

    public void windowDeiconified(WindowEvent we) {
    }

    public void windowIconified(WindowEvent we) {
    }

    public void windowClosed(WindowEvent we) {
    }

    public void windowClosing(WindowEvent we) {
        this.schliesseFenster();
    }

    public void windowOpened(WindowEvent we) {
    }
    //------------------------------------------------

    private void schliesseFenster() {
        //mutterFenster.gibFocusZurueck();
        this.dispose();
    }

    public void actionPerformed(ActionEvent ae) {
    }
}

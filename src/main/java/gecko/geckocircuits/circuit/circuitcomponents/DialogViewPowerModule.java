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
package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.geckocircuits.general.GlobalFilePathes;
import gecko.i18n.GuiFabric;
import gecko.i18n.resources.I18nKeys;
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
            @SuppressWarnings("deprecation")
            URL url = new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif");
            this.setIconImage(new ImageIcon(url).getImage());
        } catch (Exception e) {
            // ignored: icon loading is optional
        }
        this.addWindowListener(this);
        //------------------------
        this.setTitle(" " + ((ThermMODUL) elementTH).getDateiname());
        JTabbedPane tabber = new JTabbedPane();
        tabber.addTab("RthCth-Network Model", c);
        tabber.addTab("3D Structure", this.buildGUI());
        //
        JButton jbOK = GuiFabric.getJButton(I18nKeys.OK);
        jbOK.setActionCommand("OK");
        jbOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                closeWindow();
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

    private JPanel buildGUI() {
        //------------------------
        // Grafische Beschreibung des PowerModule -->
        //
        JPanel pM = new JPanel();
        pM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        pM.setLayout(new BorderLayout());
        Image imgMx = null;
        try {
            @SuppressWarnings("deprecation")
            URL url = new URL(GlobalFilePathes.PFAD_PICS_URL, "modulIntern.png");
            imgMx = new ImageIcon(url).getImage();
        } catch (Exception e) {
            System.out.println(e);
        }
        final Image imgM = imgMx;
        JComponent jc1 = new JComponent() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(imgM, 0, 0, this);
            }
        };
        pM.add(jc1, BorderLayout.CENTER);
        return pM;
        //------------------------
    }

    //------------------------------------------------
    @Override
    public void windowDeactivated(WindowEvent we) {
        //this.requestFocus();
    }

    @Override
    public void windowActivated(WindowEvent we) {
        // no-op
    }

    @Override
    public void windowDeiconified(WindowEvent we) {
        // no-op
    }

    @Override
    public void windowIconified(WindowEvent we) {
        // no-op
    }

    @Override
    public void windowClosed(WindowEvent we) {
        // no-op
    }

    @Override
    public void windowClosing(WindowEvent we) {
        this.closeWindow();
    }

    @Override
    public void windowOpened(WindowEvent we) {
        // no-op
    }
    //------------------------------------------------

    private void closeWindow() {
        //mutterFenster.gibFocusZurueck();
        this.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        // no-op
    }
}

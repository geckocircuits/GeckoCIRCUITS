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

import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author andy
 */
class LISNDialog extends DialogElementLK {

    public LISNDialog(final LISN parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        JPanel jpDefLISN = new JPanel();
        jpDefLISN.setLayout(new BorderLayout());
        JComponent jcLISN = new JComponent() {
            public void paint(Graphics g) {
                try {
                    g.setColor(Color.white);
                    g.fillRect(0, 0, 999, 999);
                    Image img = (new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "lisn.png"))).getImage();
                    g.drawImage(img, 10, 0, new JFrame());
                } catch (Exception e) {
                    System.out.println(e + "   srthrszhj5shj");
                }
            }
        };
        jcLISN.setPreferredSize(new Dimension(306 + 2 * 10, 322));
        jpDefLISN.add(jcLISN, BorderLayout.CENTER);
        //---------------
        JTabbedPane tabberLISN = new JTabbedPane();
        tabberLISN.addTab("Definition", jpDefLISN);
        //tabberLISN.addTab("Parameter", jpParLISN);
        //
        con.add(tabberLISN, BorderLayout.CENTER);

    }

    @Override
    public void processInputIndividual() {
        // nothing todo for LISN
    }
}

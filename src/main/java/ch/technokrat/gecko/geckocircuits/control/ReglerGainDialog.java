/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JPanel;

class ReglerGainDialog extends DialogElementCONTROL<ReglerGain> {
    public ReglerGainDialog(ReglerGain reglerGain) {
        super(reglerGain);    
    }

    @Override
    protected void baueGuiIndividual() {
        final int bG = 80,
                hG = 32,
                abstandG = 15;
        JComponent compImG = new JComponent() {
            public void paint(Graphics g) {
                g.setColor(Color.white);
                g.fillRect(0, 0, bG, hG);
                g.setColor(Color.black);
                g.drawRect(0, 0, bG - 1, hG - 1);
                //---------------
                // R(s)= r0  --> Vermeiden des Ladens eines Bildes
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("R(s) =  r", 6, 20);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("0", 60, 25);  // r0
            }
        };
        compImG.setPreferredSize(new Dimension(bG, hG + abstandG));
        JPanel pImG = new JPanel();
        pImG.setLayout(new BorderLayout());
        pImG.add(compImG, BorderLayout.CENTER);
        jpM.add(pImG, BorderLayout.NORTH);
        //
        JPanel pGA = createParameterPanel(element._gain);
        jpM.add(pGA, BorderLayout.CENTER);
    }
}

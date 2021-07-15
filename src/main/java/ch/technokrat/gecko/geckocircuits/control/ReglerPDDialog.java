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
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

class ReglerPDDialog extends DialogElementCONTROL<ReglerPD> {
   
    public ReglerPDDialog(final ReglerPD reglerPD) {
        super(reglerPD);        
    }

    @Override
    protected void baueGuiIndividual() {
        jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Parameter", TitledBorder.LEFT, TitledBorder.TOP));
        //
        final int bD = 100,
                hD = 45,
                abstandPD = 15;
        JComponent compPD = new JComponent() {
            public void paint(Graphics g) {
                g.setColor(Color.white);
                g.fillRect(0, 0, bD, hD);
                g.setColor(Color.black);
                g.drawRect(0, 0, bD - 1, hD - 1);
                //---------------
                // R(s)= a1*s  --> Vermeiden des Ladens eines Bildes
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("R(s) =  a", 6, 24);
                g.drawString("s", 75, 24);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("1", 62, 30);  // a1
            }
        };
        compPD.setPreferredSize(new Dimension(bD, hD + abstandPD));
        JPanel pPD2 = new JPanel();
        pPD2.setLayout(new BorderLayout());
        pPD2.add(compPD, BorderLayout.CENTER);
        jpM.add(pPD2, BorderLayout.NORTH);
        //
        JPanel pPD = createParameterPanel(element._gain);
        jpM.add(pPD, BorderLayout.CENTER);        
    }
    
            
}

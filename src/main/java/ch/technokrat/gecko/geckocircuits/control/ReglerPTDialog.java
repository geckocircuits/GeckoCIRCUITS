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

class ReglerPTDialog extends DialogElementCONTROL<AbstractReglerPT> {
    
    private static final int WIDTH_PT1 = 140;
    private static final int HEIGHT_PT1 = 45;
    private static final int DISTANCE_PT1 = 15;
    
    public ReglerPTDialog(final AbstractReglerPT reglerPT) {
        super(reglerPT);
    }

    @Override
    protected void baueGuiIndividual() {
        jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                "Parameter", TitledBorder.LEFT, TitledBorder.TOP));
        
        final JComponent compPT1 = new JComponent() {
            
            //CHECKSTYLE:OFF this mess comes from Uwe :-(
            @Override
            public void paint(final Graphics graphics) {
                graphics.setColor(Color.white);
                graphics.fillRect(0, 0, WIDTH_PT1, HEIGHT_PT1);
                graphics.setColor(Color.black);
                graphics.drawRect(0, 0, WIDTH_PT1 - 1, HEIGHT_PT1 - 1);
                
                if (element instanceof ReglerPT1) {
                    // R(s)= a1/(1+s*T)  --> Vermeiden des Ladens eines Bildes
                    graphics.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                    graphics.drawString("R(s) = ", 6, 24);
                    graphics.drawString("a", 70, 12);
                    graphics.drawString("1 + s T", 56, 33);
                    graphics.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                    graphics.drawString("1", 77, 15);  // a1
                    graphics.drawLine(55, 18, 110, 18);
                }

                if (element instanceof ReglerPT2) {
                    graphics.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                    graphics.drawString("R(s) = ", 6, 24);
                    graphics.drawString("a", 70, 12);
                    graphics.drawString("1 + ( s T )", 56, 33);
                    graphics.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                    graphics.drawString("1", 77, 15);  // a1
                    graphics.drawString("2", 122, 30);  // (s*T)^2
                    graphics.drawLine(55, 18, 130, 18);
                }

            }
            // CHECKSTYLE:ON
        };
        compPT1.setPreferredSize(new Dimension(WIDTH_PT1, HEIGHT_PT1 + DISTANCE_PT1));
        final JPanel pPT1 = new JPanel();
        pPT1.setLayout(new BorderLayout());
        pPT1.add(compPT1, BorderLayout.CENTER);
        jpM.add(pPT1, BorderLayout.NORTH);
        
        final JPanel pPT1a = createParameterPanel(element._TVal, element._a1Val);
        jpM.add(pPT1a, BorderLayout.CENTER);                
    }
}

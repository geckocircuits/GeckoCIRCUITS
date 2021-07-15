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

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ReglerIntegratorDialog extends DialogElementCONTROL<ReglerIntegrator> {

    public ReglerIntegratorDialog(final ReglerIntegrator reglerIntegrator) {
        super(reglerIntegrator);
    }

    @Override
    protected void baueGuiIndividual() {
        JComponent compIm2 = getImageComponent();
        JPanel pIm2 = new JPanel();
        pIm2.setLayout(new BorderLayout());
        pIm2.add(compIm2, BorderLayout.CENTER);
        jpM.add(pIm2, BorderLayout.NORTH);
        //
        JPanel pINT = createParameterPanel(element._a1Val, element._y0Val, element._minLimit, element._maxLimit);
        jpM.add(pINT, BorderLayout.CENTER);
        //
        // Erklaerender Text: 
        JLabel txtINT1 = new JLabel("z == 1    >>   Reset");
        txtINT1.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        txtINT1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        JPanel pImTxt = new JPanel();
        pImTxt.setLayout(new BorderLayout());
        pImTxt.add(new JLabel(" "), BorderLayout.NORTH);  // Vertikal-Abstand
        pImTxt.add(txtINT1, BorderLayout.CENTER);
        pImTxt.add(new JLabel(" "), BorderLayout.SOUTH);  // Vertikal-Abstand a
        //
        jpM.add(pImTxt, BorderLayout.SOUTH);        
    }

    private JComponent getImageComponent() {
        final int bi = 90,
                hi = 45,
                abstandi = 15;
        
        final JComponent compIm2 = new JComponent() {
            public void paint(Graphics g) {
                g.setColor(Color.white);
                g.fillRect(0, 0, bi, hi);
                g.setColor(Color.black);
                g.drawRect(0, 0, bi - 1, hi - 1);
                // R(s)= a1/s  --> Vermeiden des Ladens eines Bildes
                int q = 30;
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("R(s) =  ", 6, 24);
                g.drawString("a", 90 - q, 12);
                g.drawString("s", 90 - q, 32);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("1", 97 - q, 15);  // a1
                g.drawLine(86 - q, 18, 110 - q, 18);
            }
        };
        compIm2.setPreferredSize(new Dimension(bi, hi + abstandi));
        return compIm2;
    }
}

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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class ReglerPIDialog extends DialogElementCONTROL<ReglerPI> {

    public ReglerPIDialog(ReglerPI aThis) {
        super(aThis);
    }

    @Override
    protected void baueGuiIndividual() {        
        //
        final int b = 30,
                h = 45,
                abstand = 15;
        JLabel compIm = new JLabel("<html>R(s) = r<sub>0</sub> + a<sub>1</sub>/s<br>"
                + "R(s) = r<sub>0</sub> &middot (1 + 1 / (s &middot T))</html");


        compIm.setHorizontalAlignment(SwingConstants.CENTER);
        compIm.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        compIm.setPreferredSize(new Dimension(b, h + abstand));
        JPanel pIm = new JPanel();
        pIm.setLayout(new BorderLayout());
        pIm.add(compIm, BorderLayout.CENTER);
        jpM.add(pIm, BorderLayout.NORTH);
        //
        
        JPanel pPI = createParameterPanel(element._r0, element._a1, element._TimeConstant);
        jpM.add(pPI, BorderLayout.CENTER);

        final FormatJTextField r0TextField = tf.get(0);
        double r0Value = r0TextField.getNumberFromField();
        final FormatJTextField a1TextField = tf.get(1);
        double a1Value = a1TextField.getNumberFromField();
        
        final FormatJTextField timeTextField = tf.get(2);
        
        if (a1Value != 0) {
            timeTextField.setNumberToField(r0Value / a1Value);
        }

        KeyAdapter adapter = new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                try {
                    double r0Value = r0TextField.getNumberFromField();
                    double timeValue = timeTextField.getNumberFromField();
                    
                    if (timeValue != 0) {
                        double newA1Value = r0Value / timeValue;                                                
                        a1TextField.setNumberToField(newA1Value);
                    }
                } catch (Throwable ex) {
                    // really, here, I don't want any output messages!
                }
            }
        };

        r0TextField.addKeyListener(adapter);
        timeTextField.addKeyListener(adapter);        

        a1TextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    double a1Value = a1TextField.getNumberFromField();                     
                    if (a1Value != 0) {
                        timeTextField.setNumberToField(r0TextField.getNumberFromField() / a1Value);
                    }
                } catch (Throwable ex) {
                    // really, here, I don't want any output messages!
                }
            }
        });                               
    }
}

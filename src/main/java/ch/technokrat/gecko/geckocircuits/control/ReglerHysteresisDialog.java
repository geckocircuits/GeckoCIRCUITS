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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JPanel;

class ReglerHysteresisDialog extends AbstractDialogWithExternalOption<ReglerHysteresis> {
    private static final int IMAGE_COMPONENT_WIDTH = 130;
    private static final int IMAGE_COMPONENT_HEIGHT = 100;
        
    private JPanel _parameterPanel;

    public ReglerHysteresisDialog(final ReglerHysteresis reglerHys) {
        super(reglerHys);
    }

    @Override
    protected void baueGuiIndividual() {                
        final JComponent compImG2 = createImageComponent();
        compImG2.setPreferredSize(new Dimension(IMAGE_COMPONENT_WIDTH, IMAGE_COMPONENT_HEIGHT));
        
        final JPanel pImG2 = new JPanel();
        pImG2.setLayout(new BorderLayout());
        pImG2.add(compImG2, BorderLayout.CENTER);
        jpM.add(pImG2, BorderLayout.CENTER);
        //
        _parameterPanel = createParameterPanel(element._hysteresisThreshold);
        jpM.add(_parameterPanel, BorderLayout.SOUTH);                
        
        for(JComponent comp : getComponentsDisabledExternal()) {
            comp.setEnabled(!element.isExternalSet());
        }        
        
        jpM.add(_jCheckBoxUseExternal, BorderLayout.NORTH);
    }    

    private JComponent createImageComponent() {
        //CHECKSTYLE:OFF
        final int[] polyLineXCoord = new int[]{90, 82, 82, 90, 10, 50, 50, 47, 53, 50, 50};
        final int[] polyLineYCoord = new int[]{50, 47, 47, 50, 50, 50, 10, 18, 18, 10, 90};
        assert polyLineXCoord.length == polyLineYCoord.length;        

        return new JComponent() {
            @Override
            public void paint(final Graphics graphics) {
                RenderingHints oldRendering = ((Graphics2D) graphics).getRenderingHints();
                
                ((Graphics2D) graphics).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                
                graphics.setColor(Color.white);
                graphics.fillRect(0, 0, 100, 100);
                graphics.setColor(Color.black);
                graphics.drawRect(0, 0, 99, 99);
                graphics.setColor(Color.gray);
                graphics.drawPolyline(polyLineXCoord, polyLineYCoord, polyLineXCoord.length);
                graphics.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                graphics.drawString("in", 88, 47);
                graphics.drawString("out", 56, 12);
                graphics.setColor(Color.black);
                graphics.drawRect(35, 25, 30, 50);
                graphics.drawLine(50, 25, 95, 25);
                graphics.drawLine(50, 75, 5, 75);
                graphics.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                graphics.drawString("+h", 66, 69);
                graphics.drawString("-h", 17, 48);
                ((Graphics2D) graphics).setRenderingHints(oldRendering);
            }
        };
        //CHECKSTYLE:ON
    }

    @Override
    JComponent[] getComponentsDisabledExternal() {        
        return new JComponent[] {tf.get(0)};
    }
        
    
}

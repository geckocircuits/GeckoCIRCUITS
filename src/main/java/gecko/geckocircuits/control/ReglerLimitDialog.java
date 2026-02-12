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
package gecko.geckocircuits.control;

import gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

class ReglerLimitDialog extends AbstractDialogWithExternalOption<ReglerLimit> {    

    public ReglerLimitDialog(final ReglerLimit reglerLimit) {
        super(reglerLimit);
    }

    @Override
    protected void baueGuiIndividual() {        
        jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Parameter", TitledBorder.LEFT, TitledBorder.TOP));
        //
        JComponent compLIMIT = createImageComponent();
        compLIMIT.setPreferredSize(new Dimension(100, 115));
        JPanel pImLIMIT = new JPanel();
        pImLIMIT.setLayout(new BorderLayout());
        pImLIMIT.add(compLIMIT, BorderLayout.CENTER);
        jpM.add(pImLIMIT, BorderLayout.CENTER);
        //
        JPanel pLIMIT = createParameterPanel(element._minLimit, element._maxLimit);
        jpM.add(pLIMIT, BorderLayout.SOUTH);                
        
        for(JComponent comp : getComponentsDisabledExternal()) {
            comp.setEnabled(!element.isExternalSet());
        }        
        
        jpM.add(_jCheckBoxUseExternal, BorderLayout.NORTH);        
    }

    @Override
    JComponent[] getComponentsDisabledExternal() {
        return new JComponent[] {tf.get(0), tf.get(1)};
    }

    //CHECKSTYLE:OFF
    private JComponent createImageComponent() {
        final int[] polyXCoords = new int[]{90, 82, 82, 90, 10, 50, 50, 47, 53, 50, 50};
        final int[] polyYCoords = new int[]{50, 47, 53, 50, 50, 50, 10, 18, 18, 10, 90};
        
        assert polyXCoords.length == polyYCoords.length;
        
        
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
                graphics.drawPolyline(polyXCoords, polyYCoords, polyXCoords.length);
                graphics.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                graphics.drawString("in", 50 + 40 - 11, 50 + 19);
                graphics.drawString("out", 50 - 22, 50 - 40 + 17);
                graphics.setColor(Color.black);
                graphics.drawLine(65, 25, 95, 25);
                graphics.drawLine(35, 75, 5, 75);
                graphics.drawLine(35, 75, 65, 25);
                graphics.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                graphics.drawString("max", 65, 22);
                graphics.drawString("min", 6, 72);
                
                ((Graphics2D) graphics).setRenderingHints(oldRendering);
            }
        };   
        //CHECKSTYLE:OFF
    }
}

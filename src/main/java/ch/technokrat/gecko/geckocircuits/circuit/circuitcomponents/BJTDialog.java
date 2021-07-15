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

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

class BJTDialog extends DialogElementLK<BJT> {
    private JRadioButton _npnButton;
    private JRadioButton _pnpButton;
    private static final int NUMBER_GRID_ROWS = 9;
    
    public BJTDialog(final BJT parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        JPanel pIN = createParameterPanel(element._forwardBeta, element._backwardBeta, 
                element._baseResistance, element._emitterResistance, element._collectorResistance, element._forwardVoltage);        
        
        ButtonGroup npnpnpGroup = new ButtonGroup();
        _npnButton = new JRadioButton("NPN type");
        _pnpButton = new JRadioButton("PNP type");
    
        npnpnpGroup.add(_npnButton);
        npnpnpGroup.add(_pnpButton);
        if (element._isNpn.getValue()) {
            _npnButton.setSelected(true);
        } else {
            _pnpButton.setSelected(true);
        }

        _npnButton.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        _npnButton.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);

        _pnpButton.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        _pnpButton.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        GridLayout grid = (GridLayout) pIN.getLayout();
        grid.setRows(NUMBER_GRID_ROWS);
        pIN.add(new JLabel(""));
        pIN.add(_npnButton);
        pIN.add(_pnpButton);
        con.add(pIN, BorderLayout.CENTER);
    }
    
    @Override
    public void processInputIndividual() {
        element._isNpn.setUserValue(_npnButton.isSelected());                        
    }
}

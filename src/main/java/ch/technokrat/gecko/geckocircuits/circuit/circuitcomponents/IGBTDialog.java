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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public final class IGBTDialog extends AbstractDialogPowerSwitch<IGBT> {

    private JCheckBox jCheckBoxSatCurrent;
    private FormatJTextField _tfSaturationCurrent;

    public IGBTDialog(IGBT elementLK) {
        super(elementLK);
    }

    
    public JPanel createParameterPanel() {
        JPanel returnValue = createParameterPanel(element._forwardVoltageDrop, element._onResistance,
                element._offResistance, element.numberParalleled);        
        
        _tfSaturationCurrent = new FormatJTextField();
        
        double initISat = ((IGBT) element)._saturationCurrent.getValue();
        if (initISat <= 0) {
            initISat = 10;
        }
        _tfSaturationCurrent.setNumberToField(initISat);
        
        jCheckBoxSatCurrent = new JCheckBox("iSAT [A] =");
        jCheckBoxSatCurrent.setSelected(element._isSatCurEnabled.getValue());
        _tfSaturationCurrent.setEnabled(jCheckBoxSatCurrent.isSelected());
        jCheckBoxSatCurrent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                _tfSaturationCurrent.setEnabled(jCheckBoxSatCurrent.isSelected());
            }
        });

        jCheckBoxSatCurrent.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        GridLayout grid = (GridLayout) returnValue.getLayout();
        grid.setRows(grid.getRows()+2);
        grid.setColumns(2);
        returnValue.add(new JLabel(""));
        returnValue.add(jCheckBoxSatCurrent);        
        returnValue.add(_tfSaturationCurrent);
        return returnValue;
    }

    @Override
    public void processInputIndividual() {
        setValueFromTextField(element._saturationCurrent, _tfSaturationCurrent);        
        element._isSatCurEnabled.setUserValue(jCheckBoxSatCurrent.isSelected());        
    }    
}

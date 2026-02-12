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
package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.geckocircuits.allg.FormatJTextField;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

class MOSFETDialog extends AbstractDialogPowerSwitch<MOSFET> {

    protected FormatJTextField _antiUF;
    protected FormatJTextField _antiROn;
    protected FormatJTextField _antiROff;    
    
    public MOSFETDialog(final MOSFET parent) {
        super(parent);
    }


    @Override
    public JPanel createParameterPanel() {
        JPanel switchPanel = createParameterPanel(element._onResistance, element._offResistance, element.numberParalleled);
        switchPanel.setBorder(new TitledBorder("Switch parameters"));        
        JPanel diodePanel = createParameterPanel(element._adUf, element._adRon, element._adRoff);
        diodePanel.setBorder(new TitledBorder("Antiparallel diode parameters"));
        JPanel returnValue = new JPanel(new GridLayout(2,1));        
        returnValue.add(switchPanel);
        returnValue.add(diodePanel);
        return returnValue;
    }                           
}

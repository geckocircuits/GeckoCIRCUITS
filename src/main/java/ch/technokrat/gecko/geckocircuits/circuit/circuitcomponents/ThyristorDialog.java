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

import javax.swing.JPanel;

public final class ThyristorDialog extends AbstractDialogPowerSwitch<Thyristor> {

    public ThyristorDialog(final Thyristor parent) {
        super(parent);
    }
        

    @Override
    public JPanel createParameterPanel() {        
        return createParameterPanel(element._forwardVoltageDrop, element._onResistance, 
                element._offResistance, element._reverseRecoveryDelay);
    }
        
}

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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.control.DialogElementCONTROL;
import java.awt.BorderLayout;
import javax.swing.JPanel;

final class ThermPvChipDialog extends DialogElementLK {

    ThermPvChipDialog(final ThermPvChip parent) {
        super(parent);
    }

    @Override
    public void baueGUIIndividual() {
        JPanel jpM = null;
        if (element instanceof ComponentCoupable) {
            jpM = DialogElementCONTROL.createComponentCouplingPanel((AbstractBlockInterface) element);
        }
        con.add(jpM, BorderLayout.CENTER);
    }

    @Override
    public void processInputIndividual() {
        // nothing todo here!
    }
    
    
}

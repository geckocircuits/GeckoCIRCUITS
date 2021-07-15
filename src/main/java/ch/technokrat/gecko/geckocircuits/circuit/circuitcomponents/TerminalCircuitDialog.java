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
import ch.technokrat.gecko.geckocircuits.circuit.CircuitLabel;
import javax.swing.JLabel;

class TerminalCircuitDialog extends DialogElementLK {

    private FormatJTextField terminalLabel;

    public TerminalCircuitDialog(AbstractCircuitTerminal parent) {
        super(parent);        
    }

    @Override
    protected void baueGUIIndividual() {
        JLabel label = labelFabric("Label:");
        if(terminalLabel == null) {
            terminalLabel = new FormatJTextField();
        }
        
        terminalLabel.setText(element.XIN.get(0).getLabelObject().getLabelString());
        jPanelName.add(label);
        jPanelName.add(terminalLabel);
        terminalLabel.requestFocus();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        terminalLabel.grabFocus();
        terminalLabel.requestFocus();
        terminalLabel.selectAll();
    }

    @Override
    public void processInputIndividual() {   
        final CircuitLabel labelObject = element.XIN.get(0).getLabelObject();
        labelObject.setLabelFromUserDialog(terminalLabel.getText());
    }
}

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
import ch.technokrat.gecko.geckocircuits.circuit.CircuitLabel;
import javax.swing.JLabel;

class ReglerTerminalDialog extends DialogElementCONTROL {

    private FormatJTextField terminalLabel;

    public ReglerTerminalDialog(ReglerTERMINAL reglerTerminal) {
        super(reglerTerminal);
    }

    @Override
    protected void baueGuiIndividual() {
        JLabel label = new JLabel("Label:");
        terminalLabel = new FormatJTextField();
        terminalLabel.setText(element.XIN.get(0).getLabelObject().getLabelString());
        jPanelName.add(label);
        jPanelName.add(terminalLabel);
    }

    @Override
    protected void processInputs() {
        try {
            final CircuitLabel label = element.XIN.get(0).getLabelObject();
            label.setLabelFromUserDialog(terminalLabel.getText());
            this.schliesseFenster();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

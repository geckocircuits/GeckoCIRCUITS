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

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;

final class DialogReglerVariableInputs extends DialogElementCONTROL {

    private static final int NORTH_GRID_SIZE = 3;
    private JSpinner _spinnerInputNumber;

    public DialogReglerVariableInputs(AbstractReglerVariableInputs reglerVariableInputs) {
        super(reglerVariableInputs);
    }

    @Override
    protected void processInputs() {
        ((AbstractReglerVariableInputs) element)._inputTerminalNumber.setUserValue((Integer) _spinnerInputNumber.getValue());
    }

    @Override
    protected void baueGuiIndividual() {
        SpinnerModel spinnerModel = new javax.swing.SpinnerNumberModel(Integer.valueOf(element.XIN.size()),
                Integer.valueOf(2), null, Integer.valueOf(1));
        _spinnerInputNumber = new JSpinner(spinnerModel);
        JLabel jLabNoInputs = new JLabel("Number of inputs:");
        jPanelName.add(jLabNoInputs);
        jPanelName.add(_spinnerInputNumber);
    }

    @Override
    public int getRequiredGridSize() {
        return NORTH_GRID_SIZE;
    }
}

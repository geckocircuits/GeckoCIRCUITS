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

import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public final class ReglerAdd extends AbstractReglerVariableInputs {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerAdd.class, "ADD", I18nKeys.ADDITION);

    public ReglerAdd() {
        super(2);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"sum"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.SUM_OF_INPUTS};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (XIN.size() == 2) {
            return new AbstractControlCalculatable(2, 1) {

                @Override
                public void berechneYOUT(final double deltaT) {
                    _outputSignal[0][0] = _inputSignal[0][0] + _inputSignal[1][0];
                }
            };
        } else {
            return new AbstractControlCalculatable(XIN.size(), 1) {

                @Override
                public void berechneYOUT(final double deltaT) {
                    _outputSignal[0][0] = 0;
                    for (int i1 = 0; i1 < _inputSignal.length; i1++) {
                        _outputSignal[0][0] += _inputSignal[i1][0];
                    }
                }
            };
        }
    }    
}

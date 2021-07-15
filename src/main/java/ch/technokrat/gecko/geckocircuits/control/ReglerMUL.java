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

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public final class ReglerMUL extends AbstractReglerVariableInputs {
    static ControlTypeInfo tinfo = new ControlTypeInfo(ReglerMUL.class, "MUL", I18nKeys.MULTIPLICATION);

    public ReglerMUL() {
        super(2);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"product"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MULTIPLICATION_OF_INPUT};
    }            

    private class TwoParameterMultiplication extends AbstractControlCalculatable {

        public TwoParameterMultiplication() {
            super(2, 1);
        }
        
        @Override
        public void berechneYOUT(final double deltaT) {
            _outputSignal[0][0] = _inputSignal[0][0] * _inputSignal[1][0];
        }
    }

    private class MoreParameterMultiplication extends AbstractControlCalculatable {

        public MoreParameterMultiplication() {
            super(XIN.size(), 1);
        }
        
        @Override
        public void berechneYOUT(final double deltaT) {
            _outputSignal[0][0] = _inputSignal[0][0];
            for (int i = 1; i < _inputSignal.length; i++) {
                _outputSignal[0][0] *= _inputSignal[i][0];
            }
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {        
        if (XIN.size() == 2) {
            return new TwoParameterMultiplication();
        } else {
            return new MoreParameterMultiplication();
        }

    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);

        if (YOUT.size() > 1) { // this is a weird bug... for old GeckoCIRCUITS models, there may be two output terminal labels,
            // and therefore this was initialized wrong! I introduced this on 19.10.2012, maybe in the future, this bugfix
            // can be removed (when very old model files are not in use, anymore).
            setOutputTerminalNumber(1);
        }
    }    
    
}

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
package ch.technokrat.gecko.geckocircuits.control.calculators;

/**
 * Divide the first input by the second input. Do some error check, e.g. 0/0 should return 0
 * @author andreas
 */
public final class DivCalculator extends AbstractTwoInputsOneOutputCalculator {

    private static final double LARGE_NUMBER = 1e40;

    @Override
    public void berechneYOUT(final double deltaT) {
        final double result = _inputSignal[0][0] / _inputSignal[1][0];  // normale Division
        _outputSignal[0][0] = result;
        if(Double.isNaN(result)) { // here, we assume 0/0 = 0, since possibly no input is selected to this block!
            assert _inputSignal[0][0] == 0 : "illegal division! " + _inputSignal[0][0] + "/" + _inputSignal[1][0];
            assert _inputSignal[1][0] == 0 : "illegal division! " + _inputSignal[0][0] + "/" + _inputSignal[1][0];
            _outputSignal[0][0] = 0; 
        }
        
        if(Double.isInfinite(result)) { // this is the case when dividing e.g. 1/0
            
            //assert false : "illegal division! " + _inputSignal[0][0] + "/" + _inputSignal[1][0]; 
            // just return a largge value with the sign of the input.
            _outputSignal[0][0] = Math.signum(_inputSignal[0][0]) * LARGE_NUMBER;
        }                             
    }
}

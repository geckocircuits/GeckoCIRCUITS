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
package gecko.geckocircuits.control.calculators;

public final class LimitCalculatorInternal extends AbstractControlCalculatable {

    private double _lowerLimit;
    private double _upperLimit;

    public LimitCalculatorInternal(final double minLimit, final double maxLimit) {
        super(1, 1);
        setMinMaxValues(minLimit, maxLimit);        
    }
                

    @Override
    public void berechneYOUT(final double deltaT) {
        _outputSignal[0][0] = _inputSignal[0][0];        
        if (_inputSignal[0][0] <= _lowerLimit) {            
            _outputSignal[0][0] = _lowerLimit;
        } else if (_inputSignal[0][0] >= _upperLimit) {            
            _outputSignal[0][0] = _upperLimit;
        }
    }

    public void setMinMaxValues(final double min, final double max) {
        if(min > max) {
            throw new IllegalArgumentException("Error in limit calculation: the minimum value"
                    + "\nmust be smaller than the maximum value!");
        }
        _lowerLimit = min;
        _upperLimit = max;
    }
}

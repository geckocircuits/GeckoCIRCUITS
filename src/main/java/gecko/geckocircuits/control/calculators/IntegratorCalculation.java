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

public final class IntegratorCalculation extends AbstractControlCalculatable implements InitializableAtSimulationStart {

    private double _y1old = 0;  // Storage of Integral part
    private double _xold = 0, _y11 = -1;
    private double _xoldInit, _yoldInit;  // for Init&Start simulation
    private double _a1Val = 1;
    private final double _y0Val;
    private double _min = -1;
    private double _max = 1;  // default: G(s)= a1/s    

    public IntegratorCalculation(final double constantFactor, final double initValue, final double min, final double max) {
        super(2, 1);
        _a1Val = constantFactor;
        _y0Val = initValue;        
        setMinMax(min, max);                        
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        _y1old = _yoldInit;
        _xold = _xoldInit;
        _outputSignal[0][0] = _y0Val;
        _y1old = _y0Val;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        if (_inputSignal[1][0] < 1) {   // normaler Betrieb - Integration  
            
            _y11 = _y1old + _a1Val * deltaT * (_inputSignal[0][0] + _xold) / 2.0;
            if (_y11 <= _min) {
                _y11 = _min;
            }
            if (_y11 >= _max) {
                _y11 = _max;
            }
            _outputSignal[0][0] = _y11;
            _xold = _inputSignal[0][0];
            _y1old = _y11;
        } else {// reset: alles auf Null bzw. Init            
            _xold = 0;
            _y1old = _y0Val;
            _outputSignal[0][0] = _y0Val;
        }
    }

    public void setA1Val(final double a1Val) {
        this._a1Val = a1Val;
    }

    public void setMinMax(final double min, final double max) {
        if (min > max) {
            throw new IllegalArgumentException("Error: minimum value is larger than maximum!");
        }
        _min = min;
        _max = max;

    }
}

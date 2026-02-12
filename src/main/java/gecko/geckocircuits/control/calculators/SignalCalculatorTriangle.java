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

public final class SignalCalculatorTriangle extends AbstractSignalCalculatorPeriodic {    
    public SignalCalculatorTriangle(final int noInputs, final double amplitudeAC, final double frequency,
            final double phase, final double anteilDC, final double duty) {
        super(noInputs, amplitudeAC, frequency, phase, anteilDC, duty);        
    }    
    
    /**
     * Todo: this function is duplicated @see SignalCalculatorRectangle     
     */
    @Override
    protected void calculateStartSignal(final double dtx, final double txEnd, final double phaseX) {
        double txValue = 0;
        while (txValue < (txEnd * phaseX / (2 * Math.PI))) {
            final double dyUPx = (_amplitudeAC * 2 * _frequency * dtx) / _dutyRatio;
            final double dyDOWNx = (_amplitudeAC * 2 * _frequency * dtx) / (1 - _dutyRatio);
                        
            if (_aufsteigend) {
                _triangle += dyUPx;
            } else {
                _triangle -= dyDOWNx;
            }
            if (_amplitudeAC != 0) {  // bei t==0 kann es hier Verwirrung geben!
                if (_triangle >= _amplitudeAC) {
                    _triangle = _amplitudeAC;
                    _aufsteigend = false;
                } else if (_triangle <= -_amplitudeAC) {
                    _triangle = -_amplitudeAC;
                    _aufsteigend = true;
                }
            }
            txValue += dtx;
        }
        _triangle = -_triangle;
    }


    @Override
    public void berechneYOUT(final double deltaT) {
        //signal= amplitudeAC*(2/Math.PI*Math.asin(Math.sin(2*Math.PI*frequenz*t -phase))) +anteilDC; 
        _dyUP = (_amplitudeAC * 2 * _frequency * deltaT) / _dutyRatio;
        _dyDOWN = (_amplitudeAC * 2 * _frequency * deltaT) / (1 - _dutyRatio);
                        
        
        if (_aufsteigend) {
            _triangle += _dyUP;
        } else {
            _triangle -= _dyDOWN;
        }
        
        if (_amplitudeAC != 0) {  // bei t==0 kann es hier Verwirrung geben!
            if (_triangle >= +_amplitudeAC) {
                _triangle = +_amplitudeAC;
                _aufsteigend = false;
            } else if (_triangle <= -_amplitudeAC) {
                _triangle = -_amplitudeAC;
                _aufsteigend = true;
            }
        }
        _outputSignal[0][0] = _triangle + _anteilDC;
    }
}

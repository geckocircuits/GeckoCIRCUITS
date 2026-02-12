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

public final class SignalCalculatorRectangle extends AbstractSignalCalculatorPeriodic {

    private static final double FOUR = 4;

    public SignalCalculatorRectangle(final int noInputs, final double amplitudeAC,
            final double frequency, final double phase, final double anteilDC, final double duty) {
        super(noInputs, amplitudeAC, frequency, phase, anteilDC, duty);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        //double dphi= Math.PI*(0.5-tastverhaeltnis);  // Korrekturwinkel, damit Rechteck-Signal immer im Ursprung beginnt
        //double fdr= 1/Math.PI*Math.asin(Math.sin(2*Math.PI*frequenz*t -phase +dphi)) +0.5;  // [0...1]
        _dyUP = FOUR * _frequency * deltaT;
        _dyDOWN = FOUR * _frequency * deltaT;
        if (_aufsteigend) {
            _triangle += _dyUP;
        } else {
            _triangle -= _dyDOWN;
        }
        if (_triangle >= 1) {
            _triangle = 1;
            _aufsteigend = false;
        } else if (_triangle <= -1) {
            _triangle = -1;
            _aufsteigend = true;
        }
        if (_triangle > 1 - 2 * _dutyRatio) {
            _outputSignal[0][0] = _amplitudeAC + _anteilDC;
        } else {
            _outputSignal[0][0] = _anteilDC;
        }
    }

    /**
     * Todo: this function is duplicated @see SignalCalculatorTriangle     
     */
    @Override
    protected void calculateStartSignal(final double dtx, final double txEnd,
            final double phaseX) {
        
        double txValue = 0;
        double txE = txEnd * phaseX / (2 * Math.PI) - (1 - 2 * _dutyRatio) / (4 * _frequency);
        if (txE < 0) {
            txE += txEnd;
        }
        while (txValue < txE) {
            final double dyUPx = 2 * _frequency * dtx / 0.5;
            final double dyDOWNx = 2 * _frequency * dtx / (1 - 0.5);
            if (_aufsteigend) {
                _triangle += dyUPx;
            } else {
                _triangle -= dyDOWNx;
            }
            if (_amplitudeAC != 0) {  // bei t==0 kann es hier Verwirrung geben!
                if (_triangle >= 1) {
                    _triangle = 1;
                    _aufsteigend = false;
                } else if (_triangle <= -1) {
                    _triangle = -1;
                    _aufsteigend = true;
                }
            }
            txValue += dtx;
        }
        _triangle = -_triangle;
    }
}

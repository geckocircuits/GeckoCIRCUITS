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

public class SignalCalculatorSinus extends AbstractSignalCalculatorPeriodic {
    
    /**
     * 
     * @param noInputs
     * @param amplitudeAC
     * @param frequency in Hz
     * @param phase measured in Radians
     * @param anteilDC
     * @param duty in fact, this parameter is not even used here. For conveniece, I kept it
     * to use similarities with signalREct and signalTri
     */
    public SignalCalculatorSinus(final int noInputs,  final double amplitudeAC,
            final double frequency, final double phase, final double anteilDC, final double duty) {
        super(noInputs, amplitudeAC, frequency, phase, anteilDC, duty);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        _outputSignal[0][0] = _amplitudeAC * Math.sin(TWO_PI * _frequency * _time 
                - _phase) + _anteilDC;
    }

    @Override
    protected void calculateStartSignal(double dtx, double txEnd, double phaseX) {
        // nothing todo for sinus-calculator.
    }
}

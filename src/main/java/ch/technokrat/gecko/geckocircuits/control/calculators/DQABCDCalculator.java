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

public final class DQABCDCalculator extends AbstractControlCalculatable {

    private static final double TWO_THIRD = 0.6666666666666;
    private static final double TWO_PI_THIRD = Math.PI * TWO_THIRD;
    private static final int NO_INPUTS = 3;
    private static final int NO_OUTPUTS = 3;

    public DQABCDCalculator() {
        super(NO_INPUTS, NO_OUTPUTS);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        final double dVal = _inputSignal[0][0];
        final double qVal = _inputSignal[1][0];
        final double theta = _inputSignal[2][0];
        final double aVal = dVal * Math.cos(theta) - qVal * Math.sin(theta);
        final double cVal = dVal * Math.cos(theta + TWO_PI_THIRD) - qVal * Math.sin(theta + TWO_PI_THIRD);
        final double bVal = -(aVal + cVal);
        _outputSignal[0][0] = aVal;
        _outputSignal[1][0] = bVal;
        _outputSignal[2][0] = cVal;
    }
}

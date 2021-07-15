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

public final class ABCDQCalculator extends AbstractControlCalculatable {

    private static final double SQRT3 = Math.sqrt(3);
    private static final int NR_INPUTS = 4;
    private static final int NR_OUTPUTS = 2;
    private static final int THETA_INPUT_INDEX = 3;
    private static final int THREE = 3;
    
    public ABCDQCalculator() {
        super(NR_INPUTS, NR_OUTPUTS);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        final double aVal = _inputSignal[0][0];
        final double bVal = _inputSignal[1][0];
        final double cVal = _inputSignal[2][0];
        final double theta = _inputSignal[THETA_INPUT_INDEX][0];
        final double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);

        final double dVal = (2 * (aVal - bVal) * cosTheta + (bVal - cVal) * (cosTheta + SQRT3 * sinTheta)) / THREE;
        final double qVAl = -(2 * (aVal - bVal) * sinTheta + (bVal - cVal) * (sinTheta - SQRT3 * cosTheta)) / THREE;
        _outputSignal[0][0] = dVal;
        _outputSignal[1][0] = qVAl;
    }
}

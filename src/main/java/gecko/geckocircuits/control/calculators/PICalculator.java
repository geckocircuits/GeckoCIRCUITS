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

public final class PICalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {

    private double _r0;
    private double _a1;
    private double y1alt = 0;  // Speicherung des I-Anteils
    private double xalt = 0, y11 = -1;

    public PICalculator(final double r0, final double a1) {
        super(1, 1);
        _r0 = r0;
        _a1 = a1;
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        y1alt = 0;
        xalt = 0;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        y11 = y1alt + 0.5 * _a1 * deltaT * (_inputSignal[0][0] + xalt);
        _outputSignal[0][0] = y11 + _r0 * _inputSignal[0][0];
        xalt = _inputSignal[0][0];
        y1alt = y11;
    }

    public void setA1(final double a1) {
        _a1 = a1;
    }

    public void setR0(final double r0) {
        _r0 = r0;
    }
}
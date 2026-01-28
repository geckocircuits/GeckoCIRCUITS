/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.core.control.calculators;

import ch.technokrat.gecko.core.control.NotCalculateableMarker;

/**
 * Calculator that outputs a constant value.
 *
 * This block has no inputs and one output. The output value is set via the
 * constructor or setConst() method and remains constant throughout the simulation.
 *
 * Since this implements NotCalculateableMarker, berechneYOUT() is never called
 * during normal simulation - the output is set directly when the constant changes.
 *
 * @author GeckoCIRCUITS Team
 */
public final class ConstantCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    /**
     * Creates a constant calculator with the specified output value.
     *
     * @param constValue the constant output value
     */
    public ConstantCalculator(final double constValue) {
        super(0, 1);
        setConst(constValue);
    }

    /**
     * Sets the constant output value.
     *
     * Since this calculator is "not calculatable", berechneYOUT will never be
     * executed. Therefore, the output must be updated immediately when setting
     * the constant value.
     *
     * @param constValue the new constant value
     */
    public void setConst(final double constValue) {
        _outputSignal[0][0] = constValue;
    }

    /**
     * Gets the current constant value.
     *
     * @return the constant output value
     */
    public double getConst() {
        return _outputSignal[0][0];
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        // This is not calculatable - method should never be called
        assert false : "ConstantCalculator.berechneYOUT should never be called";
    }
}

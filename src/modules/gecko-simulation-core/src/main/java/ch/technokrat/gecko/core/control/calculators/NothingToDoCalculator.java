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
 * A no-operation calculator that does nothing.
 *
 * This calculator is used as a placeholder for blocks that don't require
 * any computation but need to exist in the control block hierarchy.
 * It accepts any number of inputs and outputs but performs no calculation.
 *
 * Since this implements NotCalculateableMarker, the simulation engine knows
 * it can skip this block during the calculation phase.
 *
 * @author GeckoCIRCUITS Team
 */
public class NothingToDoCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    /**
     * Creates a no-operation calculator with the specified number of inputs and outputs.
     *
     * @param noInputs number of input terminals
     * @param noOutputs number of output terminals
     */
    public NothingToDoCalculator(int noInputs, int noOutputs) {
        super(noInputs, noOutputs);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        // Nothing to do, as the class name says!
    }
}

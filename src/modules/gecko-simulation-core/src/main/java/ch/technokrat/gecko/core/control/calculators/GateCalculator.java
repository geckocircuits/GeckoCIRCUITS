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
 * Calculator for gate signal handling.
 *
 * This is a marker block with one input and no outputs. It is used to receive
 * gate signals for switching components. The actual gate signal processing
 * happens elsewhere - this calculator just provides the input connection point.
 *
 * Since this implements NotCalculateableMarker, berechneYOUT() is never called.
 *
 * @author GeckoCIRCUITS Team
 */
public final class GateCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    /**
     * Creates a gate calculator with one input and no outputs.
     */
    public GateCalculator() {
        super(1, 0);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        // This is not calculatable - method should never be called
        assert false : "GateCalculator.berechneYOUT should never be called";
    }
}

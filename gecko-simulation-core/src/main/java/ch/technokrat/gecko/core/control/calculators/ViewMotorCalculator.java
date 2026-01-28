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
 * Calculator placeholder for motor visualization blocks.
 *
 * This is a marker calculator used with motor view components. The actual
 * visualization happens in the GUI layer - this calculator exists only to
 * provide the control block structure required by the simulation engine.
 *
 * Since this implements NotCalculateableMarker, berechneYOUT() is never called.
 *
 * @author GeckoCIRCUITS Team
 */
public final class ViewMotorCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    /**
     * Creates a view motor calculator with no inputs and one output.
     */
    public ViewMotorCalculator() {
        super(0, 1);
    }

    @Override
    public void berechneYOUT(double deltaT) {
        // This is not calculatable - method should never be called
        assert false : "ViewMotorCalculator.berechneYOUT should never be called";
    }
}

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
package ch.technokrat.gecko.core.control;

/**
 * Interface for control blocks that need to be notified when the time step changes.
 *
 * Some control blocks maintain internal state that depends on the time step (dt).
 * For example, delay elements with fixed-size buffers need to recalculate their
 * buffer sizes and interpolate values when dt changes during simulation.
 *
 * This interface allows the simulation engine to notify such blocks when the
 * time step is modified (e.g., during adaptive time stepping or stop-continue).
 *
 * @author GeckoCIRCUITS Team
 */
public interface IsDtChangeSensitive {

    /**
     * Called when the simulation time step changes.
     *
     * Implementations should recalculate any internal state that depends on dt,
     * such as buffer sizes, filter coefficients, or integration constants.
     *
     * @param dt the new time step in seconds
     */
    void initWithNewDt(double dt);
}

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
package ch.technokrat.gecko.core.circuit.matrix;

/**
 * Extended stamper interface for switching/stateful components.
 *
 * This interface extends IMatrixStamper to support components that have internal
 * state which affects their behavior, such as diodes, thyristors, IGBTs, and
 * ideal switches.
 *
 * Stateful components may require matrix re-factorization when their state changes,
 * and the solver may need to iterate until all component states converge.
 *
 * Typical usage:
 * <pre>
 *     1. Solve MNA equations with current state
 *     2. Call updateState() with solved voltages and currents
 *     3. If isStateChanged() returns true:
 *        - Re-stamp matrix with new state
 *        - Repeat from step 1
 *     4. Call resetStateChange() after convergence
 * </pre>
 *
 * GUI-free version for use in headless simulation core.
 */
public interface IStatefulStamper extends IMatrixStamper {

    /**
     * Updates the internal state based on circuit conditions.
     *
     * This method should be called after solving the MNA equations to
     * determine if the component's state should change (e.g., diode
     * turning on/off).
     *
     * @param vx voltage at node X
     * @param vy voltage at node Y
     * @param current current through the component
     * @param time current simulation time
     */
    void updateState(double vx, double vy, double current, double time);

    /**
     * Checks if the component state has changed since the last check.
     *
     * If true, the MNA matrix needs to be re-stamped with the new
     * component parameters (e.g., different resistance for ON vs OFF).
     *
     * @return true if state has changed and matrix update is needed
     */
    boolean isStateChanged();

    /**
     * Resets the state-changed flag.
     *
     * Should be called after the solver has converged and the state
     * change has been processed.
     */
    void resetStateChange();

    /**
     * Gets the current ON/OFF state of the component.
     *
     * @return true if component is in conducting (ON) state
     */
    boolean isOn();

    /**
     * Forces the component to a specific state.
     *
     * Useful for initialization or controlled switching.
     *
     * @param on true to set ON state, false for OFF
     */
    void setState(boolean on);

    /**
     * Gets the current effective resistance of the component.
     *
     * For switching components, this returns different values
     * depending on whether the component is ON or OFF.
     *
     * @return current effective resistance in Ohms
     */
    double getCurrentResistance();
}

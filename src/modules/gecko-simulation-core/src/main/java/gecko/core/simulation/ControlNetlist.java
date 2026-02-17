/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.core.simulation;

import gecko.core.control.calculators.AbstractControlCalculatable;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Headless control block netlist for executing control calculators.
 * Manages execution order of control blocks in a simulation without GUI dependencies.
 *
 * <p>This class was extracted from {@code gecko.geckocircuits.control.NetzlisteCONTROL} to provide
 * GUI-free control block execution for REST APIs, batch processing, and headless simulations.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * List<AbstractControlCalculatable> orderedCalcs = buildCalculators();
 * ControlNetlist netlist = ControlNetlist.fromOrderedCalculators(orderedCalcs);
 *
 * double dt = 1e-6;  // time step
 * for (double time = 0; time <= 0.01; time += dt) {
 *     netlist.executeTimeStep(dt, time);
 *     // Read output signals from calculators
 * }
 * }</pre>
 */
public class ControlNetlist {

    private AbstractControlCalculatable[] sortedCalculators = new AbstractControlCalculatable[0];
    private double simulationTime;

    /**
     * Creates an empty ControlNetlist with no calculators.
     * Suitable for circuits with no control blocks.
     */
    public ControlNetlist() {
        this.simulationTime = 0.0;
    }

    /**
     * Sets the ordered list of calculators to execute.
     * Must be called before executeTimeStep() to execute any calculations.
     *
     * @param calculators the list of control calculators in execution order
     * @throws NullPointerException if calculators is null
     */
    public void setSortedCalculators(List<AbstractControlCalculatable> calculators) {
        if (calculators == null) {
            throw new NullPointerException("Calculators list cannot be null");
        }
        this.sortedCalculators = calculators.toArray(new AbstractControlCalculatable[0]);
    }

    /**
     * Execute all control calculators for one simulation time step.
     * Calculators are executed in the order they were added via setSortedCalculators().
     *
     * @param dt the time step size in seconds
     * @param time the current simulation time in seconds
     */
    public void executeTimeStep(double dt, double time) {
        this.simulationTime = time;
        AbstractControlCalculatable.setTime(time);
        for (AbstractControlCalculatable calc : sortedCalculators) {
            if (calc != null) {
                calc.berechneYOUT(dt);
            }
        }
    }

    /**
     * Returns the number of calculators managed by this netlist.
     *
     * @return the number of control calculators
     */
    public int getCalculatorCount() {
        return sortedCalculators.length;
    }

    /**
     * Returns the current simulation time from the last executeTimeStep() call.
     *
     * @return the simulation time in seconds
     */
    public double getSimulationTime() {
        return simulationTime;
    }

    /**
     * Returns true if this netlist has any calculators to execute.
     *
     * @return true if calculator count > 0, false otherwise
     */
    public boolean hasCalculators() {
        return sortedCalculators.length > 0;
    }

    /**
     * Returns a copy of the internal calculator array.
     * Useful for inspection and debugging.
     *
     * @return array of sorted calculators
     */
    public AbstractControlCalculatable[] getSortedCalculators() {
        AbstractControlCalculatable[] copy = new AbstractControlCalculatable[sortedCalculators.length];
        System.arraycopy(sortedCalculators, 0, copy, 0, sortedCalculators.length);
        return copy;
    }

    // ========== Factory Methods ==========

    /**
     * Create an empty ControlNetlist with no calculators.
     * Suitable for circuits that have no control blocks.
     *
     * @return a new ControlNetlist with 0 calculators
     */
    public static ControlNetlist createEmpty() {
        return new ControlNetlist();
    }

    /**
     * Create a ControlNetlist from a pre-ordered list of calculators.
     * The calculators must already be sorted in the correct execution order.
     *
     * @param orderedCalculators list of calculators in execution order
     * @return a new ControlNetlist configured with the provided calculators
     * @throws NullPointerException if orderedCalculators is null
     */
    public static ControlNetlist fromOrderedCalculators(List<AbstractControlCalculatable> orderedCalculators) {
        if (orderedCalculators == null) {
            throw new NullPointerException("Ordered calculators list cannot be null");
        }
        ControlNetlist netlist = new ControlNetlist();
        netlist.setSortedCalculators(orderedCalculators);
        return netlist;
    }

    /**
     * Create a ControlNetlist from a pre-ordered array of calculators.
     * The calculators must already be sorted in the correct execution order.
     *
     * @param orderedCalculators array of calculators in execution order
     * @return a new ControlNetlist configured with the provided calculators
     * @throws NullPointerException if orderedCalculators is null
     */
    public static ControlNetlist fromOrderedCalculators(AbstractControlCalculatable[] orderedCalculators) {
        if (orderedCalculators == null) {
            throw new NullPointerException("Ordered calculators array cannot be null");
        }
        ControlNetlist netlist = new ControlNetlist();
        netlist.sortedCalculators = new AbstractControlCalculatable[orderedCalculators.length];
        System.arraycopy(orderedCalculators, 0, netlist.sortedCalculators, 0, orderedCalculators.length);
        return netlist;
    }

    /**
     * Returns an unmodifiable list of the current calculators.
     *
     * @return list view of the sorted calculators
     */
    public List<AbstractControlCalculatable> getCalculatorsList() {
        List<AbstractControlCalculatable> list = new ArrayList<>(sortedCalculators.length);
        for (AbstractControlCalculatable calc : sortedCalculators) {
            list.add(calc);
        }
        return Collections.unmodifiableList(list);
    }
}

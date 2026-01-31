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
package ch.technokrat.gecko.core.circuit;

import ch.technokrat.gecko.core.allg.SolverType;

/**
 * Pure simulation interface for circuit components.
 * NO GUI DEPENDENCIES - can be extracted to gecko-simulation-core.
 * 
 * <p>This interface defines the contract for circuit component calculators
 * that perform time-domain simulation. All methods are pure simulation logic
 * with no dependency on java.awt, javax.swing, or any UI framework.
 * 
 * <p>Calculator lifecycle:
 * <ol>
 *   <li>{@link #init()} - Initialize state before simulation starts</li>
 *   <li>{@link #assignTerminalIndices()} - Connect to matrix indices</li>
 *   <li>{@link #calculateVoltage(double[])} - Calculate voltage from potentials</li>
 *   <li>Stamping methods - Contribute to system matrix</li>
 *   <li>{@link #saveHistory()} / {@link #stepBack()} - History management</li>
 * </ol>
 * 
 * @author GeckoCIRCUITS Team
 * @since 2.0 (refactored for GUI-free extraction)
 */
public interface ICircuitCalculator {
    
    /**
     * Initialize calculator state before simulation.
     * Resets all voltages, currents, and potentials to zero.
     */
    void init();
    
    /**
     * Assign terminal indices from the circuit matrix.
     * Must be called after circuit topology is established.
     */
    void assignTerminalIndices();
    
    /**
     * Get the voltage across the component.
     * @return voltage in volts (V)
     */
    double getVoltage();
    
    /**
     * Get the current through the component.
     * @return current in amperes (A)
     */
    double getCurrent();
    
    /**
     * Get the potential at a specific terminal.
     * @param terminalNumber 0 for first terminal, 1 for second terminal
     * @return potential in volts (V)
     */
    double getPotential(int terminalNumber);
    
    /**
     * Calculate voltage from the potential array.
     * @param potentials array of node potentials from circuit solver
     */
    void calculateVoltage(double[] potentials);
    
    /**
     * Get the matrix index for a specific terminal.
     * @param terminalNumber 0 for first terminal, 1 for second terminal
     * @return the matrix index, or -1 if not assigned
     */
    int getTerminalIndex(int terminalNumber);
    
    /**
     * Get the current from previous timestep.
     * Used for integration methods requiring history.
     * @return previous current in amperes (A)
     */
    double getOldCurrent();
    
    /**
     * Save current state to history for potential step-back.
     * Called at end of successful timestep.
     */
    void saveHistory();
    
    /**
     * Perform history maintenance after timestep.
     * @param time current simulation time
     */
    void historyMaintainance(double time);
    
    /**
     * Step back one timestep in simulation history.
     * Used when solver needs to retry with smaller step.
     */
    void stepBack();
    
    /**
     * Get the solver type for this calculator.
     * @return the integration method (BE, TRZ, or GS)
     */
    SolverType getSolverType();
    
    /**
     * Set the component number in the circuit.
     * @param componentNumber unique identifier for this component
     */
    void setComponentNumber(int componentNumber);
    
    /**
     * Get the component number.
     * @return the unique identifier for this component
     */
    int getComponentNumber();
}

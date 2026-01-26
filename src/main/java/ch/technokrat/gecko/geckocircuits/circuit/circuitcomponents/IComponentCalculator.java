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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;

/**
 * Standardized interface for circuit component calculators.
 * 
 * <p>This interface provides a unified contract for calculator components
 * that perform time-domain simulation calculations. It consolidates the
 * common patterns found in CapacitorCalculator and InductorCalculator.
 * 
 * <p>Calculator lifecycle:
 * <ol>
 *   <li>{@link #initialize()} - Called before simulation starts</li>
 *   <li>{@link #stampMatrix(double[][], double[])} - Stamp values into system matrix</li>
 *   <li>{@link #calculateCurrent(double[], double, double)} - Calculate current after solve</li>
 *   <li>{@link #updateHistory()} - Store values for next timestep</li>
 * </ol>
 * 
 * <p>Supported solver types:
 * <ul>
 *   <li>{@link SolverType#SOLVER_BE} - Backward Euler (first-order, stable)</li>
 *   <li>{@link SolverType#SOLVER_TRZ} - Trapezoidal (second-order, accurate)</li>
 *   <li>{@link SolverType#SOLVER_GS} - Gear-Shichman (third-order, for stiff systems)</li>
 * </ul>
 * 
 * @see CapacitorCalculator
 * @see InductorCalculator
 * @see CurrentCalculatable
 * @see AStampable
 * @see BStampable
 */
public interface IComponentCalculator extends CurrentCalculatable {
    
    /**
     * Initialize calculator state before simulation.
     * Called once at simulation start to set initial conditions.
     */
    void initialize();
    
    /**
     * Get the current solver type.
     * 
     * @return the active solver type
     */
    SolverType getSolverType();
    
    /**
     * Set the solver type for integration.
     * 
     * @param solverType the solver to use (BE, TRZ, or GS)
     */
    void setSolverType(SolverType solverType);
    
    /**
     * Get the component's primary value (capacitance, inductance, etc.).
     * 
     * @return the component value in SI units
     */
    double getComponentValue();
    
    /**
     * Set the component's primary value.
     * 
     * @param value the component value in SI units
     */
    void setComponentValue(double value);
    
    /**
     * Get the initial condition (voltage for C, current for L).
     * 
     * @return the initial value
     */
    double getInitialCondition();
    
    /**
     * Set the initial condition.
     * 
     * @param initialValue the initial value (voltage or current)
     */
    void setInitialCondition(double initialValue);
    
    /**
     * Calculate the equivalent conductance for the companion model.
     * 
     * <p>For capacitor (Backward Euler): G = C/dt
     * <p>For capacitor (Trapezoidal): G = 2*C/dt
     * <p>For inductor (Backward Euler): G = dt/L
     * <p>For inductor (Trapezoidal): G = dt/(2*L)
     * 
     * @param deltaT the simulation time step
     * @return the equivalent conductance
     */
    double calculateEquivalentConductance(double deltaT);
    
    /**
     * Calculate the history current source for the companion model.
     * This represents the "memory" of the component from previous timesteps.
     * 
     * @param deltaT the simulation time step
     * @return the history current source value
     */
    double calculateHistorySource(double deltaT);
    
    /**
     * Get the voltage across the component.
     * 
     * @return voltage in volts
     */
    double getVoltage();
    
    /**
     * Get the stored energy in the component.
     * 
     * <p>For capacitor: E = 0.5 * C * V^2
     * <p>For inductor: E = 0.5 * L * I^2
     * 
     * @return energy in joules
     */
    double getStoredEnergy();
    
    /**
     * Check if the component supports non-linear behavior.
     * 
     * @return true if non-linear modeling is supported
     */
    boolean isNonLinear();
    
    /**
     * Update the component value for non-linear components.
     * Called during Newton-Raphson iterations.
     * 
     * @param operatingPoint the current operating point (voltage or current)
     */
    void updateNonLinearValue(double operatingPoint);
}

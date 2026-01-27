/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Simulation engine components: solver context and integration methods.
 * 
 * <p>This package contains the numerical integration algorithms and solver
 * configuration used by the circuit simulator.</p>
 * 
 * <h2>Classes (from legacy circuit.simulation package)</h2>
 * <ul>
 *   <li>{@code SolverContext} - Encapsulates solver type (BE, TRZ, GS) and timestep</li>
 *   <li>{@code SimulationState} - Holds current simulation state variables</li>
 *   <li>{@code TimeIterator} - Controls time-stepping loop</li>
 * </ul>
 * 
 * <h2>Solver Types</h2>
 * <ul>
 *   <li><strong>BE (Backward Euler)</strong> - First-order implicit, unconditionally stable</li>
 *   <li><strong>TRZ (Trapezoidal)</strong> - Second-order implicit, can oscillate</li>
 *   <li><strong>GS (Gear-Shichman)</strong> - Variable order, for stiff systems</li>
 * </ul>
 * 
 * <h2>Coverage: 97%</h2>
 * <p>This package is fully tested and ready for extraction.</p>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.circuit.simulation;

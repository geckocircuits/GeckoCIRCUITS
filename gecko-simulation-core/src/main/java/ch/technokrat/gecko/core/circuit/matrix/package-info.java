/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * MNA (Modified Nodal Analysis) matrix stampers for circuit elements.
 * 
 * <p>Each circuit element type (resistor, capacitor, inductor, sources, switches)
 * has a corresponding stamper class that implements {@code IMatrixStamper}.
 * The stamper encapsulates the mathematical model for how that element
 * contributes to the system matrix A and source vector b in Ax=b.</p>
 * 
 * <h2>Stamper Classes (from legacy circuit.matrix package)</h2>
 * <ul>
 *   <li>{@code ResistorStamper} - Simple conductance stamping</li>
 *   <li>{@code CapacitorBEStamper} - Capacitor with Backward Euler integration</li>
 *   <li>{@code CapacitorTRZStamper} - Capacitor with Trapezoidal integration</li>
 *   <li>{@code InductorBEStamper} - Inductor with Backward Euler integration</li>
 *   <li>{@code InductorTRZStamper} - Inductor with Trapezoidal integration</li>
 *   <li>{@code VoltageSourceStamper} - Ideal voltage source</li>
 *   <li>{@code CurrentSourceStamper} - Ideal current source</li>
 *   <li>{@code SwitchStamper} - Ideal switch (on/off resistance)</li>
 * </ul>
 * 
 * <h2>Coverage Target: 85%</h2>
 * <p>Current: 77% - Need additional edge case tests</p>
 * 
 * @since 2.0
 * @see ch.technokrat.gecko.core.api.IMatrixStamper
 */
package ch.technokrat.gecko.core.circuit.matrix;

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

/**
 * GeckoCIRCUITS Simulation Core - GUI-Free Domain Logic
 * 
 * <h2>Overview</h2>
 * This package contains the pure simulation engine extracted from the 
 * GeckoCIRCUITS desktop application. It is designed to be:
 * <ul>
 *   <li><strong>GUI-free</strong> - No Swing, AWT, or JavaFX dependencies</li>
 *   <li><strong>Headless-compatible</strong> - Runs on servers without displays</li>
 *   <li><strong>API-ready</strong> - Clean interfaces for REST/RMI access</li>
 *   <li><strong>GraalVM-compatible</strong> - Can be compiled to native image</li>
 * </ul>
 * 
 * <h2>Package Structure</h2>
 * <pre>
 * ch.technokrat.gecko.core
 * ├── api/          - Public simulation interfaces (IMatrixStamper, etc.)
 * ├── circuit/      - Circuit representation and MNA matrix building
 * │   ├── matrix/   - Matrix stampers for circuit elements
 * │   ├── netlist/  - Netlist parsing and construction
 * │   └── simulation/ - Simulation engine (solver context, etc.)
 * ├── control/      - Control block calculators
 * │   └── calculators/ - Signal processing (RMS, THD, FFT, etc.)
 * ├── math/         - Mathematical utilities
 * │   ├── Matrix.java - Dense matrix with LU decomposition
 * │   ├── NComplex.java - Complex number arithmetic
 * │   └── GlobalMatrixMath.java - Matrix utilities
 * └── data/         - Data containers and structures
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a simple RC circuit simulation
 * SolverContext ctx = new SolverContext(dt, SolverContext.SOLVER_BE);
 * StamperRegistry registry = StamperRegistry.createDefault();
 * 
 * // Get stampers
 * IMatrixStamper resistor = registry.getStamper(CircuitTyp.LK_R);
 * IMatrixStamper capacitor = registry.getStamper(CircuitTyp.LK_C);
 * 
 * // Build MNA matrix
 * double[][] A = new double[2][2];
 * resistor.stampMatrixA(A, 0, 1, -1, new double[]{1000.0}, dt);
 * capacitor.stampMatrixA(A, 1, 0, -1, new double[]{1e-6}, dt);
 * 
 * // Solve and iterate
 * Matrix solver = new Matrix(A);
 * solver.luDecomposition();
 * double[] x = solver.luSolve(b);
 * }</pre>
 * 
 * <h2>Prohibited Dependencies</h2>
 * The following imports are <strong>FORBIDDEN</strong> in this module:
 * <ul>
 *   <li>{@code java.awt.*}</li>
 *   <li>{@code javax.swing.*}</li>
 *   <li>{@code java.applet.*}</li>
 *   <li>Any GUI framework classes</li>
 * </ul>
 * 
 * Build will fail if GUI dependencies are detected.
 * 
 * @since 2.0
 * @see ch.technokrat.gecko.core.api.IMatrixStamper
 * @see ch.technokrat.gecko.core.circuit.simulation.SolverContext
 */
package ch.technokrat.gecko.core;

/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Mathematical utilities for circuit simulation.
 * 
 * <p>This package provides core mathematical operations needed for
 * nodal analysis and signal processing, including matrix operations,
 * complex number arithmetic, and numerical algorithms.</p>
 * 
 * <h2>Classes (from legacy geckocircuits.math package)</h2>
 * <ul>
 *   <li>{@code Matrix} - Dense matrix with LU decomposition and solve</li>
 *   <li>{@code NComplex} - Complex number class with arithmetic operations</li>
 *   <li>{@code GlobalMatrixMath} - Static utilities for matrix operations</li>
 *   <li>{@code SparseMatrix} - Compressed sparse row format matrix</li>
 *   <li>{@code FFT} - Fast Fourier Transform implementation</li>
 * </ul>
 * 
 * <h2>Coverage Target: 85%</h2>
 * <p>Current: 55% - Priority for additional tests</p>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.math;

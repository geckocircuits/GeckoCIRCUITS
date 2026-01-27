/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Control system block calculators - signal processing algorithms.
 * 
 * <p>This package contains the pure calculation logic for control system blocks,
 * separated from the GUI display and editing classes that remain in the
 * desktop module.</p>
 * 
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code calculators/} - 71 GUI-free calculator classes</li>
 * </ul>
 * 
 * <h2>Calculator Types</h2>
 * <ul>
 *   <li><strong>Signal Generation</strong> - Sine, square, triangle, noise</li>
 *   <li><strong>Signal Processing</strong> - RMS, THD, FFT, harmonics</li>
 *   <li><strong>Mathematical</strong> - Add, multiply, integrate, differentiate</li>
 *   <li><strong>Control</strong> - PID, transfer functions, state-space</li>
 *   <li><strong>Logic</strong> - AND, OR, comparators, flip-flops</li>
 * </ul>
 * 
 * <h2>Coverage Target: 85%</h2>
 * <p>Current: 41% - Major opportunity for improvement</p>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.control;

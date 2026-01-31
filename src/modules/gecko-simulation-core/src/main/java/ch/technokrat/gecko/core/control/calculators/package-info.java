/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Pure calculator implementations for control blocks - GUI-FREE.
 * 
 * <p>These 71 classes contain the actual calculation algorithms for control
 * system blocks. They have NO GUI dependencies and can run headless.</p>
 * 
 * <h2>GUI-Free Calculators (examples)</h2>
 * <ul>
 *   <li>{@code AbstractControlCalculatable} - Base class for all calculators</li>
 *   <li>{@code SineCalculator} - Sine wave generation</li>
 *   <li>{@code RMSCalculator} - Root Mean Square calculation</li>
 *   <li>{@code THDCalculator} - Total Harmonic Distortion</li>
 *   <li>{@code FFTCalculator} - Fast Fourier Transform</li>
 *   <li>{@code PIDCalculator} - PID controller</li>
 *   <li>{@code IntegratorCalculator} - Numerical integration</li>
 *   <li>{@code ComparatorCalculator} - Signal comparison</li>
 * </ul>
 * 
 * <h2>Excluded (remain in desktop)</h2>
 * <ul>
 *   <li>{@code DEMUXCalculator} - Depends on ReglerDemux GUI class</li>
 *   <li>{@code SpaceVectorCalculator} - Depends on SpaceVectorDisplay GUI</li>
 * </ul>
 * 
 * <h2>Coverage Target: 85%</h2>
 * <p>Current: 41% (4,241 of 10,117 instructions)</p>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.control.calculators;

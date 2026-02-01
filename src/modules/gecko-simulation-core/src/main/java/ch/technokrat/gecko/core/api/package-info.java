/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Public API interfaces for GeckoCIRCUITS simulation core.
 * 
 * <p>This package defines the stable public interfaces that external code
 * (REST API, RMI server, CLI tools) should use to interact with the
 * simulation engine.</p>
 * 
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@code IMatrixStamper} - Strategy for stamping circuit elements into MNA matrix</li>
 *   <li>{@code ISimulationEngine} - High-level simulation control</li>
 *   <li>{@code ICircuitBuilder} - Fluent API for constructing circuits</li>
 * </ul>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.api;

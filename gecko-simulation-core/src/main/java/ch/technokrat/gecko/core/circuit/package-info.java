/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Circuit representation and MNA (Modified Nodal Analysis) matrix construction.
 * 
 * <p>This package contains the core circuit domain model and the numerical
 * foundation for SPICE-like circuit simulation using nodal analysis.</p>
 * 
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code matrix/} - MNA matrix stampers for each circuit element type</li>
 *   <li>{@code netlist/} - Netlist parsing and circuit graph construction</li>
 *   <li>{@code simulation/} - Solver context and integration methods</li>
 * </ul>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.circuit;

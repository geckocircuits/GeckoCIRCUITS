/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */

/**
 * Netlist parsing and circuit graph construction.
 * 
 * <p>This package handles the conversion from circuit description formats
 * (IPES files, XML, SPICE netlist) into the internal circuit graph
 * representation used by the simulation engine.</p>
 * 
 * <h2>Classes (from legacy circuit.netlist package)</h2>
 * <ul>
 *   <li>{@code NetListBuilder} - Constructs netlist from circuit components</li>
 *   <li>{@code NetListLK} - Main netlist representation</li>
 *   <li>{@code NodeMapping} - Maps component terminals to matrix indices</li>
 *   <li>{@code CircuitGraphValidator} - Validates circuit connectivity</li>
 * </ul>
 * 
 * <h2>Coverage: 99%</h2>
 * <p>This package is fully tested and ready for extraction.</p>
 * 
 * @since 2.0
 */
package ch.technokrat.gecko.core.circuit.netlist;

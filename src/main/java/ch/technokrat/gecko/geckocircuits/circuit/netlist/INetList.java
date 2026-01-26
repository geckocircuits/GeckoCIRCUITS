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
package ch.technokrat.gecko.geckocircuits.circuit.netlist;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;

/**
 * Interface for netlist representation in circuit simulation.
 * A netlist contains information about circuit components, their types,
 * node connections, and parameters needed for MNA (Modified Nodal Analysis).
 * 
 * <p>Key concepts:
 * <ul>
 *   <li><b>Nodes</b>: Connection points in the circuit, numbered from 0 (ground reference)</li>
 *   <li><b>Elements</b>: Circuit components (resistors, capacitors, sources, etc.)</li>
 *   <li><b>Voltage Sources</b>: Components that require special handling in MNA</li>
 * </ul>
 * 
 * @author Extracted from NetListLK for better testability
 * @since Sprint 2 - Circuit Refactoring
 */
public interface INetList {
    
    /**
     * Gets the maximum node index in the netlist.
     * Node 0 is typically the ground reference.
     * 
     * @return the highest node number used in the circuit
     */
    int getNodeMax();
    
    /**
     * Gets the maximum voltage source number.
     * Voltage sources (and LKOP2 elements) are numbered sequentially starting from 1.
     * 
     * @return the highest voltage source number
     */
    int getVoltageSourceMax();
    
    /**
     * Gets the total number of elements in the netlist.
     * 
     * @return element count
     */
    int getElementCount();
    
    /**
     * Gets the circuit type for an element at the given index.
     * 
     * @param index element index (0-based)
     * @return the CircuitTyp enum value
     */
    CircuitTyp getType(int index);
    
    /**
     * Gets the X node (positive/input terminal) for an element.
     * 
     * @param index element index (0-based)
     * @return node number for the X terminal
     */
    int getNodeX(int index);
    
    /**
     * Gets the Y node (negative/output terminal) for an element.
     * 
     * @param index element index (0-based)
     * @return node number for the Y terminal
     */
    int getNodeY(int index);
    
    /**
     * Gets the parameters for an element at the given index.
     * Parameter layout depends on element type:
     * <ul>
     *   <li>[0] = rD (dynamic resistance)</li>
     *   <li>[1] = uForward (forward voltage)</li>
     *   <li>[2] = rOn (ON resistance)</li>
     *   <li>[3] = rOff (OFF resistance)</li>
     *   <li>[8] = gate signal (for switches)</li>
     *   <li>[9] = recovery time (for thyristors)</li>
     * </ul>
     * 
     * @param index element index (0-based)
     * @return parameter array for the element
     */
    double[] getParameter(int index);
    
    /**
     * Gets the voltage source number for an element.
     * Returns -1 if the element is not a voltage source type.
     * 
     * @param index element index (0-based)
     * @return voltage source number (1-based) or -1
     */
    int getVoltageSourceNumber(int index);
    
    /**
     * Gets the current simulation time.
     * 
     * @return simulation time in seconds
     */
    double getSimulationTime();
}

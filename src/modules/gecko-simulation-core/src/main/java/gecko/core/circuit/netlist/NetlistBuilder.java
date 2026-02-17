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
package gecko.core.circuit.netlist;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;

/**
 * Factory class for building CircuitNetlist from various sources.
 *
 * <p>Bridges the gap between circuit file parsing (CircuitModel) and simulation
 * (CircuitNetlist/INetList). Provides static factory methods to construct netlists
 * with proper initialization of MNA (Modified Nodal Analysis) data structures.</p>
 *
 * <p>This factory is used by the headless simulation engine to convert parsed circuit
 * models into executable netlist representations. Current implementation provides:
 * <ul>
 *   <li>Empty netlist creation for testing matrix solvers</li>
 *   <li>Netlist construction from CircuitModel with dimension estimation</li>
 * </ul>
 *
 * @author Phase 3 - Circuit Extraction Refactoring
 * @since v2.18.0
 */
public class NetlistBuilder {

    private NetlistBuilder() {
        // Utility class - not instantiable
    }

    /**
     * Build a simple CircuitNetlist for testing with given dimensions.
     *
     * <p>Creates an empty netlist with the specified size and initializes all arrays
     * with zero/empty values. This is useful for unit testing the matrix solver without
     * needing actual circuit components.</p>
     *
     * <p>The netlist will have:
     * <ul>
     *   <li>nodeCount nodes (numbered 0 to nodeCount-1, plus ground)</li>
     *   <li>voltageSourceCount voltage sources (numbered 1 to voltageSourceCount)</li>
     *   <li>elementCount passive components</li>
     *   <li>All components initialized as RESISTOR type with zero resistance</li>
     *   <li>All nodes connected to ground (node 0)</li>
     * </ul>
     *
     * @param nodeCount number of nodes in circuit (excluding ground, typically >= 1)
     * @param voltageSourceCount number of independent voltage sources (typically >= 0)
     * @param elementCount number of passive components (typically >= 0)
     * @return configured CircuitNetlist with initialized but empty circuit data
     *
     * @throws IllegalArgumentException if any count is negative
     */
    public static CircuitNetlist buildEmpty(
            int nodeCount, int voltageSourceCount, int elementCount) {

        // Validate parameters
        if (nodeCount < 0) {
            throw new IllegalArgumentException("nodeCount must be non-negative, got: " + nodeCount);
        }
        if (voltageSourceCount < 0) {
            throw new IllegalArgumentException("voltageSourceCount must be non-negative, got: " + voltageSourceCount);
        }
        if (elementCount < 0) {
            throw new IllegalArgumentException("elementCount must be non-negative, got: " + elementCount);
        }

        // Create component arrays with default values
        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] voltageSourceNr = new int[elementCount];
        double[][] params = new double[elementCount][];

        // Initialize with defaults
        for (int i = 0; i < elementCount; i++) {
            types[i] = CircuitTypCore.LK_R;  // Default to resistor
            nodeX[i] = 0;                     // Connected to ground
            nodeY[i] = 0;                     // Connected to ground
            voltageSourceNr[i] = -1;          // Not a voltage source
            params[i] = new double[1];        // Single parameter array
            params[i][0] = 0.0;               // Zero resistance
        }

        // Create netlist and initialize
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params,
                            nodeCount > 0 ? nodeCount - 1 : 0,  // maxNodeIndex
                            voltageSourceCount,                   // maxVoltageSourceIndex
                            elementCount);

        return netlist;
    }

    /**
     * Build a CircuitNetlist from a parsed CircuitModel.
     *
     * <p>This is the main entry point for converting circuit file data into a netlist
     * suitable for simulation. The CircuitModel contains component lists and connection
     * information extracted from .ipes circuit files.</p>
     *
     * <p>Current implementation:
     * <ul>
     *   <li>Estimates node and voltage source counts from component counts</li>
     *   <li>Creates empty netlist with appropriate dimensions</li>
     *   <li>Returns null safely if model is null</li>
     * </ul>
     *
     * <p>Full topology extraction (node assignments, connection resolution) will be
     * implemented when component parsing is complete in the CircuitFileParser.</p>
     *
     * @param model circuit model from CircuitFileParser, may be null
     * @return configured CircuitNetlist, or empty netlist if model is null
     *
     * @see CircuitModel#getTotalComponentCount()
     * @see CircuitModel#getCircuitComponents()
     * @see CircuitModel#getConnections()
     */
    public static CircuitNetlist buildFromCircuitModel(CircuitModel model) {
        if (model == null) {
            return buildEmpty(0, 0, 0);
        }

        // Get component counts from model
        int totalComponents = model.getTotalComponentCount();

        // Dimension estimation:
        // - Nodes: assume ~1 node per 2 components (conservative estimate)
        // - Voltage sources: estimate ~1 per 5 components
        // - Elements: use actual component count
        int estimatedNodeCount = Math.max(1, totalComponents / 2 + 1);
        int estimatedVoltageSourceCount = Math.max(0, totalComponents / 5);

        return buildEmpty(
            estimatedNodeCount,
            estimatedVoltageSourceCount,
            totalComponents
        );
    }
}

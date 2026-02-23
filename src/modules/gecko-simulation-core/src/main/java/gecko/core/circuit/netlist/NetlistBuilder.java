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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 *   <li>Netlist construction from CircuitModel with topology extraction via label matching</li>
 *   <li>Graceful fallback to dimension estimation when labels are unavailable</li>
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
     * <p>Implementation:
     * <ul>
     *   <li>Collects all components from circuit, control, and thermal lists</li>
     *   <li>Attempts topology extraction via label matching if labels are present</li>
     *   <li>Falls back to dimension estimation if labels are unavailable</li>
     *   <li>Assigns voltage source numbers to voltage sources and coupable inductors</li>
     *   <li>Extracts component parameters from parsed model</li>
     *   <li>Initializes netlist with proper node and voltage source counts</li>
     * </ul>
     *
     * @param model circuit model from CircuitFileParser, may be null
     * @return configured CircuitNetlist with extracted or estimated topology, or empty netlist if model is null
     *
     * @see CircuitModel#getCircuitComponents()
     * @see CircuitModel#getControlComponents()
     * @see CircuitModel#getThermalComponents()
     * @see CircuitModel#getTotalComponentCount()
     */
    public static CircuitNetlist buildFromCircuitModel(CircuitModel model) {
        if (model == null) {
            return buildEmpty(0, 0, 0);
        }

        // Collect all components from all lists
        List<CircuitModel.ComponentData> allComponents = new ArrayList<>();
        allComponents.addAll(model.getCircuitComponents());
        allComponents.addAll(model.getControlComponents());
        allComponents.addAll(model.getThermalComponents());

        if (allComponents.isEmpty()) {
            return buildEmpty(0, 0, 0);
        }

        // Check if any component has terminal labels (indicates parsed from file)
        boolean hasLabels = allComponents.stream()
                .anyMatch(comp -> comp.getTerminalXLabels().length > 0 || comp.getTerminalYLabels().length > 0);

        if (hasLabels) {
            // Use label-based topology extraction
            return buildFromComponentsWithLabels(allComponents);
        } else {
            // Fall back to dimension estimation for backward compatibility
            return buildWithEstimatedDimensions(allComponents);
        }
    }

    /**
     * Build netlist from components that have terminal labels (from .ipes file parsing).
     *
     * <p>Extracts topology through label matching:
     * <ul>
     *   <li>Builds a label-to-node mapping</li>
     *   <li>Resolves component connectivity through label matching</li>
     *   <li>Assigns voltage source numbers</li>
     * </ul>
     */
    private static CircuitNetlist buildFromComponentsWithLabels(List<CircuitModel.ComponentData> components) {
        // Step 1: Build node label → index map
        // Ground reference: label "0" or "" → node 0
        Map<String, Integer> labelToNode = new LinkedHashMap<>();
        labelToNode.put("0", 0);
        labelToNode.put("", 0);  // Empty label also maps to ground
        int nextNode = 1;

        for (CircuitModel.ComponentData comp : components) {
            for (String label : comp.getTerminalXLabels()) {
                if (!labelToNode.containsKey(label)) {
                    labelToNode.put(label, nextNode++);
                }
            }
            for (String label : comp.getTerminalYLabels()) {
                if (!labelToNode.containsKey(label)) {
                    labelToNode.put(label, nextNode++);
                }
            }
        }

        int nodeCount = nextNode;
        int elementCount = components.size();

        // Step 2: Assign voltage source numbers
        int voltageSourceCount = 0;
        int[] voltageSourceNumbers = new int[elementCount];
        CircuitTypCore[] types = new CircuitTypCore[elementCount];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);
            CircuitTypCore typ;
            try {
                typ = CircuitTypCore.fromTypeNumber(comp.getType());
            } catch (IllegalArgumentException e) {
                // Unknown type - default to resistor
                typ = CircuitTypCore.LK_R;
            }
            types[i] = typ;

            if (typ == CircuitTypCore.LK_U || typ == CircuitTypCore.LK_LKOP2) {
                voltageSourceNumbers[i] = ++voltageSourceCount;
            } else {
                voltageSourceNumbers[i] = -1;
            }
        }

        // Step 3: Build node arrays and parameter arrays
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        double[][] params = new double[elementCount][10]; // up to 10 params per component

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);

            // Assign X terminal (positive/start)
            String[] xLabels = comp.getTerminalXLabels();
            nodeX[i] = xLabels.length > 0 ? labelToNode.getOrDefault(xLabels[0], 0) : 0;

            // Assign Y terminal (negative/end)
            String[] yLabels = comp.getTerminalYLabels();
            nodeY[i] = yLabels.length > 0 ? labelToNode.getOrDefault(yLabels[0], 0) : 0;

            // Extract numeric parameters
            for (int p = 0; p < 10; p++) {
                Object val = comp.getParameters().get("param" + p);
                params[i][p] = (val instanceof Number) ? ((Number) val).doubleValue() : 0.0;
            }
            // Ensure primary value is set (safety)
            if (params[i][0] == 0.0) {
                Object primary = comp.getParameters().get(resolveParameterKey(comp.getType()));
                if (primary instanceof Number) {
                    params[i][0] = ((Number) primary).doubleValue();
                }
            }
        }

        // Step 4: Initialize and return netlist
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                nodeCount - 1, voltageSourceCount, elementCount);
        return netlist;
    }

    /**
     * Build netlist with estimated dimensions (backward compatible mode).
     *
     * <p>Used when components don't have terminal labels (created programmatically).
     * Estimates node and voltage source counts based on component count.</p>
     */
    private static CircuitNetlist buildWithEstimatedDimensions(List<CircuitModel.ComponentData> components) {
        int totalComponents = components.size();

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

    /**
     * Returns the semantic parameter key for the primary value of a component type.
     */
    private static String resolveParameterKey(int type) {
        return switch (type) {
            case 1 -> "resistance";
            case 2 -> "inductance";
            case 3 -> "capacitance";
            case 4 -> "amplitude";
            case 5 -> "amplitude";
            default -> "value";
        };
    }
}

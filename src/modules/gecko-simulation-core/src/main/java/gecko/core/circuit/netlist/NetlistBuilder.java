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
import gecko.core.circuit.matrix.StamperRegistry;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
 *   <li>Coordinate-wire resolution when saved terminal labels are missing</li>
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
     *   <li>Collects electrical components from the circuit list</li>
     *   <li>Resolves terminal labels and validates connectivity to the reference node</li>
     *   <li>Rejects missing or ambiguous terminal topology</li>
     *   <li>Assigns voltage source numbers to voltage sources and coupable inductors</li>
     *   <li>Extracts component parameters from parsed model</li>
     *   <li>Initializes netlist with proper node and voltage source counts</li>
     * </ul>
     *
     * @param model circuit model from CircuitFileParser, may be null
     * @return netlist with validated topology, or an empty netlist for a null or empty model
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

        List<CircuitModel.ComponentData> allComponents = new ArrayList<>();
        List<CircuitModel.ComponentData> couplingComponents = new ArrayList<>();
        CircuitTopology topology = CircuitTopology.resolve(model);
        topology.validatePhysicalConnections();
        Set<CircuitTopology.ComponentIdentity> active = new HashSet<>(topology.activeComponents());
        Set<String> names = new HashSet<>();
        for (CircuitModel.ComponentData component : model.getCircuitComponents()) {
            if (component == null) {
                throw new IllegalArgumentException("Circuit component cannot be null");
            }
            if (!active.contains(topology.identity(component))) continue;
            if (component.getName() != null && !component.getName().isEmpty()
                    && !names.add(component.getName())) {
                throw new IllegalArgumentException("Duplicate component name '" + component.getName() + "'");
            }
            if (component.getType() == CircuitTypCore.LK_M.getTypeNumber()) {
                couplingComponents.add(component);
            } else {
                allComponents.add(component);
            }
        }
        if (allComponents.isEmpty()) {
            if (!couplingComponents.isEmpty()) {
                throw new IllegalArgumentException("Coupling '" + couplingComponents.get(0).getName()
                        + "' has no windings");
            }
            return buildEmpty(0, 0, 0);
        }

        StamperRegistry registry = StamperRegistry.createDefault();
        for (CircuitModel.ComponentData component : allComponents) {
            CircuitTypCore type;
            try {
                type = CircuitTypCore.fromTypeNumber(component.getType());
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Unsupported electrical component '" + component.getName()
                        + "' with type " + component.getType(), error);
            }
            if (!registry.hasStamper(type)) {
                throw new IllegalArgumentException("Unsupported electrical component '" + component.getName()
                        + "' with type " + component.getType() + " (" + type + ")");
            }
        }

        CircuitTopology.Group[][] terminalGroups = new CircuitTopology.Group[allComponents.size()][2];
        boolean[][] referenceNodes = new boolean[allComponents.size()][2];
        boolean hasElectricalWires = topology.hasConnections("LK") || topology.hasConnections("RELUCTANCE")
                || topology.hasConnections("THERMAL");
        for (int i = 0; i < allComponents.size(); i++) {
            var component = allComponents.get(i);
            for (int side = 0; side < 2; side++) {
                terminalLabel(component, side == 0);
                var group = topology.group(component, side == 0 ? CircuitTopology.Side.INPUT : CircuitTopology.Side.OUTPUT, 0);
                if (group == null) throw new IllegalArgumentException("Component '" + component.getName() + "' has no terminal topology");
                terminalGroups[i][side] = group;
                referenceNodes[i][side] = group.canonicalLabel().equals("0") || group.canonicalLabel().equalsIgnoreCase("gnd")
                        || group.canonicalLabel().isEmpty() && !topology.hasConnections(group.domain()) && component.getTerminalData().isEmpty();
            }
        }

        // Keep legacy label-only models working without inferring geometry at default positions.
        boolean hasLabels = allComponents.stream()
                .anyMatch(comp -> comp.getTerminalXLabels().length > 0 || comp.getTerminalYLabels().length > 0);

        if (hasLabels || hasElectricalWires) {
            // Use label-based topology extraction
            boolean legacyLabelOnly = !hasElectricalWires
                    && allComponents.stream().allMatch(component -> component.getTerminalData().isEmpty())
                    && allComponents.stream().map(component -> component.getSavedBlock() == null
                            ? 0L : component.getSavedBlock().parentSheetIdentifier()).distinct().count() == 1;
            CircuitNetlist netlist = buildFromComponentsWithGroups(allComponents, terminalGroups, referenceNodes,
                    legacyLabelOnly, topology.connectionGroupOrder());
            registerMagneticCouplings(netlist, allComponents, couplingComponents);
            return netlist;
        } else {
            throw new IllegalArgumentException("Component '" + allComponents.get(0).getName()
                    + "' has no terminal topology. Use buildEmpty for explicit preallocation.");
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
    private static CircuitNetlist buildFromComponentsWithGroups(List<CircuitModel.ComponentData> components,
            CircuitTopology.Group[][] terminalGroups, boolean[][] referenceNodes, boolean legacyLabelOnly,
            List<CircuitTopology.Group> connectionOrder) {
        // Step 1: Build node label → index map
        // Ground reference: label "0" or "" → node 0
        // Legacy label-only models share named numerical nodes across physical component types.
        // Explicit terminal domains and geometric topology retain their typed groups.
        Map<Object, Integer> groupToNode = new LinkedHashMap<>();
        int nextNode = 1;

        for (int component = 0; component < terminalGroups.length; component++) {
            for (int side = 0; side < 2; side++) {
                var group = terminalGroups[component][side];
                Object key = nodeKey(group, legacyLabelOnly);
                if (referenceNodes[component][side]) groupToNode.put(key, 0);
                else if (!groupToNode.containsKey(key)) {
                    groupToNode.put(key, nextNode++);
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
            CircuitTypCore typ = CircuitTypCore.fromTypeNumber(comp.getType());
            types[i] = typ;

            if (typ == CircuitTypCore.LK_U || typ == CircuitTypCore.LK_LKOP2
                    || typ == CircuitTypCore.TH_TEMP || typ == CircuitTypCore.REL_MMF) {
                voltageSourceNumbers[i] = ++voltageSourceCount;
            } else {
                voltageSourceNumbers[i] = -1;
            }
        }

        // Step 3: Build node arrays and parameter arrays
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        double[][] params = new double[elementCount][];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);

            nodeX[i] = groupToNode.get(nodeKey(terminalGroups[i][0], legacyLabelOnly));
            nodeY[i] = groupToNode.get(nodeKey(terminalGroups[i][1], legacyLabelOnly));
            if (nodeX[i] == nodeY[i] && (types[i] == CircuitTypCore.LK_U
                    || types[i] == CircuitTypCore.TH_TEMP || types[i] == CircuitTypCore.REL_MMF)) {
                throw new IllegalArgumentException("Voltage source '" + comp.getName()
                        + "' has both terminals on the same node");
            }

            // Extract numeric parameters — use actual parsed count (some components need 11+)
            int paramCount = 0;
            for (String key : comp.getParameters().keySet()) {
                if (key.startsWith("param")) {
                    try {
                        int idx = Integer.parseInt(key.substring(5));
                        paramCount = Math.max(paramCount, idx + 1);
                    } catch (NumberFormatException ignored) {}
                }
            }
            // Minimum 13 slots to accommodate all component parameter indices
            // (capacitors use [10], switches use [8]/[12])
            paramCount = Math.max(paramCount, 13);
            params[i] = new double[paramCount];

            for (int p = 0; p < paramCount; p++) {
                Object val = comp.getParameters().get("param" + p);
                params[i][p] = (val instanceof Number) ? ((Number) val).doubleValue() : 0.0;
            }
            // Ensure primary value is set (safety)
            if (!comp.getParameters().containsKey("param0")) {
                Object primary = comp.getParameters().get(resolveParameterKey(comp.getType()));
                if (primary instanceof Number) {
                    params[i][0] = ((Number) primary).doubleValue();
                }
            }
        }

        for (int i = 0; i < elementCount; i++) {
            if (types[i] == CircuitTypCore.LK_LKOP2
                    && (!Double.isFinite(params[i][0]) || params[i][0] <= 0)) {
                throw new IllegalArgumentException("Winding '" + components.get(i).getName()
                        + "' requires finite positive inductance");
            }
        }

        // Step 4: Build component name-to-index map
        Map<String, Integer> nameToIndex = new LinkedHashMap<>();
        for (int i = 0; i < elementCount; i++) {
            String name = components.get(i).getName();
            if (name != null && !name.isEmpty()) {
                nameToIndex.put(name, i);
            }
        }

        boolean wireReference = !legacyLabelOnly && !connectionOrder.isEmpty();
        // Wired models use the desktop reference (ADR-0011): the first wire potential is the datum and
        // MatrixSolver pins every other isolated group, so they need no ground node. Label-only models do.
        if (!wireReference) validateGroundConnectivity(components, nodeX, nodeY);
        else rejectCurrentSourceBridges(components, types, nodeX, nodeY, nodeCount);
        int[] order = new int[nodeCount];
        java.util.Arrays.setAll(order, node -> node);
        if (wireReference) {
            var orderedNodes = new java.util.LinkedHashSet<Integer>();
            for (var group : connectionOrder) {
                Integer node = groupToNode.get(nodeKey(group, false));
                if (node != null) orderedNodes.add(node);
            }
            for (int node = 0; node < nodeCount; node++) orderedNodes.add(node);
            int next = 0;
            for (int node : orderedNodes) order[node] = next++;
        }

        // Step 5: Initialize and return netlist
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                nodeCount - 1, voltageSourceCount, elementCount);
        netlist.setComponentNameToIndex(nameToIndex);
        if (wireReference) netlist.setNumericalNodeOrder(order);
        return netlist;
    }

    private static Object nodeKey(CircuitTopology.Group group, boolean legacyLabelOnly) {
        return legacyLabelOnly ? group.canonicalLabel() : group;
    }

    private static void registerMagneticCouplings(CircuitNetlist netlist,
            List<CircuitModel.ComponentData> components, List<CircuitModel.ComponentData> couplings) {
        if (couplings.isEmpty()) {
            return;
        }
        Map<Long, Integer> identifiers = new LinkedHashMap<>();
        for (int i = 0; i < components.size(); i++) {
            Object identifier = components.get(i).getParameters().get("uniqueObjectIdentifier");
            if (identifier instanceof Number number && number.longValue() != 0
                    && identifiers.putIfAbsent(number.longValue(), i) != null) {
                throw new IllegalArgumentException("Duplicate component identifier " + identifier);
            }
        }
        for (CircuitModel.ComponentData coupling : couplings) {
            try {
                int[] windings = new int[2];
                for (int end = 0; end < 2; end++) {
                    Object identifier = coupling.getParameters().get("coupledReferenceID" + end);
                    if (identifier instanceof Number number) {
                        windings[end] = identifiers.getOrDefault(number.longValue(), -1);
                    } else {
                        String[] references = coupling.getParameterStrings();
                        windings[end] = end < references.length
                                ? netlist.getElementIndexByName(references[end]) : -1;
                    }
                    if (windings[end] < 0) {
                        throw new IllegalArgumentException("Missing winding reference " + (end + 1));
                    }
                }
                Object coefficient = coupling.getParameters().get("param0");
                if (!(coefficient instanceof Number number)) {
                    throw new IllegalArgumentException("Missing coupling coefficient");
                }
                netlist.registerMutualCoupling(windings[0], windings[1], number.doubleValue(),
                        netlist.getParameter(windings[0])[0], netlist.getParameter(windings[1])[0]);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Coupling '" + coupling.getName() + "': " + error.getMessage(), error);
            }
        }
    }

    private static String terminalLabel(CircuitModel.ComponentData component, boolean xTerminal) {
        String[] labels = xTerminal ? component.getTerminalXLabels() : component.getTerminalYLabels();
        if (labels.length > 1 || (labels.length == 1 && labels[0] == null)) {
            throw new IllegalArgumentException("Component '" + component.getName()
                    + "' requires one valid label per electrical terminal");
        }
        // Parsed legacy files represent the implicit reference node with an absent label.
        return labels.length == 0 ? "" : normalizeLabel(labels[0]);
    }

    static String normalizeLabel(String label) {
        String trimmed = label.trim();
        return trimmed.equalsIgnoreCase("gnd") ? "0" : trimmed;
    }

    private static void validateGroundConnectivity(List<CircuitModel.ComponentData> components,
                                                    int[] nodeX, int[] nodeY) {
        Map<Integer, List<Integer>> adjacency = new NodeIndexer(nodeX, nodeY).buildNodeToElementsMap();
        Set<Integer> connected = new HashSet<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        connected.add(0);
        pending.add(0);
        while (!pending.isEmpty()) {
            int node = pending.removeFirst();
            for (int element : adjacency.getOrDefault(node, List.of())) {
                CircuitTypCore type = CircuitTypCore.fromTypeNumber(components.get(element).getType());
                if (type == CircuitTypCore.LK_I || type == CircuitTypCore.TH_FLOW) {
                    continue; // An ideal current source adds no voltage constraint or conductance.
                }
                int other = nodeX[element] == node ? nodeY[element] : nodeX[element];
                if (connected.add(other)) {
                    pending.addLast(other);
                }
            }
        }
        for (int element = 0; element < components.size(); element++) {
            if (!connected.contains(nodeX[element]) || !connected.contains(nodeY[element])) {
                throw new TopologyValidationException("UNCONNECTED_GROUND", components.get(element).getName(), "Component '" + components.get(element).getName()
                        + "' is not connected to ground (node 0)");
            }
        }
    }

    /**
     * MatrixSolver pins every group that current sources do not join (ADR-0012). A current source between two such
     * groups would drive its current through a pin, so reject it like the desktop, whose matrix is singular for it.
     */
    private static void rejectCurrentSourceBridges(List<CircuitModel.ComponentData> components,
            CircuitTypCore[] types, int[] nodeX, int[] nodeY, int nodeCount) {
        int[] parent = new int[nodeCount];
        for (int node = 0; node < nodeCount; node++) parent[node] = node;
        for (int element = 0; element < types.length; element++) {
            if (types[element] == CircuitTypCore.LK_I || types[element] == CircuitTypCore.TH_FLOW) continue;
            parent[root(parent, nodeX[element])] = root(parent, nodeY[element]);
        }
        for (int element = 0; element < types.length; element++) {
            if ((types[element] == CircuitTypCore.LK_I || types[element] == CircuitTypCore.TH_FLOW)
                    && root(parent, nodeX[element]) != root(parent, nodeY[element])) {
                String name = components.get(element).getName();
                throw new TopologyValidationException("UNCONNECTED_GROUND", name, "Current source '" + name
                        + "' is the only connection between two electrical groups; its current would flow through"
                        + " the reference stamp");
            }
        }
    }

    private static int root(int[] parent, int node) {
        while (parent[node] != node) node = parent[node] = parent[parent[node]];
        return node;
    }

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

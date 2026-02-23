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
import java.util.*;

/**
 * Core netlist representation for circuit simulation.
 * Extracted from NetListLK for headless simulation without GUI dependencies.
 *
 * <p>A CircuitNetlist contains all information needed for MNA (Modified Nodal Analysis):
 * <ul>
 *   <li>Component types and connectivity (nodes)</li>
 *   <li>Component parameters (resistance, inductance, capacitance, etc.)</li>
 *   <li>Voltage source numbering for the extended MNA system</li>
 *   <li>Magnetic coupling information between inductors</li>
 * </ul>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Store component types, nodes, and parameters</li>
 *   <li>Track node and voltage source numbering</li>
 *   <li>Manage mutual inductance couplings via MutualCouplingRegistry</li>
 *   <li>Provide access to netlist data for matrix stamping</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * CircuitNetlist netlist = new CircuitNetlist();
 * netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params,
 *                     maxNodeIndex, maxVoltageSourceIndex, elementCount);
 * netlist.registerMutualCoupling(L1_idx, L2_idx, k, L1_value, L2_value);
 * </pre>
 *
 * @author Extracted from NetListLK for circuit refactoring
 * @since v2.18.0 Phase 3 - Netlist migration to core
 */
public class CircuitNetlist implements INetList {

    // Basic circuit topology
    private CircuitTypCore[] componentTypes;
    private int[] nodeX;          // X terminal node indices
    private int[] nodeY;          // Y terminal node indices
    private int[] voltageSourceNumbers;  // Voltage source numbering for extended MNA
    private double[][] parameters;       // Component parameters

    // Netlist metrics
    private int nodeCount;              // Maximum node index (nodes 0 to nodeCount)
    private int voltageSourceCount;     // Maximum voltage source index
    private int elementCount;           // Total number of elements

    // Magnetic coupling management
    private final MutualCouplingRegistry couplingRegistry;

    // Current simulation state
    private double simulationTime;

    // Result storage (Task 3.6)
    private double[] lastNodeVoltages = new double[0];
    private double[] lastComponentCurrents = new double[0];

    /**
     * Creates a new empty CircuitNetlist.
     * Call initNetlist() to populate it with circuit data.
     */
    public CircuitNetlist() {
        this.couplingRegistry = new MutualCouplingRegistry();
        this.simulationTime = 0.0;
    }

    /**
     * Initialize circuit netlist from basic component data.
     *
     * <p>This method populates the netlist with all circuit topology and parameter information
     * needed for MNA. The component types array, node indices, and parameters must have
     * consistent lengths equal to elementCount.
     *
     * <p>Node numbering: Nodes are numbered from 0 (ground reference) to maxNodeIndex.
     * The total number of nodes is maxNodeIndex + 1.
     *
     * <p>Voltage source numbering: Voltage sources and LKOP2 elements (coupable inductors)
     * are numbered sequentially starting from 1. Non-source elements have -1.
     *
     * @param types component types array (one per element)
     * @param nodeX component positive terminal (X) node indices
     * @param nodeY component negative terminal (Y) node indices
     * @param voltageSourceNr voltage source numbering array (-1 for non-sources)
     * @param params component parameter arrays (layout depends on component type)
     * @param maxNodeIndex maximum node index in circuit
     * @param maxVoltageSourceIndex maximum voltage source index
     * @param elementCount total number of elements (including subcircuit)
     *
     * @throws IllegalArgumentException if array lengths are inconsistent or values invalid
     */
    public void initNetlist(
            CircuitTypCore[] types,
            int[] nodeX,
            int[] nodeY,
            int[] voltageSourceNr,
            double[][] params,
            int maxNodeIndex,
            int maxVoltageSourceIndex,
            int elementCount) {

        // Validate input arrays
        if (types == null || nodeX == null || nodeY == null || voltageSourceNr == null || params == null) {
            throw new IllegalArgumentException("Array parameters cannot be null");
        }

        if (types.length != elementCount || nodeX.length != elementCount ||
            nodeY.length != elementCount || voltageSourceNr.length != elementCount ||
            params.length != elementCount) {
            throw new IllegalArgumentException(
                String.format("Array length mismatch: expected %d elements but got types=%d, " +
                    "nodeX=%d, nodeY=%d, voltageSourceNr=%d, params=%d",
                    elementCount, types.length, nodeX.length, nodeY.length,
                    voltageSourceNr.length, params.length));
        }

        if (maxNodeIndex < 0) {
            throw new IllegalArgumentException("maxNodeIndex must be non-negative");
        }
        if (maxVoltageSourceIndex < 0) {
            throw new IllegalArgumentException("maxVoltageSourceIndex must be non-negative");
        }

        // Assign fields
        this.componentTypes = types.clone();
        this.nodeX = nodeX.clone();
        this.nodeY = nodeY.clone();
        this.voltageSourceNumbers = voltageSourceNr.clone();
        this.parameters = new double[elementCount][];
        for (int i = 0; i < elementCount; i++) {
            if (params[i] != null) {
                this.parameters[i] = params[i].clone();
            }
        }
        this.nodeCount = maxNodeIndex;
        this.voltageSourceCount = maxVoltageSourceIndex;
        this.elementCount = elementCount;

        // Clear any previous couplings when reinitializing
        couplingRegistry.clear();
    }

    /**
     * Sets all component parameters from an existing NetListLK (or equivalent data).
     * Called by NetlistBuilder when converting circuit file data to headless netlist.
     *
     * <p>This is a factory-style convenience method that wraps initNetlist() call.
     * It allows callers to set component parameters per-element after construction.
     *
     * <p>Equivalent to calling:
     * <pre>
     * netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params,
     *                     maxNode, maxVoltageSource, elementCount);
     * </pre>
     *
     * @param types component types array (one per element)
     * @param nodeX component positive terminal (X) node indices
     * @param nodeY component negative terminal (Y) node indices
     * @param voltageSourceNr voltage source numbering array (-1 for non-sources)
     * @param params component parameter arrays (layout depends on component type)
     * @param maxNode maximum node index in circuit
     * @param maxVoltageSource maximum voltage source index
     * @param elementCount total number of elements (including subcircuit)
     *
     * @throws IllegalArgumentException if array lengths are inconsistent or values invalid
     */
    public void setAllParameters(
            CircuitTypCore[] types,
            int[] nodeX,
            int[] nodeY,
            int[] voltageSourceNr,
            double[][] params,
            int maxNode,
            int maxVoltageSource,
            int elementCount) {
        this.initNetlist(types, nodeX, nodeY, voltageSourceNr, params,
                         maxNode, maxVoltageSource, elementCount);
    }

    /**
     * Gets a single parameter value for a component element.
     *
     * <p>Convenience method for accessing individual parameter values from the
     * multi-dimensional parameter array without exposing the full array.
     *
     * @param elementIndex the element index (0 to elementCount-1)
     * @param paramIndex the parameter index within that element's parameter array
     * @return the parameter value
     *
     * @throws IndexOutOfBoundsException if indices are out of range
     * @throws NullPointerException if parameters have not been initialized
     */
    public double getElementParam(int elementIndex, int paramIndex) {
        if (elementIndex < 0 || elementIndex >= elementCount) {
            throw new IndexOutOfBoundsException(
                String.format("Element index %d out of range [0, %d)", elementIndex, elementCount));
        }
        double[] params = parameters[elementIndex];
        if (params == null) {
            throw new NullPointerException(
                String.format("Parameters for element %d are null", elementIndex));
        }
        if (paramIndex < 0 || paramIndex >= params.length) {
            throw new IndexOutOfBoundsException(
                String.format("Parameter index %d out of range [0, %d)", paramIndex, params.length));
        }
        return params[paramIndex];
    }

    /**
     * Sets a single parameter value for a component element.
     *
     * <p>Convenience method for updating individual parameter values.
     * Used when parameters change during simulation (e.g., temperature-dependent values).
     *
     * @param elementIndex the element index (0 to elementCount-1)
     * @param paramIndex the parameter index within that element's parameter array
     * @param value the new parameter value
     *
     * @throws IndexOutOfBoundsException if indices are out of range
     * @throws NullPointerException if parameters have not been initialized
     */
    public void setElementParam(int elementIndex, int paramIndex, double value) {
        if (elementIndex < 0 || elementIndex >= elementCount) {
            throw new IndexOutOfBoundsException(
                String.format("Element index %d out of range [0, %d)", elementIndex, elementCount));
        }
        double[] params = parameters[elementIndex];
        if (params == null) {
            throw new NullPointerException(
                String.format("Parameters for element %d are null", elementIndex));
        }
        if (paramIndex < 0 || paramIndex >= params.length) {
            throw new IndexOutOfBoundsException(
                String.format("Parameter index %d out of range [0, %d)", paramIndex, params.length));
        }
        params[paramIndex] = value;
    }

    /**
     * Registers a magnetic coupling between two inductors.
     *
     * <p>This is extracted from NetListLK.definiere_magnetischeKopplungen_im_LK().
     * The coupling is defined by a coupling coefficient k (0 to 1) and the inductance
     * values of both coupled inductors. The mutual inductance is calculated as:
     * M = k * sqrt(L1 * L2)
     *
     * <p>The coupling information is used during matrix stamping (LKMatrizen) to inject
     * mutual inductance effects between coupled inductor currents.
     *
     * @param inductor1Index element index of first inductor in netlist
     * @param inductor2Index element index of second inductor in netlist
     * @param couplingCoefficient k value (typically 0 to 1, where 1 = perfect coupling)
     * @param inductance1 inductance value of first inductor (Henries)
     * @param inductance2 inductance value of second inductor (Henries)
     *
     * @return the registered Coupling object
     *
     * @throws IllegalArgumentException if coupling parameters are invalid
     * @throws IndexOutOfBoundsException if inductor indices are out of range
     */
    public MutualCouplingRegistry.Coupling registerMutualCoupling(
            int inductor1Index,
            int inductor2Index,
            double couplingCoefficient,
            double inductance1,
            double inductance2) {

        // Validate inductor indices
        if (inductor1Index < 0 || inductor1Index >= elementCount) {
            throw new IndexOutOfBoundsException(
                String.format("Inductor1 index %d out of range [0, %d)", inductor1Index, elementCount));
        }
        if (inductor2Index < 0 || inductor2Index >= elementCount) {
            throw new IndexOutOfBoundsException(
                String.format("Inductor2 index %d out of range [0, %d)", inductor2Index, elementCount));
        }

        // Delegate to registry
        return couplingRegistry.registerCoupling(inductor1Index, inductor2Index,
                                                 couplingCoefficient, inductance1, inductance2);
    }

    /**
     * Gets all couplings involving a specific inductor.
     *
     * @param inductorIndex the inductor element index
     * @return list of couplings (empty if not coupled)
     */
    public List<MutualCouplingRegistry.Coupling> getCouplingsFor(int inductorIndex) {
        return couplingRegistry.getCouplingsFor(inductorIndex);
    }

    /**
     * Gets coupling partners for an inductor.
     *
     * @param inductorIndex the inductor element index
     * @return list of partner inductor indices
     */
    public List<Integer> getCouplingPartners(int inductorIndex) {
        return couplingRegistry.getCouplingPartners(inductorIndex);
    }

    /**
     * Gets the mutual inductance between two inductors.
     *
     * @param inductor1Index first inductor
     * @param inductor2Index second inductor
     * @return mutual inductance M, or 0 if not coupled
     */
    public double getMutualInductance(int inductor1Index, int inductor2Index) {
        return couplingRegistry.getMutualInductance(inductor1Index, inductor2Index);
    }

    /**
     * Checks if two inductors are coupled.
     *
     * @param inductor1Index first inductor
     * @param inductor2Index second inductor
     * @return true if coupled
     */
    public boolean areCoupled(int inductor1Index, int inductor2Index) {
        return couplingRegistry.areCoupled(inductor1Index, inductor2Index);
    }

    /**
     * Gets all couplings in the netlist.
     *
     * @return unmodifiable list of all couplings
     */
    public List<MutualCouplingRegistry.Coupling> getAllCouplings() {
        return couplingRegistry.getAllCouplings();
    }

    /**
     * Builds coupling arrays for LKMatrices (MNA matrix stamping).
     *
     * <p>This method is called during matrix setup to prepare mutual inductance data
     * for the MNA solver. The returned arrays are in the format:
     * <ul>
     *   <li>zuLKOP2gehoerigeM_spgQnr[] - voltage source numbers of coupling partners</li>
     *   <li>zuLKOP2gehoerigeM_kWerte[] - mutual inductance values</li>
     * </ul>
     *
     * @return triple of arrays: [spgQnr[], kWerte[]]
     */
    public double[][][] buildCouplingArrays() {
        return couplingRegistry.buildCouplingArrays(voltageSourceNumbers);
    }

    // ============================================================================
    // INetList Implementation - Basic netlist interface
    // ============================================================================

    @Override
    public int getNodeMax() {
        return nodeCount;
    }

    @Override
    public int getVoltageSourceMax() {
        return voltageSourceCount;
    }

    @Override
    public int getElementCount() {
        return elementCount;
    }

    @Override
    public CircuitTypCore getType(int index) {
        if (index < 0 || index >= elementCount) {
            throw new IndexOutOfBoundsException("Element index: " + index);
        }
        return componentTypes[index];
    }

    @Override
    public int getNodeX(int index) {
        if (index < 0 || index >= elementCount) {
            throw new IndexOutOfBoundsException("Element index: " + index);
        }
        return nodeX[index];
    }

    @Override
    public int getNodeY(int index) {
        if (index < 0 || index >= elementCount) {
            throw new IndexOutOfBoundsException("Element index: " + index);
        }
        return nodeY[index];
    }

    @Override
    public double[] getParameter(int index) {
        if (index < 0 || index >= elementCount) {
            throw new IndexOutOfBoundsException("Element index: " + index);
        }
        return parameters[index];
    }

    @Override
    public int getVoltageSourceNumber(int index) {
        if (index < 0 || index >= elementCount) {
            throw new IndexOutOfBoundsException("Element index: " + index);
        }
        return voltageSourceNumbers[index];
    }

    @Override
    public double getSimulationTime() {
        return simulationTime;
    }

    /**
     * Sets the current simulation time.
     *
     * @param time the simulation time in seconds
     */
    public void setSimulationTime(double time) {
        this.simulationTime = time;
    }

    // ============================================================================
    // Task 3.6: Nonlinear Component Update & Result Storage
    // ============================================================================

    /**
     * Update nonlinear component parameters (capacitances, resistances).
     * Called before building matrix A when diode/transistor states change.
     *
     * <p>This method evaluates whether any nonlinear component's parameters
     * have changed during the current timestep (e.g., diode switching state,
     * temperature-dependent resistance). If parameters changed, the MNA matrix
     * must be rebuilt.
     *
     * <p>Extracted from NetListLK.updateNonlinearCapacitancesAndResistors().
     * Current implementation is a placeholder that returns false (no updates).
     * When component models are fully migrated to gecko-simulation-core,
     * this will iterate through registered nonlinear components and evaluate
     * their parameter changes.
     *
     * @return true if any component was updated (matrix needs rebuild), false otherwise
     */
    public boolean updateNonlinearComponents() {
        // Placeholder for now - will be implemented when component models are available
        // Return false = no updates needed (matrix unchanged)
        return false;
    }

    /**
     * Record computed result data (node voltages, component currents) after solve.
     *
     * <p>This method stores the solution vectors computed by the MatrixSolver
     * for later extraction and visualization. The data is copied (not referenced)
     * to prevent external modification.
     *
     * <p>Extracted from LKMatrizen result extraction logic.
     * Called by SimulationEngine after each timestep once the MNA system is solved.
     *
     * @param nodeVoltages node potential array from MatrixSolver.getP()
     *                     (nodeVoltages[i] = potential at node i relative to ground)
     * @param componentCurrents component current array from MatrixSolver.getIALT()
     *                           (componentCurrents[i] = current through element i)
     */
    public void storeResults(double[] nodeVoltages, double[] componentCurrents) {
        // Store for data container extraction
        this.lastNodeVoltages = nodeVoltages != null ? nodeVoltages.clone() : new double[0];
        this.lastComponentCurrents = componentCurrents != null ? componentCurrents.clone() : new double[0];
    }

    /**
     * Gets the last computed node voltages (solution vector P from MNA).
     *
     * <p>Returns the node voltage array from the most recent simulation step.
     * The array is a copy, so modifications do not affect the stored values.
     *
     * @return array of node voltages indexed by node number (0 = ground reference)
     */
    public double[] getLastNodeVoltages() {
        return lastNodeVoltages.clone();
    }

    /**
     * Gets the last computed component currents (solution vector IALT from MNA).
     *
     * <p>Returns the component current array from the most recent simulation step.
     * The array is a copy, so modifications do not affect the stored values.
     *
     * @return array of component currents indexed by element number
     */
    public double[] getLastComponentCurrents() {
        return lastComponentCurrents.clone();
    }

    /**
     * Gets reference to last computed node voltages (for performance-critical code).
     *
     * <p>CAUTION: Returns direct reference without cloning. Do not modify returned array.
     * Use only in performance-critical paths where cloning overhead is unacceptable.
     *
     * @return reference to internal node voltages array
     */
    public double[] getLastNodeVoltagesRef() {
        return lastNodeVoltages;
    }

    /**
     * Gets reference to last computed component currents (for performance-critical code).
     *
     * <p>CAUTION: Returns direct reference without cloning. Do not modify returned array.
     * Use only in performance-critical paths where cloning overhead is unacceptable.
     *
     * @return reference to internal component currents array
     */
    public double[] getLastComponentCurrentsRef() {
        return lastComponentCurrents;
    }

    // ============================================================================
    // Utility Methods
    // ============================================================================

    /**
     * Gets the total number of nodes in the netlist (including ground).
     *
     * @return total node count = nodeMax + 1
     */
    public int getTotalNodeCount() {
        return nodeCount + 1;
    }

    /**
     * Validates netlist consistency.
     * Checks that all node indices are within bounds and parameters are assigned.
     *
     * @return true if netlist is valid
     */
    public boolean isValid() {
        if (componentTypes == null || elementCount == 0) {
            return false;
        }

        for (int i = 0; i < elementCount; i++) {
            if (componentTypes[i] == null) {
                return false;
            }
            if (nodeX[i] < 0 || nodeX[i] > nodeCount) {
                return false;
            }
            if (nodeY[i] < 0 || nodeY[i] > nodeCount) {
                return false;
            }
            if (parameters[i] == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Clears all netlist data.
     * After calling this, you must call initNetlist() again to repopulate.
     */
    public void clear() {
        componentTypes = null;
        nodeX = null;
        nodeY = null;
        voltageSourceNumbers = null;
        parameters = null;
        nodeCount = 0;
        voltageSourceCount = 0;
        elementCount = 0;
        couplingRegistry.clear();
        simulationTime = 0.0;
        lastNodeVoltages = new double[0];
        lastComponentCurrents = new double[0];
    }

    @Override
    public String toString() {
        return String.format(
            "CircuitNetlist[elements=%d, nodes=0..%d, vSources=0..%d, couplings=%d, time=%.6e]",
            elementCount, nodeCount, voltageSourceCount,
            couplingRegistry.getCouplingCount(), simulationTime);
    }
}

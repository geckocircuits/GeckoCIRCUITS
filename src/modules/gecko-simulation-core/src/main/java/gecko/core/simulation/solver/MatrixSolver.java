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
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.matrix.IMatrixStamper;
import gecko.core.circuit.matrix.StamperRegistry;
import gecko.core.circuit.netlist.INetList;
import gecko.core.math.Matrix;

/**
 * Matrix solver for MNA (Modified Nodal Analysis) circuit simulations.
 *
 * Manages allocation and initialization of matrices and vectors required for
 * solving circuit equations using modified nodal analysis. Extracted from legacy
 * LKMatrices to provide a GUI-free, testable solver implementation.
 *
 * Matrices include:
 * - System matrix A and right-hand side vector b for solving Ax=b
 * - Node potentials (p, pALT, pALTALT, pALTALTALT) for time-stepping methods
 * - Component currents (iALT, iALTALT, iALTALTALT) for multi-step integration
 */
public class MatrixSolver {
    /** Large conductance added to ground node (row/col 0) to anchor the MNA system.
     *  Without this, the KCL equations are linearly dependent and the matrix is singular. */
    private static final double GROUND_CONDUCTANCE = 1e15;

    private int matrixSize;           // Matrix order (node count + voltage source count + 1)
    private double[][] a;            // System matrix A for MNA
    private double[] bVector;         // Right-hand side vector b
    private double[] p;               // Current node potentials
    private double[] pALT;            // Previous time-step node potentials
    private double[] pALTALT;         // Two steps back node potentials
    private double[] pALTALTALT;      // Three steps back node potentials
    private double[] iALT;            // Previous time-step component currents
    private double[] iALTALT;         // Two steps back component currents
    private double[] iALTALTALT;      // Three steps back component currents
    private SolverType solverType;

    // LU decomposition solver state
    private Matrix matrixSolverA;     // Cached Matrix wrapper for A
    private boolean matrixChanged;    // Flag to indicate A matrix needs refactorization
    private boolean wireReference;

    /**
     * Constructs a MatrixSolver with specified solver type.
     *
     * @param solverType the numerical integration method (e.g., Backward Euler, Trapezoidal)
     */
    public MatrixSolver(SolverType solverType) {
        if (solverType == null) {
            throw new IllegalArgumentException("Solver type cannot be null");
        }
        this.solverType = solverType;
        this.matrixSolverA = null;
        this.matrixChanged = true;
    }

    /**
     * Initializes all matrices and vectors for MNA simulation.
     *
     * Allocates memory for system matrix A, solution vectors, node potentials,
     * and component currents based on the circuit topology (node count and
     * voltage source count).
     *
     * @param nodeCount the number of nodes in the circuit
     * @param voltageSourceCount the number of voltage sources
     * @param elementCount the total number of circuit elements
     */
    public void initializeMatrices(int nodeCount, int voltageSourceCount, int elementCount) {
        long requiredSize = (long) nodeCount + voltageSourceCount + 1;
        if (nodeCount < 0 || voltageSourceCount < 0 || elementCount < 0 || requiredSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix dimensions must be non-negative and fit an integer");
        }
        // Matrix size includes all nodes, voltage source variables, plus ground reference
        matrixSize = (int) requiredSize;
        wireReference = false;

        // Allocate system matrix A and solution vector b
        a = new double[matrixSize][matrixSize];
        bVector = new double[matrixSize];

        // Allocate node potential vectors for multi-step integration methods
        p = new double[matrixSize];
        pALT = new double[matrixSize];
        pALTALT = new double[matrixSize];
        pALTALTALT = new double[matrixSize];

        // Allocate component current vectors for multi-step integration methods
        iALT = new double[elementCount];
        iALTALT = new double[elementCount];
        iALTALTALT = new double[elementCount];
        matrixSolverA = null;
        matrixChanged = true;
    }

    // Getters for matrix components
    public int getMatrixSize() {
        return matrixSize;
    }

    public double[][] getSystemMatrix() {
        return a;
    }

    public double[] getb() {
        return bVector;
    }

    public double[] getP() {
        return p;
    }

    public double[] getPALT() {
        return pALT;
    }

    public double[] getPALTALT() {
        return pALTALT;
    }

    public double[] getPALTALTALT() {
        return pALTALTALT;
    }

    public double[] getIALT() {
        return iALT;
    }

    public double[] getIALTALT() {
        return iALTALT;
    }

    public double[] getIALTALTALT() {
        return iALTALTALT;
    }

    public SolverType getSolverType() {
        return solverType;
    }

    /**
     * Updates node potentials by shifting history for time-stepping methods.
     *
     * Implements the history shifting mechanism for multi-step integration methods:
     * - Shift node potentials: pALTALTALT <- pALTALT <- pALT <- p
     * - Shift component currents: iALTALTALT <- iALTALT <- iALT <- current values
     *
     * This method should be called at the end of each time step, after the matrix
     * solution is complete and component currents have been calculated. It prepares
     * the history arrays for the next time step, enabling higher-order time-stepping
     * methods (e.g., Gear-Shichman).
     *
     * @param dt the time step size (seconds)
     * @param time the current simulation time (seconds)
     * @deprecated Use {@link #shiftHistory()} followed by storing currents into iALT.
     *             This method exists for backward compatibility with existing tests.
     */
    @Deprecated
    public void updateNodePotentials(double dt, double time) {
        shiftHistory();
    }

    /**
     * Shifts all history arrays one step back, matching the proven
     * {@code aktualisiereKnotenpotentiale()} method from LKMatrices.
     *
     * <p>Order (critical for correctness):
     * <ol>
     *   <li>pALTALTALT ← pALTALT ← pALT ← p</li>
     *   <li>iALTALTALT ← iALTALT ← iALT</li>
     * </ol>
     *
     * <p>After calling this method, iALT still holds the previous step's values
     * (now also copied into iALTALT). The caller must then overwrite iALT with
     * the newly converged currents.
     */
    public void shiftHistory() {
        // Shift node potential history
        System.arraycopy(pALTALT, 0, pALTALTALT, 0, matrixSize);
        System.arraycopy(pALT, 0, pALTALT, 0, matrixSize);
        System.arraycopy(p, 0, pALT, 0, matrixSize);

        // Shift component current history
        System.arraycopy(iALTALT, 0, iALTALTALT, 0, iALT.length);
        System.arraycopy(iALT, 0, iALTALT, 0, iALT.length);
    }

    /**
     * Builds the system matrix A for MNA (Modified Nodal Analysis).
     *
     * Matrix A contains the conductance/admittance contributions from all circuit components.
     * Each component type (resistor, capacitor, inductor, etc.) stamps its contribution
     * into the matrix using the corresponding stamper from the registry.
     *
     * The matrix is cleared at the beginning and each component's stamper then adds its
     * contribution to the appropriate matrix positions based on its node connections.
     *
     * Extracted from legacy LKMatrices.schreibeMatrix_A() method (lines 116-367).
     * This simplified version:
     * - Clears the matrix
     * - Iterates through netlist components
     * - Uses stampers from the registry for component contributions
     * - Adds symmetric mutual-inductance constraints between winding currents
     *
     * @param netlist the circuit netlist containing components and topology
     * @param dt the time step size (used for dynamic components like capacitors)
     * @param time the current simulation time
     * @param capacitorError flag indicating if capacitor has convergence issues
     * @throws IllegalArgumentException if netlist is null
     */
    public void buildMatrixA(INetList netlist, double dt, double time, boolean capacitorError) {
        if (netlist == null) {
            throw new IllegalArgumentException("Netlist cannot be null");
        }

        // Step 1: Clear matrix A (set all entries to 0)
        if (a != null) {
            for (int i = 0; i < matrixSize; i++) {
                for (int j = 0; j < matrixSize; j++) {
                    a[i][j] = 0.0;
                }
            }
        }

        // Create stamper registry for component contributions
        StamperRegistry registry = StamperRegistry.createDefault();

        // Step 2: Iterate through all components in the netlist
        int elementCount = netlist.getElementCount();
        for (int elementIndex = 0; elementIndex < elementCount; elementIndex++) {
            CircuitTypCore componentType = netlist.getType(elementIndex);
            int nodeX = netlist.getNodeX(elementIndex);
            int nodeY = netlist.getNodeY(elementIndex);
            int voltageSourceNumber = netlist.getVoltageSourceNumber(elementIndex);
            int nodeZ = netlist.getNodeMax() + voltageSourceNumber;
            double[] parameters = netlist.getParameter(elementIndex);

            // Step 3: Get stamper for this component type and stamp matrix A
            IMatrixStamper stamper = registry.getStamper(componentType);
            if (stamper != null) {
                stamper.stampMatrixA(a, nodeX, nodeY, nodeZ, parameters, dt, solverType);
            }
            if (componentType == CircuitTypCore.LK_I && SourceControl.mode(parameters) == 5) {
                int target = SourceControl.target(netlist, elementIndex);
                int controlX = netlist.getNodeX(target);
                int controlY = netlist.getNodeY(target);
                double gain = parameters[11];
                a[nodeX][controlX] += gain;
                a[nodeY][controlY] += gain;
                a[nodeX][controlY] -= gain;
                a[nodeY][controlX] -= gain;
            }
            if ((componentType == CircuitTypCore.LK_U || componentType == CircuitTypCore.REL_MMF
                    || componentType == CircuitTypCore.TH_TEMP)
                    && SourceControl.mode(parameters) == 5 && parameters[14] == 0) {
                int target = SourceControl.target(netlist, elementIndex);
                double gain = parameters[11];
                a[nodeZ][netlist.getNodeX(target)] -= gain;
                a[nodeZ][netlist.getNodeY(target)] += gain;
            }
            if ((componentType == CircuitTypCore.LK_U || componentType == CircuitTypCore.REL_MMF
                    || componentType == CircuitTypCore.TH_TEMP) && SourceControl.mode(parameters) == 3) {
                int target = SourceControl.target(netlist, elementIndex);
                a[nodeZ][SourceControl.branch(netlist, target)] -= parameters[11];
            }
            if ((componentType == CircuitTypCore.LK_U || componentType == CircuitTypCore.REL_MMF
                    || componentType == CircuitTypCore.TH_TEMP) && SourceControl.mode(parameters) == 4) {
                int target = SourceControl.target(netlist, elementIndex);
                a[nodeZ][SourceControl.branch(netlist, target)] += parameters[11] / dt;
            }
            if ((componentType == CircuitTypCore.LK_U || componentType == CircuitTypCore.REL_MMF
                    || componentType == CircuitTypCore.TH_TEMP) && SourceControl.mode(parameters) == 6) {
                int target = SourceControl.target(netlist, elementIndex);
                a[nodeZ][nodeX] -= 1.0;
                a[nodeZ][nodeY] += 1.0;
                a[nodeZ][nodeZ] += 1.0 / parameters[11];
                a[nodeZ][SourceControl.branch(netlist, target)] += 1.0;
            }
            // If no stamper is registered, component is skipped (e.g., terminals, unimplemented types)
        }

        // Step 4: Pin ground node (node 0) to anchor the MNA system.
        // Without this, the KCL equations for nodes 0..N are linearly dependent
        // (they sum to zero by Kirchhoff's current law), making the matrix singular.
        // Adding a large conductance to ground forces p[0] ≈ 0.
        wireReference = netlist.usesWireReference();
        if (!wireReference) a[0][0] += GROUND_CONDUCTANCE;
        // Desktop LKMatrices pins each galvanically isolated group the same way: 1 S at its lowest node.
        // No current leaves an isolated group, so the stamp only fixes its reference potential.
        for (int node : isolatedGroupReferences(netlist)) a[node][node] += 1.0;

        double alpha = solverType == SolverType.SOLVER_BE ? 1
                : solverType == SolverType.SOLVER_TRZ ? 2 : 1.5;
        for (var coupling : netlist.getAllCouplings()) {
            int first = netlist.getNodeMax() + netlist.getVoltageSourceNumber(coupling.getInductor1Index());
            int second = netlist.getNodeMax() + netlist.getVoltageSourceNumber(coupling.getInductor2Index());
            double weight = alpha * coupling.getMutualInductance() / dt;
            a[first][second] -= weight;
            a[second][first] -= weight;
        }

        // Mark matrix as changed so LU decomposition will be recalculated
        matrixChanged = true;
    }

    /**
     * Updates controlled voltage-source limits from the current solution and invalidates the cached factorization when needed.
     *
     * @param netlist the circuit with its configured source limits and current limit states
     * @param time the current simulation time
     * @return true when a source changes its limit state
     */
    public boolean updateControlledSourceLimits(INetList netlist, double time) {
        boolean changed = false;
        for (int element = 0; element < netlist.getElementCount(); element++) {
            if (netlist.getType(element) != CircuitTypCore.LK_U) {
                continue;
            }
            double[] parameters = netlist.getParameter(element);
            if (SourceControl.mode(parameters) != 5
                    || ((int) parameters[0] == SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY && time <= 0)) {
                continue;
            }
            int target = SourceControl.target(netlist, element);
            int controlX = netlist.getNodeX(target);
            int controlY = netlist.getNodeY(target);
            int branch = netlist.getNodeMax() + netlist.getVoltageSourceNumber(element);
            double gain = parameters[11];
            double voltage = gain * (p[controlX] - p[controlY]);
            double oldState = parameters[14];
            double newState = oldState;
            if (oldState == 0) {
                newState = voltage < parameters[12] ? -1 : voltage > parameters[13] ? 1 : 0;
            } else if ((oldState == -1 && voltage >= parameters[12])
                    || (oldState == 1 && voltage <= parameters[13])) {
                // Return to the linear equation before a source can reach the opposite limit.
                newState = 0;
            }
            if (oldState == newState) {
                continue;
            }

            if (oldState == 0) {
                a[branch][controlX] += gain;
                a[branch][controlY] -= gain;
            } else {
                bVector[branch] -= oldState < 0 ? parameters[12] : parameters[13];
            }
            if (newState == 0) {
                a[branch][controlX] -= gain;
                a[branch][controlY] += gain;
            } else {
                bVector[branch] += newState < 0 ? parameters[12] : parameters[13];
            }
            parameters[14] = newState;
            changed = true;
        }
        if (changed) {
            matrixChanged = true;
        }
        return changed;
    }

    /**
     * Builds the right-hand side vector b for the MNA system (Ax=b).
     *
     * This method stamps the b-vector contributions from all circuit components,
     * including:
     * - Inductors (history terms from previous currents)
     * - Capacitors (history terms from previous voltages)
     * - Independent voltage and current sources (with support for DC, AC, and controlled variants)
     * - Nonlinear components (diodes, IGBTs, thyristors)
     * - Coupled inductors (LKOP2) with mutual inductance coupling
     *
     * The method uses different formulations based on the solver type:
     * - SOLVER_BE (Backward Euler): First-order accurate, stable
     * - SOLVER_TRZ (Trapezoidal): Second-order accurate, oscillation-prone
     * - SOLVER_GS (Gear-Shichman): Third-order accurate for stiff problems
     *
     * Extracted from LKMatrices.schreibeMatrix_B() (lines 368-595).
     *
     * @param netlist the circuit netlist containing component types, node connections, and parameters
     * @param dt the time step (integration time interval in seconds)
     * @param time the current simulation time (used for time-dependent sources)
     * @param capacitorError flag for capacitor error correction (reserved for future use)
     */
    public void buildVectorB(INetList netlist, double dt, double time, boolean capacitorError) {
        final double FAST_NULL_L = 1e-12;  // Threshold for negligible inductance

        // Clear vector b for accumulation
        for (int i = 0; i < matrixSize; i++) {
            bVector[i] = 0.0;
        }

        // Iterate through all circuit elements and stamp b-vector contributions
        for (int elementIdx = 0; elementIdx < netlist.getElementCount(); elementIdx++) {
            CircuitTypCore componentType = netlist.getType(elementIdx);
            int nodeX = netlist.getNodeX(elementIdx);
            int nodeY = netlist.getNodeY(elementIdx);
            int voltageSourceNumber = netlist.getVoltageSourceNumber(elementIdx);
            int voltageSourceIdx = netlist.getNodeMax() + voltageSourceNumber;

            double bContribution = 0.0;

            switch (componentType) {
                // ===== Resistors and passive components (no b-vector contribution) =====
                case LK_R:
                case REL_RELUCTANCE:
                case TH_RTH:
                case TH_AMBIENT:
                case LK_S:  // Ideal switch behaves like very high/low resistance
                case LK_MOSFET:
                    // No contribution to b-vector (handled in matrix A)
                    break;

                // ===== Inductors (with history terms) =====
                case LK_L:
                case NONLIN_REL:
                    double[] params = netlist.getParameter(elementIdx);
                    double inductance = params[0];

                    if (inductance < FAST_NULL_L) {
                        // Nearly-zero inductance: treat as connection
                        if (solverType == SolverType.SOLVER_BE) {
                            bContribution = -iALT[elementIdx];
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            bContribution = -iALT[elementIdx] - dt * (pALT[nodeX] - pALT[nodeY]) / (2 * FAST_NULL_L);
                        } else if (solverType == SolverType.SOLVER_GS) {
                            bContribution = -4.0 / 3.0 * iALT[elementIdx] + 1.0 / 3.0 * iALTALT[elementIdx];
                        }
                    } else {
                        // Standard inductor history term
                        if (solverType == SolverType.SOLVER_BE) {
                            bContribution = -iALT[elementIdx];
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            bContribution = -iALT[elementIdx] - dt * (pALT[nodeX] - pALT[nodeY]) / (2 * inductance);
                        } else if (solverType == SolverType.SOLVER_GS) {
                            bContribution = -4.0 / 3.0 * iALT[elementIdx] + 1.0 / 3.0 * iALTALT[elementIdx];
                        }
                    }
                    bVector[nodeX] += bContribution;
                    bVector[nodeY] -= bContribution;
                    break;

                // ===== Coupled Inductors (LKOP2 with mutual inductance) =====
                case LK_LKOP2:
                    params = netlist.getParameter(elementIdx);
                    bContribution = params[0] / dt * windingHistory(elementIdx);
                    if (solverType == SolverType.SOLVER_TRZ) {
                        bContribution -= pALT[nodeX] - pALT[nodeY];
                    }
                    bVector[voltageSourceIdx] += bContribution;
                    break;

                // ===== Capacitors (with history terms) =====
                case TH_CTH:
                    // Thermal capacitance: ensure parameters are set correctly
                    params = netlist.getParameter(elementIdx);
                    params[6] = params[0];
                    params[7] = params[0];
                    // Falls through to LK_C handling
                    /* falls through */
                case LK_C:
                    params = netlist.getParameter(elementIdx);
                    double capacitance = params[6];
                    double nonlinearFactor = params[7];
                    double fac = 1.0 - nonlinearFactor / capacitance;

                    if (solverType == SolverType.SOLVER_BE) {
                        bContribution = capacitance / dt * (pALT[nodeX] - pALT[nodeY]) + fac * params[10];
                    } else if (solverType == SolverType.SOLVER_TRZ) {
                        bContribution = 2 * capacitance / dt * (pALT[nodeX] - pALT[nodeY]) + iALT[elementIdx] + fac * params[10];
                    } else if (solverType == SolverType.SOLVER_GS) {
                        bContribution = capacitance / dt * (2 * (pALT[nodeX] - pALT[nodeY]) - 0.5 * (pALTALT[nodeX] - pALTALT[nodeY])) + fac * params[10];
                    }
                    bVector[nodeX] += bContribution;
                    bVector[nodeY] -= bContribution;
                    break;

                // ===== Nonlinear Semiconductors (Diodes, IGBTs, Thyristors) =====
                case LK_IGBT:
                case LK_D:
                case LK_THYR:
                    params = netlist.getParameter(elementIdx);
                    if (params[0] < 10000) {
                        // ON-state resistance component
                        bContribution = params[1] / params[0];
                        bVector[nodeX] += bContribution;
                        bVector[nodeY] -= bContribution;
                    }
                    break;

                // ===== Current Sources (independent and controlled) =====
                case LK_I:
                case TH_FLOW: {
                    params = netlist.getParameter(elementIdx);
                    int sourceType = (int) params[0];
                    double amplitude;
                    double frequency;
                    double phase;
                    double offset;

                    switch (sourceType) {
                        case SourceType.QUELLE_DC_NEW:
                        case SourceType.QUELLE_DC:
                            bContribution = -params[1];
                            break;
                        case SourceType.QUELLE_SIGNALGESTEUERT_NEW:
                        case SourceType.QUELLE_SIGNALGESTEUERT:
                            bContribution = -params[1];
                            break;
                        case SourceType.QUELLE_SIN_NEW:
                        case SourceType.QUELLE_SIN:
                            // Sinusoidal source: amplitude * sin(2*pi*f*t - phase) + offset
                            amplitude = params[20];
                            frequency = params[2];
                            phase = params[4];
                            offset = params[3];
                            bContribution = -amplitude * Math.sin(2 * Math.PI * frequency * time - Math.toRadians(phase)) + offset;
                            break;
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                            // Voltage-controlled current sources are handled via matrix A
                            bContribution = 0.0;
                            break;
                        default:
                            bContribution = 0.0;
                            break;
                    }

                    bVector[nodeX] += bContribution;
                    bVector[nodeY] -= bContribution;
                    break;
                }

                // ===== Voltage Sources (independent and controlled) =====
                case LK_U:
                case REL_MMF:
                case TH_TEMP: {
                    params = netlist.getParameter(elementIdx);
                    int sourceType = (int) params[0];

                    switch (sourceType) {
                        case SourceType.QUELLE_DC_NEW:
                        case SourceType.QUELLE_DC:
                            bVector[voltageSourceIdx] += params[1];
                            break;
                        case SourceType.QUELLE_SIGNALGESTEUERT_NEW:
                        case SourceType.QUELLE_SIGNALGESTEUERT:
                            bVector[voltageSourceIdx] += params[1];
                            break;
                        case SourceType.QUELLE_SIN_NEW:
                        case SourceType.QUELLE_SIN: {
                            // Sinusoidal voltage source
                            double amplitude = params[20];
                            double frequency = params[2];
                            double phase = params[4];
                            double offset = params[3];
                            bVector[voltageSourceIdx] += amplitude * Math.sin(2 * Math.PI * frequency * time - Math.toRadians(phase)) + offset;
                            break;
                        }
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                            // Voltage-controlled voltage source
                            if (params[14] == -1) {
                                bVector[voltageSourceIdx] += params[12];
                            }
                            if (params[14] == 1) {
                                bVector[voltageSourceIdx] += params[13];
                            }
                            break;
                        case SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER:
                        case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY:
                            // These are handled via matrix A (voltage is function of node potentials)
                            break;
                        case SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW:
                        case SourceType.QUELLE_DIDTCURRENTCONTROLLED: {
                            // dI/dt controlled source
                            double gain = params[11];
                            if (time <= 0) {
                                bVector[voltageSourceIdx] += 2 * gain * params[15] / dt;
                            } else {
                                int target = SourceControl.target(netlist, elementIdx);
                                bVector[voltageSourceIdx] += gain * pALT[SourceControl.branch(netlist, target)] / dt;
                            }
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }

                // ===== Terminal Elements and Special Components =====
                case LK_TERMINAL:
                case TH_TERMINAL:
                case REL_TERMINAL:
                case LK_GLOBAL_TERMINAL:
                case TH_GLOBAL_TERMINAL:
                case REL_GLOBAL_TERMINAL:
                case LK_M:
                    // No b-vector contribution for terminals or mutual inductance elements
                    break;

                // ===== Unknown or unsupported component types =====
                default:
                    // Silently ignore unknown types (already reported at circuit build time)
                    break;
            }
        }
        for (var coupling : netlist.getAllCouplings()) {
            int first = coupling.getInductor1Index();
            int second = coupling.getInductor2Index();
            double weight = coupling.getMutualInductance() / dt;
            bVector[netlist.getNodeMax() + netlist.getVoltageSourceNumber(first)] += weight * windingHistory(second);
            bVector[netlist.getNodeMax() + netlist.getVoltageSourceNumber(second)] += weight * windingHistory(first);
        }

    }

    private double windingHistory(int element) {
        return switch (solverType) {
            case SOLVER_BE -> -iALT[element];
            case SOLVER_TRZ -> -2 * iALT[element];
            case SOLVER_GS -> -2 * iALT[element] + 0.5 * iALTALT[element];
        };
    }

    /**
     * Marks the system matrix as changed, indicating that LU decomposition must be
     * recalculated on the next solve() call.
     *
     * Call this method after switch state changes or any topology modifications that
     * alter the system matrix A. The actual LU decomposition is deferred until solve()
     * is called, allowing multiple matrix changes to be batched efficiently.
     */
    public void setMatrixChanged() {
        matrixChanged = true;
    }

    /**
     * Solves the linear system Ax=b for node voltages using LU decomposition.
     *
     * This method performs the actual LU factorization (if the matrix has changed) and
     * then solves the system to compute node potentials. The result (A^-1 * b) is stored
     * in the p[] array.
     *
     * Process:
     * 1. Validates that matrices are initialized
     * 2. Creates a Matrix wrapper for A (if needed)
     * 3. Performs LU factorization (if A has changed)
     * 4. Converts b[] vector to Matrix format
     * 5. Solves Ax=b using the cached LU decomposition
     * 6. Extracts solution back to p[] array
     *
     * @throws IllegalStateException if matrices have not been initialized via initializeMatrices()
     * @throws RuntimeException if the system matrix A is singular (no unique solution)
     */
    public void solve() {
        // Validate that matrices are initialized
        if (a == null || bVector == null) {
            throw new IllegalStateException(
                    "Matrices not initialized. Call initializeMatrices() first.");
        }

        for (int row = 0; row < matrixSize; row++) {
            if (!Double.isFinite(bVector[row])) {
                throw new IllegalStateException("Nonfinite right-hand side at row " + row);
            }
            if (matrixChanged || matrixSolverA == null) {
                if (a[row] == null || a[row].length != matrixSize) {
                    throw new IllegalStateException("Invalid system matrix row " + row);
                }
                for (double value : a[row]) {
                    if (!Double.isFinite(value)) {
                        throw new IllegalStateException("Nonfinite system matrix at row " + row);
                    }
                }
            }
        }

        // Step 1: Create or update Matrix wrapper for A (only if matrix changed)
        if (matrixSolverA == null) {
            // First time: create Matrix wrapper from a[][]
            matrixSolverA = solveMatrix();
        } else if (matrixChanged) {
            // Matrix changed: reset cached LU decomposition and update wrapper
            if (wireReference) matrixSolverA = solveMatrix();
            else matrixSolverA.resetLUDecomp();
            // The underlying a[][] is already modified by buildMatrixA(),
            // so Matrix wrapper points to updated data
        }

        // Step 2: Create column vector B from b[] (nx1 matrix)
        int offset = wireReference ? 1 : 0;
        int size = matrixSize - offset;
        double[][] bArray = new double[size][1];
        for (int i = 0; i < size; i++) {
            bArray[i][0] = bVector[i + offset];
        }
        Matrix bMatrix = new Matrix(bArray);

        // Step 3: Solve Ax=b using LU decomposition
        // This internally caches the LU decomposition on first call,
        // and reuses it on subsequent calls (efficient for multiple RHS)
        Matrix xMatrix = matrixSolverA.solve(bMatrix);

        // Step 4: Extract solution back to p[] array
        double[][] x = xMatrix.getArray();
        for (int i = 0; i < size; i++) {
            if (!Double.isFinite(x[i][0])) {
                throw new IllegalStateException("Nonfinite solution at row " + i);
            }
        }
        if (wireReference) p[0] = 0;
        for (int i = 0; i < size; i++) {
            p[i + offset] = x[i][0];
        }

        // Mark that matrix is no longer changed (until next buildMatrixA or explicit flag)
        matrixChanged = false;
    }

    private Matrix solveMatrix() {
        if (!wireReference) return new Matrix(a, matrixSize, matrixSize);
        // Desktop excludes its reference row and column from LU decomposition.
        double[][] reduced = new double[matrixSize - 1][matrixSize - 1];
        for (int row = 1; row < matrixSize; row++) {
            System.arraycopy(a[row], 1, reduced[row - 1], 0, matrixSize - 1);
        }
        return new Matrix(reduced);
    }

    /** Lowest node of every conductively connected node group that does not contain node 0. */
    static int[] isolatedGroupReferences(INetList netlist) {
        int[] parent = new int[netlist.getNodeMax() + 1];
        for (int node = 0; node < parent.length; node++) parent[node] = node;
        for (int element = 0; element < netlist.getElementCount(); element++) {
            CircuitTypCore type = netlist.getType(element);
            // An ideal current source adds no voltage constraint or conductance between its nodes.
            if (type == CircuitTypCore.LK_I || type == CircuitTypCore.TH_FLOW) continue;
            int x = root(parent, netlist.getNodeX(element)), y = root(parent, netlist.getNodeY(element));
            // Keep the lower node as root, so each root is its group's lowest node.
            if (x < y) parent[y] = x; else parent[x] = y;
        }
        return java.util.stream.IntStream.range(1, parent.length)
                .filter(node -> root(parent, node) == node && root(parent, 0) != node).toArray();
    }

    private static int root(int[] parent, int node) {
        while (parent[node] != node) node = parent[node] = parent[parent[node]];
        return node;
    }
}

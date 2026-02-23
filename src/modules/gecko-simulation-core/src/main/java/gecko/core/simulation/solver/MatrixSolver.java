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

    /**
     * Constructs a MatrixSolver with specified solver type.
     *
     * @param solverType the numerical integration method (e.g., Backward Euler, Trapezoidal)
     */
    public MatrixSolver(SolverType solverType) {
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
        // Matrix size includes all nodes, voltage source variables, plus ground reference
        matrixSize = nodeCount + voltageSourceCount + 1;

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
     */
    public void updateNodePotentials(double dt, double time) {
        // Shift node potential history:
        // Move two-steps-back to three-steps-back
        System.arraycopy(pALTALT, 0, pALTALTALT, 0, matrixSize);
        // Move one-step-back to two-steps-back
        System.arraycopy(pALT, 0, pALTALT, 0, matrixSize);
        // Move current to one-step-back
        System.arraycopy(p, 0, pALT, 0, matrixSize);

        // Shift component current history:
        // Move two-steps-back to three-steps-back
        System.arraycopy(iALTALT, 0, iALTALTALT, 0, iALT.length);
        // Move one-step-back to two-steps-back
        System.arraycopy(iALT, 0, iALTALT, 0, iALT.length);
        // Note: iALT will be populated with current values by ComponentCurrentCalculator
        // before the next call to this method
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
     * - Skips magnetic couplings (future refinement)
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
                stamper.stampMatrixA(a, nodeX, nodeY, nodeZ, parameters, dt);
            }
            // If no stamper is registered, component is skipped (e.g., terminals, unimplemented types)
        }

        // Note: Magnetic coupling (mutual inductance) handling is deferred for future refinement.
        // The legacy code injects coupling contributions after component stamping;
        // this will be added when LKOP2 coupling arrays are integrated into the netlist.

        // Mark matrix as changed so LU decomposition will be recalculated
        matrixChanged = true;
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
                    double inductanceInAMatrix = params[10];  // Inductance stored in parameter[10]

                    if (solverType == SolverType.SOLVER_BE) {
                        bContribution = -iALT[elementIdx] * (inductanceInAMatrix / dt);
                    } else if (solverType == SolverType.SOLVER_TRZ) {
                        bContribution = -(pALT[nodeX] - pALT[nodeY]) - iALT[elementIdx] * (2 * inductanceInAMatrix / dt);
                    } else if (solverType == SolverType.SOLVER_GS) {
                        bContribution = (-2.0 * iALT[elementIdx] + 0.5 * iALTALT[elementIdx]) * (inductanceInAMatrix / dt);
                    }

                    bVector[voltageSourceIdx] += bContribution;

                    // Apply mutual inductance coupling terms if present
                    // (zuLKOP2gehoerigeM_spgQnr and zuLKOP2gehoerigeM_kWerte would be passed separately)
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
                                bVector[voltageSourceIdx] += gain * pALT[voltageSourceIdx + 1] / dt;
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

        // Step 1: Create or update Matrix wrapper for A (only if matrix changed)
        if (matrixSolverA == null) {
            // First time: create Matrix wrapper from a[][]
            matrixSolverA = new Matrix(a, matrixSize, matrixSize);
        } else if (matrixChanged) {
            // Matrix changed: reset cached LU decomposition and update wrapper
            matrixSolverA.resetLUDecomp();
            // The underlying a[][] is already modified by buildMatrixA(),
            // so Matrix wrapper points to updated data
        }

        // Step 2: Create column vector B from b[] (nx1 matrix)
        double[][] bArray = new double[matrixSize][1];
        for (int i = 0; i < matrixSize; i++) {
            bArray[i][0] = bVector[i];
        }
        Matrix bMatrix = new Matrix(bArray);

        // Step 3: Solve Ax=b using LU decomposition
        // This internally caches the LU decomposition on first call,
        // and reuses it on subsequent calls (efficient for multiple RHS)
        Matrix xMatrix = matrixSolverA.solve(bMatrix);

        // Step 4: Extract solution back to p[] array
        double[][] x = xMatrix.getArray();
        for (int i = 0; i < matrixSize; i++) {
            p[i] = x[i][0];
        }

        // Mark that matrix is no longer changed (until next buildMatrixA or explicit flag)
        matrixChanged = false;
    }
}

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
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.INetList;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.SourceType;
import java.util.Arrays;

/**
 * Solver for setting initial conditions in circuit simulation.
 *
 * This class extracts the initial condition logic from LKMatrices (lines 1290-1503)
 * to provide a GUI-free, testable initial condition solver for headless operation.
 *
 * Responsibilities:
 * 1. Setting initial node potentials (voltages) from capacitors and sources
 * 2. Setting initial element currents (inductors, current sources)
 * 3. Supporting different initialization modes (dialog vs. continue)
 *
 * Key simplifications for core module:
 * - Uses INetList interface instead of concrete NetListLK
 * - Works with MatrixSolver which provides getter/setter access
 * - Throws exceptions instead of showing GUI dialogs
 * - Seeds all integration histories from either configured states or saved states
 *
 * @author GeckoCIRCUITS Team
 */
public class InitialConditionSolver {

    private final SolverType solverType;

    /** Threshold for treating inductance as zero */
    private static final double FAST_NULL_L = 1e-10;

    /**
     * Constructs an InitialConditionSolver with the specified solver type.
     *
     * @param solverType the numerical integration method
     */
    public InitialConditionSolver(SolverType solverType) {
        if (solverType == null) {
            throw new IllegalArgumentException("Solver type cannot be null");
        }
        this.solverType = solverType;
    }

    /**
     * Restores saved node potentials and currents, then seeds all integration histories.
     * Configured dialog values remain separate from these saved runtime values.
     *
     * @param matrixSolver the matrix solver containing state arrays to initialize
     * @param netlist the circuit netlist interface
     * @param solverType the solver type for coefficient calculations
     * @throws IllegalStateException if initialization fails
     */
    public void setInitialConditions(
        MatrixSolver matrixSolver,
        INetList netlist,
        SolverType solverType
    ) {
        if (matrixSolver == null) {
            throw new IllegalStateException("MatrixSolver cannot be null");
        }
        if (netlist == null) {
            throw new IllegalStateException("INetList cannot be null");
        }

        validateState(matrixSolver, netlist);
        if (solverType != this.solverType) {
            throw new IllegalArgumentException("Initialization solver types must match");
        }
        validateSavedParameters(netlist);

        // Initialize all potential arrays to zero
        initializePotentials(matrixSolver);

        // Initialize all current arrays to zero
        initializeCurrents(matrixSolver, netlist);

        // Restore saved potentials from netlist parameters (for CONTINUE mode)
        restorePotentialsFromNetlist(matrixSolver, netlist);

        // Restore saved currents from netlist parameters
        restoreCurrentsFromNetlist(matrixSolver, netlist);
        double[] potentials = matrixSolver.getPALT();
        double reference = potentials[0];
        for (int node = 0; node <= netlist.getNodeMax(); node++) {
            potentials[node] -= reference;
        }
        seedHistories(matrixSolver, potentials, matrixSolver.getIALT());
    }

    /**
     * Initializes configured dynamic states. Zero-start circuits retain their existing startup convention.
     * A nonzero state is held fixed while the surrounding instantaneous resistive/source network is solved.
     */
    public void setDialogInitialConditions(MatrixSolver matrixSolver, INetList netlist) {
        validateState(matrixSolver, netlist);
        boolean hasInitialState = false;
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (isDynamic(netlist.getType(i))) {
                double[] parameters = netlist.getParameter(i);
                double value = parameters.length > 1 ? parameters[1] : 0;
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("Nonfinite initial state for element " + i);
                }
                hasInitialState |= value != 0;
            }
        }
        if (!hasInitialState) {
            seedHistories(matrixSolver, new double[matrixSolver.getMatrixSize()],
                    new double[netlist.getElementCount()]);
            return;
        }

        int count = netlist.getElementCount();
        boolean inductorCutset = hasInductorCutset(netlist);
        CircuitTypCore[] types = new CircuitTypCore[count];
        int[] x = new int[count], y = new int[count], sources = new int[count];
        double[][] parameters = new double[count][];
        int sourceCount = 0;
        for (int i = 0; i < count; i++) {
            CircuitTypCore original = netlist.getType(i);
            types[i] = original;
            x[i] = netlist.getNodeX(i);
            y[i] = netlist.getNodeY(i);
            double[] values = netlist.getParameter(i);
            parameters[i] = Arrays.copyOf(values, Math.max(13, values.length));
            boolean zeroStartCapacitor = (original == CircuitTypCore.LK_C || original == CircuitTypCore.TH_CTH)
                    && parameters[i][1] == 0;
            if (isDynamic(original) && !(inductorCutset && (isInductor(original) || zeroStartCapacitor))) {
                double initial = parameters[i][1];
                types[i] = original == CircuitTypCore.LK_C || original == CircuitTypCore.TH_CTH
                        ? CircuitTypCore.LK_U : CircuitTypCore.LK_I;
                parameters[i] = new double[13];
                parameters[i][0] = SourceType.QUELLE_DC_NEW;
                parameters[i][1] = initial;
            }
            boolean voltageSource = types[i] == CircuitTypCore.LK_U || types[i] == CircuitTypCore.TH_TEMP
                    || types[i] == CircuitTypCore.REL_MMF || types[i] == CircuitTypCore.LK_LKOP2;
            sources[i] = voltageSource ? ++sourceCount : -1;
        }
        CircuitNetlist initialNetwork = new CircuitNetlist();
        initialNetwork.initNetlist(types, x, y, sources, parameters,
                netlist.getNodeMax(), sourceCount, count);
        initialNetwork.setWireReference(netlist.usesWireReference());
        if (inductorCutset) {
            for (var coupling : netlist.getAllCouplings()) {
                int first = coupling.getInductor1Index();
                int second = coupling.getInductor2Index();
                initialNetwork.registerMutualCoupling(first, second, coupling.getCouplingCoefficient(),
                        parameters[first][0], parameters[second][0]);
            }
        }
        MatrixSolver initialSolver = new MatrixSolver(inductorCutset ? solverType : SolverType.SOLVER_BE);
        initialSolver.initializeMatrices(netlist.getNodeMax(), sourceCount, count);
        // A cutset requires the complete LKMatrices 1 ns pre-step, including every dynamic branch and source.
        // Its histories start at zero. Configured inductor currents are restored after the voltage solve.
        double initialStep = inductorCutset ? 1e-9 : 1;
        double initialTime = inductorCutset ? -initialStep : 0;
        initialSolver.buildMatrixA(initialNetwork, initialStep, initialTime, false);
        initialSolver.buildVectorB(initialNetwork, initialStep, initialTime, false);
        initialSolver.solve();
        double[] currents = new double[count];
        new ComponentCurrentCalculator().calculateComponentCurrents(
                initialSolver, initialNetwork, currents, 0, initialStep, initialTime, true);
        double[] potentials = new double[matrixSolver.getMatrixSize()];
        System.arraycopy(initialSolver.getP(), 0, potentials, 0, netlist.getNodeMax() + 1);
        for (int i = 0; i < count; i++) {
            if (inductorCutset && isInductor(netlist.getType(i))) {
                currents[i] = parameters[i][1];
            } else if (inductorCutset && (netlist.getType(i) == CircuitTypCore.LK_C
                    || netlist.getType(i) == CircuitTypCore.TH_CTH)) {
                // LKMatrices clears all capacitor current histories after its dialog-start pre-step.
                currents[i] = 0;
            }
            if (!Double.isFinite(currents[i])) {
                throw new IllegalStateException("Nonfinite initial current for element " + i);
            }
            int source = netlist.getVoltageSourceNumber(i);
            if (source > 0) {
                potentials[netlist.getNodeMax() + source] = currents[i];
            }
        }
        seedHistories(matrixSolver, potentials, currents);
    }

    /**
     * Solves the DC operating point and seeds every integration history with it.
     * Capacitors are open, inductors (coupled or not) are shorts, sources take their t=0 value, and switches keep
     * the state their parameters hold now. Diode-family states are the bias-consistent set found here; a stored conducting thyristor stays latched unless reverse biased.
     * The netlist, including its configured initial states, is not modified.
     *
     * @param matrixSolver the runtime solver whose histories are seeded
     * @param netlist the circuit in the node order of the runtime solver
     * @throws IllegalArgumentException if the circuit holds an element the DC solve does not support
     * @throws IllegalStateException if the DC point is not unique or the diode states do not converge
     */
    public void setDcOperatingPoint(MatrixSolver matrixSolver, INetList netlist) {
        validateState(matrixSolver, netlist);
        int count = netlist.getElementCount();
        int nodeMax = netlist.getNodeMax();
        CircuitTypCore[] types = new CircuitTypCore[count];
        int[] x = new int[count], y = new int[count], sources = new int[count];
        double[][] parameters = new double[count][];
        int sourceCount = netlist.getVoltageSourceMax();
        for (int i = 0; i < count; i++) {
            CircuitTypCore original = netlist.getType(i);
            types[i] = original;
            x[i] = netlist.getNodeX(i);
            y[i] = netlist.getNodeY(i);
            parameters[i] = netlist.getParameter(i).clone();
            sources[i] = -1;
            requireDcSupport(original, parameters[i], i);
            switch (original) {
                case LK_C -> {
                    // Open: a zero current source keeps the element and its index.
                    types[i] = CircuitTypCore.LK_I;
                    parameters[i] = dcSource(0);
                }
                case LK_L, LK_LKOP2 -> {
                    // Short: a zero voltage source carries the branch current as its auxiliary unknown.
                    types[i] = CircuitTypCore.LK_U;
                    parameters[i] = dcSource(0);
                    // A winding keeps its auxiliary number so no auxiliary row stays empty.
                    int own = original == CircuitTypCore.LK_LKOP2 ? netlist.getVoltageSourceNumber(i) : 0;
                    sources[i] = own > 0 ? own : ++sourceCount;
                }
                case LK_U -> {
                    sources[i] = netlist.getVoltageSourceNumber(i);
                    if (sources[i] < 1) {
                        throw new IllegalArgumentException("Voltage source " + i + " has no auxiliary number");
                    }
                }
                // A diode or IGBT starts blocking. A thyristor keeps its stored state, because its latch outlives the gate.
                case LK_D, LK_IGBT -> parameters[i][0] = parameters[i][3];
                default -> { }
            }
        }
        requireCapacitorsBetweenOneGroup(netlist);

        CircuitNetlist dcNetwork = new CircuitNetlist();
        dcNetwork.initNetlist(types, x, y, sources, parameters, nodeMax, sourceCount, count);
        dcNetwork.setWireReference(netlist.usesWireReference());
        MatrixSolver dcSolver = new MatrixSolver(SolverType.SOLVER_BE);
        dcSolver.initializeMatrices(nodeMax, sourceCount, count);
        solveDiodeConsistent(dcSolver, dcNetwork);

        double[] currents = new double[count];
        new ComponentCurrentCalculator().calculateComponentCurrents(dcSolver, dcNetwork, currents, 0, 1, 0, true);
        double[] potentials = new double[matrixSolver.getMatrixSize()];
        System.arraycopy(dcSolver.getP(), 0, potentials, 0, nodeMax + 1);
        for (int i = 0; i < count; i++) {
            if (!Double.isFinite(currents[i])) {
                throw new IllegalStateException("Nonfinite DC current for element " + i);
            }
            int source = netlist.getVoltageSourceNumber(i);
            if (source > 0 && (types[i] == CircuitTypCore.LK_U)) {
                potentials[nodeMax + source] = currents[i];
            }
        }
        seedHistories(matrixSolver, potentials, currents);
    }

    private static double[] dcSource(double value) {
        double[] source = new double[13];
        source[0] = SourceType.QUELLE_DC_NEW;
        source[1] = value;
        return source;
    }

    private static void requireDcSupport(CircuitTypCore type, double[] values, int element) {
        switch (type) {
            case LK_R, LK_C, LK_L, LK_LKOP2, LK_S, LK_MOSFET, LK_M, LK_TERMINAL, LK_GLOBAL_TERMINAL -> { }
            case LK_D -> requireLength(values, 4, element);
            case LK_THYR, LK_IGBT -> requireLength(values, 9, element);
            case LK_U, LK_I -> {
                int kind = (int) values[0];
                if (kind != SourceType.QUELLE_DC_NEW && kind != SourceType.QUELLE_DC
                        && kind != SourceType.QUELLE_SIN_NEW && kind != SourceType.QUELLE_SIN
                        && kind != SourceType.QUELLE_SIGNALGESTEUERT_NEW
                        && kind != SourceType.QUELLE_SIGNALGESTEUERT) {
                    throw new IllegalArgumentException("DC operating point does not support the controlled "
                            + "source type " + kind + " of element " + element);
                }
            }
            default -> throw new IllegalArgumentException("DC operating point does not support element type "
                    + type + " (element " + element + ")");
        }
    }

    private static void requireLength(double[] values, int length, int element) {
        if (values.length < length) {
            throw new IllegalArgumentException("Element " + element + " has too few parameters for a DC solve");
        }
    }

    /** A capacitor between two conductive groups leaves their relative potential to charge, not to the DC solve. */
    private static void requireCapacitorsBetweenOneGroup(INetList netlist) {
        int[] components = new int[netlist.getNodeMax() + 1];
        Arrays.setAll(components, node -> node);
        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.LK_I) {
                continue;
            }
            components[componentRoot(components, netlist.getNodeY(i))] = componentRoot(components, netlist.getNodeX(i));
        }
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (netlist.getType(i) == CircuitTypCore.LK_C
                    && componentRoot(components, netlist.getNodeX(i)) != componentRoot(components, netlist.getNodeY(i))) {
                throw new IllegalStateException("DC operating point is undetermined: capacitor " + i
                        + " is the only connection between two node groups");
            }
        }
    }

    /** Flips the most inconsistent diode-family element until every state matches its bias. */
    private static void solveDiodeConsistent(MatrixSolver solver, CircuitNetlist network) {
        int diodes = 0;
        for (int i = 0; i < network.getElementCount(); i++) {
            diodes += isDiodeFamily(network.getType(i)) ? 1 : 0;
        }
        for (int iteration = 0; iteration <= 10 * diodes + 10; iteration++) {
            solver.buildMatrixA(network, 1, 0, false);
            solver.buildVectorB(network, 1, 0, false);
            try {
                solver.solve();
            } catch (RuntimeException failure) {
                throw new IllegalStateException("DC operating point has no unique solution "
                        + "(floating node, source loop or inductor loop): " + failure.getMessage(), failure);
            }
            double[] p = solver.getP();
            int worst = -1;
            double worstViolation = 0;
            for (int i = 0; i < network.getElementCount(); i++) {
                CircuitTypCore type = network.getType(i);
                if (!isDiodeFamily(type)) {
                    continue;
                }
                double[] values = network.getParameter(i);
                double voltage = p[network.getNodeX(i)] - p[network.getNodeY(i)];
                boolean gated = type == CircuitTypCore.LK_D || values[8] >= 0.5;
                // The thresholds of the transient diode iteration: a thyristor stays latched, an IGBT does not.
                double violation = values[0] < 10000
                        ? (type == CircuitTypCore.LK_IGBT && !gated ? Double.MAX_VALUE : Math.max(0, values[1] - voltage))
                        : (gated ? Math.max(0, voltage - values[1]) : 0);
                if (violation > worstViolation) {
                    worstViolation = violation;
                    worst = i;
                }
            }
            if (worst < 0) {
                return;
            }
            double[] values = network.getParameter(worst);
            values[0] = values[0] < 10000 ? values[3] : values[2];
        }
        throw new IllegalStateException("DC operating point diode states did not converge");
    }

    private static boolean isDiodeFamily(CircuitTypCore type) {
        return type == CircuitTypCore.LK_D || type == CircuitTypCore.LK_THYR || type == CircuitTypCore.LK_IGBT;
    }

    private static boolean hasInductorCutset(INetList netlist) {
        int[] components = new int[netlist.getNodeMax() + 1];
        Arrays.setAll(components, node -> node);
        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            if (isInductor(type) || type == CircuitTypCore.LK_I || type == CircuitTypCore.TH_FLOW) {
                continue;
            }
            int x = componentRoot(components, netlist.getNodeX(i));
            int y = componentRoot(components, netlist.getNodeY(i));
            components[y] = x;
        }
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (isInductor(netlist.getType(i))
                    && componentRoot(components, netlist.getNodeX(i))
                    != componentRoot(components, netlist.getNodeY(i))) {
                return true;
            }
        }
        return false;
    }

    private static int componentRoot(int[] components, int node) {
        while (components[node] != node) {
            components[node] = components[components[node]];
            node = components[node];
        }
        return node;
    }

    private static boolean isInductor(CircuitTypCore type) {
        return type == CircuitTypCore.LK_L || type == CircuitTypCore.NONLIN_REL
                || type == CircuitTypCore.LK_LKOP2;
    }

    private static boolean isDynamic(CircuitTypCore type) {
        return type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH
                || type == CircuitTypCore.LK_L || type == CircuitTypCore.NONLIN_REL
                || type == CircuitTypCore.LK_LKOP2;
    }

    private void validateState(MatrixSolver matrix, INetList netlist) {
        if (matrix == null || netlist == null || matrix.getP() == null || matrix.getIALT() == null) {
            throw new IllegalStateException("Initialized matrices and a netlist are required");
        }
        if (matrix.getSolverType() != solverType
                || (long) netlist.getNodeMax() + netlist.getVoltageSourceMax() + 1 != matrix.getMatrixSize()
                || netlist.getElementCount() != matrix.getIALT().length) {
            throw new IllegalArgumentException("Initialization dimensions and solver types must match");
        }
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (netlist.getType(i) == null || netlist.getParameter(i) == null
                    || netlist.getNodeX(i) < 0 || netlist.getNodeX(i) > netlist.getNodeMax()
                    || netlist.getNodeY(i) < 0 || netlist.getNodeY(i) > netlist.getNodeMax()) {
                throw new IllegalArgumentException("Invalid initialization element " + i);
            }
            int source = netlist.getVoltageSourceNumber(i);
            if (source > netlist.getVoltageSourceMax()) {
                throw new IllegalArgumentException("Invalid initialization auxiliary number for element " + i);
            }
        }
    }

    private static void validateSavedParameters(INetList netlist) {
        for (int i = 0; i < netlist.getElementCount(); i++) {
            int[] fields = switch (netlist.getType(i)) {
                case LK_C, TH_CTH -> new int[]{2, 4, 5};
                case REL_MMF, TH_TEMP -> new int[]{8, 9};
                case LK_L, NONLIN_REL, LK_LKOP2 -> new int[]{2};
                case LK_I, TH_FLOW -> new int[]{6};
                default -> new int[0];
            };
            double[] values = netlist.getParameter(i);
            for (int field : fields) {
                if (field >= values.length || !Double.isFinite(values[field])) {
                    throw new IllegalArgumentException("Invalid saved state for element " + i);
                }
            }
        }
    }

    private static void seedHistories(MatrixSolver matrix, double[] potentials, double[] currents) {
        for (double[] target : new double[][]{matrix.getP(), matrix.getPALT(),
                matrix.getPALTALT(), matrix.getPALTALTALT()}) {
            System.arraycopy(potentials, 0, target, 0, potentials.length);
        }
        for (double[] target : new double[][]{matrix.getIALT(), matrix.getIALTALT(), matrix.getIALTALTALT()}) {
            System.arraycopy(currents, 0, target, 0, currents.length);
        }
    }

    /**
     * Initializes all potential arrays to zero.
     *
     * @param matrixSolver the matrix solver
     */
    private void initializePotentials(MatrixSolver matrixSolver) {
        double[] pALT = matrixSolver.getPALT();
        double[] pALTALT = matrixSolver.getPALTALT();
        double[] pALTALTALT = matrixSolver.getPALTALTALT();

        for (int i = 0; i < pALT.length; i++) {
            pALT[i] = 0;
            pALTALT[i] = 0;
            pALTALTALT[i] = 0;
        }
    }

    /**
     * Initializes all current arrays to zero.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void initializeCurrents(MatrixSolver matrixSolver, INetList netlist) {
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();
        double[] iALTALTALT = matrixSolver.getIALTALTALT();

        for (int i = 0; i < iALT.length; i++) {
            iALT[i] = 0;
            iALTALT[i] = 0;
            iALTALTALT[i] = 0;
        }
    }

    /**
     * Restores potentials from netlist parameters (saved initial conditions).
     *
     * Handles capacitor voltages and other potential sources.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void restorePotentialsFromNetlist(MatrixSolver matrixSolver, INetList netlist) {
        double[] pALT = matrixSolver.getPALT();

        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            int nodeX = netlist.getNodeX(i);
            int nodeY = netlist.getNodeY(i);
            double[] params = netlist.getParameter(i);

            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH) {
                // Capacitor: restore initial voltage
                // param[4] = voltage at X node, param[5] = voltage at Y node
                pALT[nodeX] = params[4];
                pALT[nodeY] = params[5];
            } else if (type == CircuitTypCore.REL_MMF || type == CircuitTypCore.TH_TEMP) {
                // MMF/Temperature source: restore potentials
                // param[8] = voltage at X node, param[9] = voltage at Y node
                pALT[nodeX] = params[8];
                pALT[nodeY] = params[9];
            } else if (type == CircuitTypCore.LK_LKOP2) {
                // Coupled inductor: restore source current in potential vector
                // param[2] = source current
                int voltageSourceNumber = netlist.getVoltageSourceNumber(i);
                if (voltageSourceNumber > 0) {
                    pALT[netlist.getNodeMax() + voltageSourceNumber] = params[2];
                }
            }
        }
    }

    /**
     * Restores currents from netlist parameters (saved initial conditions).
     *
     * Handles inductor currents, current sources, and coupled inductors.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void restoreCurrentsFromNetlist(MatrixSolver matrixSolver, INetList netlist) {
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();

        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            double[] params = netlist.getParameter(i);

            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH) {
                // Capacitor: restore initial current
                // param[2] = initial current
                iALT[i] = params[2];
            } else if (type == CircuitTypCore.LK_L || type == CircuitTypCore.NONLIN_REL) {
                // Inductor: restore initial current
                // param[1] = initial current (from dialog), param[2] = saved current
                iALT[i] = params[2];
                iALTALT[i] = iALT[i];
            } else if (type == CircuitTypCore.LK_LKOP2) {
                // Coupled inductor: restore initial current
                iALT[i] = params[2];
                iALTALT[i] = iALT[i];
            } else if (type == CircuitTypCore.LK_I || type == CircuitTypCore.TH_FLOW) {
                // Current source: restore initial current
                // param[6] = saved current value
                iALT[i] = params[6];
            }
        }
    }

    /**
     * Calculates the inductance coefficient for matrix stamping.
     *
     * Extracted from LKMatrices.getAWForInductance (lines 1504-1527).
     * Calculates the coefficient that relates inductance to matrix elements
     * for different numerical integration methods.
     *
     * @param inductance the inductance value
     * @param timeStep the simulation time step (dt)
     * @return the calculated coefficient aW for matrix stamping
     */
    public double getAWForInductance(double inductance, double timeStep) {
        if (!Double.isFinite(inductance) || inductance < 0
                || !Double.isFinite(timeStep) || timeStep <= 0) {
            throw new IllegalArgumentException("Inductance must be finite and non-negative; time step must be finite and positive");
        }
        double aW = 0;

        if (inductance < FAST_NULL_L) {
            // Inductance is near zero - use threshold value
            if (solverType == SolverType.SOLVER_BE) {
                aW = timeStep / FAST_NULL_L;
            } else if (solverType == SolverType.SOLVER_TRZ) {
                aW = 0.5 * timeStep / FAST_NULL_L;
            } else if (solverType == SolverType.SOLVER_GS) {
                aW = 2.0 / 3.0 * (timeStep / FAST_NULL_L);
            }
        } else {
            // Normal inductance value
            if (solverType == SolverType.SOLVER_BE) {
                aW = timeStep / inductance;
            } else if (solverType == SolverType.SOLVER_TRZ) {
                aW = 0.5 * timeStep / inductance;
            } else if (solverType == SolverType.SOLVER_GS) {
                aW = 2.0 / 3.0 * (timeStep / inductance);
            }
        }

        if (!Double.isFinite(aW)) {
            throw new IllegalArgumentException("Inductance coefficient is not finite");
        }
        return aW;
    }
}

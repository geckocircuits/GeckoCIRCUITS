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
package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.INetList;
import gecko.core.preparation.SolverFeatures;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.InitialConditionSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Owns the converged electrical solution and its solver histories.
 * {@link #advance} commits one grid step as a single history update. With zero-crossing location on,
 * {@link #advanceInterval} commits one history update per sub-step instead. In both cases the grid step
 * publishes its results, probe values and trace once, at the grid time.
 */
final class ElectricalTimeStep {

    private final CircuitNetlist netlist;
    private final INetList numericalNetlist;
    private final int[] nodeOrder;
    private final double[] publicPotentials;
    private final MatrixSolver matrixSolver;
    private final ComponentCurrentCalculator componentCurrentCalculator = new ComponentCurrentCalculator();
    private final double[] computedCurrents;
    private final double[][] matrixInputs;
    private double matrixDt = Double.NaN;

    /** Most diode-family events located by splitting inside one grid step. */
    static final int MAX_LOCATED_PER_STEP = 16;
    static final int MAX_RETAINED_EVENTS = 100_000;
    private static final double BOUNDARY_FRACTION = 1e-3;
    private static final int REFINEMENTS = 2;
    private final boolean zeroCrossing;
    private final int[] diodeElements;
    private final TransientSemiconductors nonlinearDevices;
    private final int[] sourceElements;
    private final double[] savedDiodeStates;
    private final double[] savedSourceStates;
    private final boolean[] locatedInStep;
    private final boolean[] accountedOn;
    private final boolean[] justToggled;
    private final boolean[] gatePrevious;
    private final int[] stagedBySlot;
    private final List<StagedEvent> staged = new ArrayList<>();
    private final double[] startMargin;
    private final double[] trialMargin;
    private final ZeroCrossingDetector detector = new ZeroCrossingDetector();
    private final List<ZeroCrossingEvent> events = new ArrayList<>();
    private int eventRetention = MAX_RETAINED_EVENTS;
    private long locatedCount;
    private long unlocatedCount;
    private long droppedCount;
    private double firstUnlocatedTime = Double.NaN;
    private boolean committedOnce;

    // Adaptive sub-step state. Histories, restart state and margins change only at a committed sub-step.
    // The retry step and the rejected counter also change while a trial is retried.
    private static final double VOLT_FLOOR = 1e-6;
    private static final double AMPERE_FLOOR = 1e-9;
    private static final int MAX_RETRIES = 30;
    private static final double SAFETY = 0.9;
    /** Relative slack when a grid interval is compared with the largest step, so rounding cannot split a full step. */
    private static final double LANDING_TOLERANCE = 1e-9;
    private final AdaptiveStepSettings adaptive;
    private final MatrixSolver companionSolver;
    private final MatrixSolver.Backend initializationBackend;
    private final long initializationStoredCoefficients;
    private final MatrixSolver.SparseFactorization initializationFactorization;
    private final int[] discontinuityElements;
    private final double[] discontinuityValues;
    private final double[] committedMargin;
    private final double[] previousMargin;
    private double committedMarginTime = Double.NaN;
    private double previousMarginTime = Double.NaN;
    private double controllerStep = Double.NaN;
    private boolean restartPending = true;
    private long acceptedSubSteps;
    private long rejectedSubSteps;
    private long forcedSubSteps;
    private double shortestSubStep = Double.NaN;
    private double longestSubStep = Double.NaN;
    private SubStepListener subStepListener;
    /** The last grid time that the adaptive step published. NaN before the first one. */
    private double publishedGridTime = Double.NaN;

    /** Receives each accepted sub-step in order. Tests use it to stream, so production code stores nothing. */
    interface SubStepListener {
        /**
         * @param start the sub-step start time
         * @param width the sub-step width
         * @param error the normalized error estimate, 0 when none exists
         * @param forced true when the estimate exceeded 1 and the step was accepted anyway
         * @param backwardEuler true when the sub-step used Backward Euler as a restart
         */
        void accepted(double start, double width, double error, boolean forced, boolean backwardEuler);
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType) {
        this(netlist, solverType, false);
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType, boolean zeroCrossing) {
        this(netlist, solverType, new SolverFeatures(0, zeroCrossing));
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType, SolverFeatures features) {
        this(netlist, solverType, features, false);
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType, SolverFeatures features, boolean uic) {
        this(netlist, solverType, features, uic, null);
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType, SolverFeatures features, boolean uic,
            TransientSemiconductors nonlinearDevices) {
        this.nonlinearDevices = nonlinearDevices;
        if (nonlinearDevices != null && (solverType == SolverType.SOLVER_GS || features.dcOperatingPoint() || uic)) {
            throw new IllegalArgumentException("Use backward Euler or trapezoidal integration without DC start or UIC for transient semiconductor models.");
        }
        this.adaptive = features.adaptiveStep();
        if (adaptive != null && solverType == SolverType.SOLVER_GS) {
            throw new IllegalArgumentException(SimulationTiming.GS_ADAPTIVE_MESSAGE);
        }
        this.zeroCrossing = features.zeroCrossing() || adaptive != null;
        this.netlist = netlist;
        nodeOrder = netlist.getNumericalNodeOrder();
        numericalNetlist = nodeOrder.length == 0 && nonlinearDevices == null
                ? netlist : new NumericalNetlist(netlist, nodeOrder, nonlinearDevices);
        matrixSolver = new MatrixSolver(solverType, features.sparseSolver());
        matrixSolver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());
        computedCurrents = new double[netlist.getElementCount()];
        matrixInputs = new double[netlist.getElementCount()][4];
        publicPotentials = new double[matrixSolver.getMatrixSize()];
        InitialConditionSolver initialConditions = new InitialConditionSolver(solverType);
        if (features.dcOperatingPoint()) initialConditions.setDcOperatingPoint(matrixSolver, numericalNetlist);
        else if (uic) initialConditions.setConstrainedInitialConditions(matrixSolver, numericalNetlist);
        else initialConditions.setDialogInitialConditions(matrixSolver, numericalNetlist);
        initializationBackend = initialConditions.getLastSolverBackend();
        initializationStoredCoefficients = initialConditions.getLastSolverStoredCoefficients();
        initializationFactorization = initialConditions.getLastSolverFactorization();
        int[] diodes = new int[netlist.getElementCount()];
        int[] sources = new int[netlist.getElementCount()];
        int diodeCount = 0;
        int sourceCount = 0;
        for (int element = 0; element < diodes.length; element++) {
            CircuitTypCore type = netlist.getType(element);
            if (isDiodeFamily(type) && !isNonlinearDevice(element)) diodes[diodeCount++] = element;
            else if (type == CircuitTypCore.LK_U && netlist.getParameter(element).length > 14) sources[sourceCount++] = element;
        }
        diodeElements = Arrays.copyOf(diodes, diodeCount);
        sourceElements = Arrays.copyOf(sources, sourceCount);
        savedDiodeStates = new double[diodeCount];
        savedSourceStates = new double[sourceCount];
        locatedInStep = new boolean[diodeCount];
        accountedOn = new boolean[diodeCount];
        justToggled = new boolean[diodeCount];
        gatePrevious = new boolean[diodeCount];
        stagedBySlot = new int[diodeCount];
        startMargin = new double[diodeCount];
        trialMargin = new double[diodeCount];
        for (int slot = 0; slot < diodeCount; slot++) gatePrevious[slot] = gateOn(slot);
        if (adaptive == null) {
            companionSolver = null;
            discontinuityElements = new int[0];
            discontinuityValues = new double[0];
            committedMargin = previousMargin = null;
        } else {
            companionSolver = new MatrixSolver(solverType == SolverType.SOLVER_BE ? SolverType.SOLVER_TRZ : SolverType.SOLVER_BE,
                    features.sparseSolver());
            companionSolver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
            int[] watched = new int[netlist.getElementCount()];
            int watchedCount = 0;
            for (int element = 0; element < watched.length; element++) {
                double value = discontinuityValue(element);
                if (!Double.isNaN(value)) watched[watchedCount++] = element;
            }
            discontinuityElements = Arrays.copyOf(watched, watchedCount);
            discontinuityValues = new double[watchedCount];
            committedMargin = new double[diodeCount];
            previousMargin = new double[diodeCount];
            rememberDiscontinuityInputs();
        }
    }

    /** A located event that enters the log only when its grid step ends and still holds. */
    private record StagedEvent(int slot, double time, boolean turnOn, double before, double after) { }

    private static boolean isDiodeFamily(CircuitTypCore type) {
        return type == CircuitTypCore.LK_D || type == CircuitTypCore.LK_THYR || type == CircuitTypCore.LK_IGBT;
    }

    void advance(double dt, double time, HeadlessSimulationEngine.StepTraceListener traceListener) {
        if (!Double.isFinite(dt) || dt <= 0 || !Double.isFinite(time) || time < 0) {
            throw new IllegalArgumentException("Time step must be finite and positive; time must be finite and non-negative");
        }
        solveStartTopology(matrixSolver, dt, time);
        doDiodeIteration(numericalNetlist, dt, time);

        // Compute against previous-step histories before committing the new state.
        componentCurrentCalculator.calculateComponentCurrents(
                matrixSolver, numericalNetlist, computedCurrents, 0.0, dt, time, true);
        commitNonlinearDevices(matrixSolver, dt);
        matrixSolver.shiftHistory();
        double[] currents = matrixSolver.getIALT();
        System.arraycopy(computedCurrents, 0, currents, 0, computedCurrents.length);
        double[] potentials = matrixSolver.getPALT();
        System.arraycopy(potentials, 0, publicPotentials, 0, potentials.length);
        if (nodeOrder.length != 0) {
            double reference = potentials[nodeOrder[0]];
            for (int node = 0; node < nodeOrder.length; node++) publicPotentials[node] = potentials[nodeOrder[node]] - reference;
        }
        netlist.storeResults(publicPotentials, currents);
        rememberMatrixInputs(dt);

        if (traceListener != null) {
            traceListener.onStep(time, publicPotentials, currents, netlist);
        }
    }

    /**
     * Advances one grid step like {@link #advance}, but splits the interval at diode-family switching times.
     * Each sub-step commits its solver histories once. Results and the trace publish once, at the grid time.
     * A trial may change only diode-family parameter 0, source-limit parameter 14 and matrix scratch,
     * and all three are restored before the split sub-step runs.
     *
     * <p>Every diode-family state change that a committed sub-step holds is either a located event or
     * counted as unlocated. Gate-driven edges stay on the grid and are in neither group.
     */
    void advanceInterval(double dt, double time, HeadlessSimulationEngine.StepTraceListener traceListener) {
        if (!zeroCrossing) {
            advance(dt, time, traceListener);
            return;
        }
        if (!Double.isFinite(dt) || dt <= 0 || !Double.isFinite(time) || time < 0) {
            throw new IllegalArgumentException("Time step must be finite and positive. Time must be finite and non-negative.");
        }
        if (adaptive != null) {
            advanceAdaptive(dt, time, traceListener);
            return;
        }
        double start = time - dt;
        if (committedOnce && start >= 0 && start < time) {
            locateWithin(start, dt, time);
            flushStaged();
        } else {
            solveFree(dt, time);
            commitSubStep(dt, time);
        }
        for (int slot = 0; slot < diodeElements.length; slot++) gatePrevious[slot] = gateOn(slot);
        publishGrid(time, traceListener);
    }

    private void locateWithin(double start, double dt, double time) {
        double current = start;
        double epsilon = BOUNDARY_FRACTION * dt;
        int locatedThisStep = 0;
        boolean wholeStep = true;
        Arrays.fill(locatedInStep, false);
        Arrays.fill(justToggled, false);
        Arrays.fill(stagedBySlot, -1);
        staged.clear();
        for (int slot = 0; slot < diodeElements.length; slot++) accountedOn[slot] = isOn(slot);
        while (true) {
            double width = wholeStep ? dt : time - current;
            saveTrialState();
            computeThresholdMargin(matrixSolver.getPALT(), startMargin);
            solveStartTopology(width, time);
            computeThresholdMargin(matrixSolver.getP(), trialMargin);
            doDiodeIteration(numericalNetlist, width, time, null);
            boolean[] candidate = new boolean[diodeElements.length];
            boolean any = false;
            boolean capReached = locatedThisStep >= MAX_LOCATED_PER_STEP;
            for (int slot = 0; slot < diodeElements.length; slot++) {
                if (capReached || locatedInStep[slot] || !toggled(slot) || !(startMargin[slot] * trialMargin[slot] < 0)
                        || gateDriven(slot, isOff(savedDiodeStates[slot]))) continue;
                candidate[slot] = true;
                any = true;
            }
            if (!any) {
                commitSubStep(width, time);
                reconcile(time);
                return;
            }
            double[] crossing = new double[diodeElements.length];
            int best = -1;
            for (int slot = 0; slot < candidate.length; slot++) {
                if (!candidate[slot]) continue;
                crossing[slot] = refine(matrixSolver, slot, startMargin[slot], trialMargin[slot], current, time, epsilon);
                if (best < 0 || crossing[slot] < crossing[best]) best = slot;
            }
            double eventTime = crossing[best];
            // Candidates that cross together, such as two series devices of a bridge, switch together.
            boolean[] together = new boolean[diodeElements.length];
            int count = 0;
            for (int slot = 0; slot < candidate.length && locatedThisStep + count < MAX_LOCATED_PER_STEP; slot++) {
                if (candidate[slot] && crossing[slot] - eventTime <= epsilon) { together[slot] = true; count++; }
            }
            restoreTrialState();
            if (eventTime >= time - epsilon) {
                // Every candidate crosses at the grid point, so the free trial is the committed step.
                for (int slot = 0; slot < candidate.length; slot++) if (together[slot]) accept(slot, eventTime);
                solveFree(width, time);
                commitSubStep(width, time);
                reconcile(time);
                return;
            }
            if (eventTime > current + epsilon) {
                boolean[] pinned = new boolean[netlist.getElementCount()];
                for (int slot = 0; slot < together.length; slot++) if (together[slot]) pinned[diodeElements[slot]] = true;
                subStep(eventTime - current, eventTime, pinned);
                reconcile(eventTime);
                current = eventTime;
                wholeStep = false;
            }
            for (int slot = 0; slot < together.length; slot++) {
                if (!together[slot]) continue;
                accept(slot, eventTime);
                toggleDiode(slot);
            }
            locatedThisStep += count;
        }
    }

    /** Accepts a located event: the slot's expected state flips, and the log takes it when the step ends. */
    private void accept(int slot, double eventTime) {
        locatedInStep[slot] = true;
        stagedBySlot[slot] = staged.size();
        staged.add(new StagedEvent(slot, eventTime, isOff(savedDiodeStates[slot]), startMargin[slot], trialMargin[slot]));
        accountedOn[slot] = !accountedOn[slot];
        justToggled[slot] = true;
    }

    /**
     * Compares each committed state with the state the located events expect. A device that returned to
     * its old state loses its event and counts as unlocated. Any other unexpected change counts as
     * unlocated unless a gate edge explains it.
     */
    private void reconcile(double time) {
        int unexplained = 0;
        for (int slot = 0; slot < diodeElements.length; slot++) {
            boolean on = isOn(slot);
            if (on != accountedOn[slot]) {
                if (justToggled[slot] && stagedBySlot[slot] >= 0) {
                    int index = stagedBySlot[slot];
                    staged.remove(index);
                    stagedBySlot[slot] = -1;
                    for (int other = 0; other < stagedBySlot.length; other++) if (stagedBySlot[other] > index) stagedBySlot[other]--;
                    unexplained++;
                } else if (!gateDriven(slot, on)) {
                    unexplained++;
                }
                accountedOn[slot] = on;
            }
            justToggled[slot] = false;
        }
        countUnlocated(unexplained, time);
    }

    private void flushStaged() {
        for (StagedEvent event : staged) recordEvent(event);
        staged.clear();
    }

    private boolean isOn(int slot) { return !isOff(netlist.getParameter(diodeElements[slot])[0]); }

    private static boolean isOff(double resistance) { return resistance > 10000; }

    private boolean gateOn(int slot) { return netlist.getParameter(diodeElements[slot])[8] >= 0.5; }

    /**
     * A gate edge switches a device on the grid point. It is a grid event, not a missed crossing:
     * a gate that rises turns a thyristor or IGBT on, and a removed IGBT gate turns it off.
     * A device with a gate that stays high is not gate-driven, so its crossing must be located.
     */
    private boolean gateDriven(int slot, boolean turnsOn) {
        CircuitTypCore type = netlist.getType(diodeElements[slot]);
        if (type == CircuitTypCore.LK_D) return false;
        if (turnsOn) return gateOn(slot) && !gatePrevious[slot];
        return type == CircuitTypCore.LK_IGBT && !gateOn(slot);
    }

    /** Solves, iterates with the pinned elements held (null for none), and commits histories once. */
    private void subStep(double width, double time, boolean[] pinned) {
        solveStartTopology(width, time);
        doDiodeIteration(numericalNetlist, width, time, pinned);
        commitSubStep(width, time);
    }

    /** Solves and iterates like the committed step does, without recording anything. */
    private void solveFree(double width, double time) {
        solveStartTopology(width, time);
        doDiodeIteration(numericalNetlist, width, time, null);
    }

    private void solveStartTopology(double width, double time) { solveStartTopology(matrixSolver, width, time); }

    private void solveStartTopology(MatrixSolver solver, double width, double time) {
        if (nonlinearDevices != null) {
            nonlinearDevices.solve(solver, numericalNetlist, width, time);
            return;
        }
        if (!numericalNetlist.usesWireReference() || matrixInputsChanged(width)) {
            solver.buildMatrixA(numericalNetlist, width, time, false);
        }
        solver.buildVectorB(numericalNetlist, width, time, false);
        solver.solve();
    }

    /** Commits histories for one sub-step. Public values stay unchanged until {@link #publishGrid}. */
    private void commitSubStep(double width, double time) {
        componentCurrentCalculator.calculateComponentCurrents(
                matrixSolver, numericalNetlist, computedCurrents, 0.0, width, time, true);
        commitNonlinearDevices(matrixSolver, width);
        matrixSolver.shiftHistory();
        System.arraycopy(computedCurrents, 0, matrixSolver.getIALT(), 0, computedCurrents.length);
        rememberMatrixInputs(width);
        committedOnce = true;
    }

    void publishGrid(double time, HeadlessSimulationEngine.StepTraceListener traceListener) {
        double[] currents = matrixSolver.getIALT();
        double[] potentials = matrixSolver.getPALT();
        System.arraycopy(potentials, 0, publicPotentials, 0, potentials.length);
        if (nodeOrder.length != 0) {
            double reference = potentials[nodeOrder[0]];
            for (int node = 0; node < nodeOrder.length; node++) publicPotentials[node] = potentials[nodeOrder[node]] - reference;
        }
        netlist.storeResults(publicPotentials, currents);
        if (traceListener != null) traceListener.onStep(time, publicPotentials, currents, netlist);
    }

    private void saveTrialState() {
        for (int slot = 0; slot < diodeElements.length; slot++) savedDiodeStates[slot] = netlist.getParameter(diodeElements[slot])[0];
        for (int slot = 0; slot < sourceElements.length; slot++) savedSourceStates[slot] = netlist.getParameter(sourceElements[slot])[14];
    }

    /** Restores every parameter a trial may change and forces the next solve to rebuild the matrix. */
    private void restoreTrialState() {
        for (int slot = 0; slot < diodeElements.length; slot++) netlist.getParameter(diodeElements[slot])[0] = savedDiodeStates[slot];
        for (int slot = 0; slot < sourceElements.length; slot++) netlist.getParameter(sourceElements[slot])[14] = savedSourceStates[slot];
        matrixDt = Double.NaN;
    }

    private boolean toggled(int slot) {
        return netlist.getParameter(diodeElements[slot])[0] != savedDiodeStates[slot];
    }

    private void toggleDiode(int slot) {
        double[] parameters = netlist.getParameter(diodeElements[slot]);
        // An element that was off turns on at the event, and the reverse.
        parameters[0] = isOff(savedDiodeStates[slot]) ? parameters[2] : parameters[3];
        matrixDt = Double.NaN;
    }

    /** Fills the threshold quantity {@code vX - vY - Uf} that the diode iteration compares. */
    private void computeThresholdMargin(double[] potentials, double[] target) {
        for (int slot = 0; slot < diodeElements.length; slot++) {
            int element = diodeElements[slot];
            target[slot] = potentials[numericalNetlist.getNodeX(element)] - potentials[numericalNetlist.getNodeY(element)]
                    - netlist.getParameter(element)[1];
        }
    }

    /** Refines the crossing time with regula falsi trials that keep the start topology and do not iterate. */
    private double refine(MatrixSolver solver, int slot, double gLow, double gHigh, double start, double end, double epsilon) {
        double low = start;
        double high = end;
        double crossing = detector.detectCrossings(new double[]{gLow}, new double[]{gHigh}, 0, start, end)
                .getFirst().getCrossingTime();
        double[] probe = new double[diodeElements.length];
        for (int round = 0; round < REFINEMENTS; round++) {
            if (!(crossing > start + epsilon && crossing < end - epsilon)) break;
            restoreTrialState();
            solveStartTopology(solver, crossing - start, crossing);
            computeThresholdMargin(solver.getP(), probe);
            double value = probe[slot];
            if (value == 0) break;
            if (value * gLow > 0) { low = crossing; gLow = value; } else { high = crossing; gHigh = value; }
            double next = low - gLow * (high - low) / (gHigh - gLow);
            if (!(next > low && next < high)) break;
            crossing = next;
        }
        return Math.max(start, Math.min(end, crossing));
    }

    private void recordEvent(StagedEvent event) {
        locatedCount++;
        if (events.size() >= eventRetention) { droppedCount++; return; }
        events.add(new ZeroCrossingEvent(diodeElements[event.slot()], event.time(),
                event.turnOn() ? ZeroCrossingEvent.Direction.RISING : ZeroCrossingEvent.Direction.FALLING,
                event.before(), event.after()));
    }

    private void countUnlocated(int transitions, double time) {
        if (transitions == 0) return;
        unlocatedCount += transitions;
        if (Double.isNaN(firstUnlocatedTime)) firstUnlocatedTime = time;
    }

    /**
     * Exports the physical electrical state that the next step reads, after a committed grid step. It only reads.
     * Solver histories come first, then capacitor currents, then the discrete states, which compare exactly.
     * The derived matrix cache, the clock and the event counters are not state and are not exported.
     * The adaptive controller step, restart flag, margin trend and statistics are numerical controller state,
     * not physical state, so they are not exported (ADR-0018).
     */
    void exportPeriodicState(PeriodicStateMonitor.Sink sink) {
        double[] potentials = matrixSolver.getPALT();
        int nodes = Math.min(netlist.getNodeMax() + 1, potentials.length);
        boolean secondOrder = matrixSolver.getSolverType() == SolverType.SOLVER_GS;
        exportPotentials(sink, potentials, nodes, "potential history");
        exportCurrents(sink, matrixSolver.getIALT(), "element current history");
        if (secondOrder) {
            exportPotentials(sink, matrixSolver.getPALTALT(), nodes, "potential history two steps back");
            exportCurrents(sink, matrixSolver.getIALTALT(), "element current history two steps back");
        }
        sink.group(PeriodicStateMonitor.Unit.AMPERE, "capacitor current");
        for (int element = 0; element < netlist.getElementCount(); element++) {
            double[] parameters = netlist.getParameter(element);
            if (netlist.getType(element) == CircuitTypCore.LK_C && parameters.length > 10) sink.value(parameters[10]);
        }
        sink.group(PeriodicStateMonitor.Unit.DISCRETE, "switch state");
        for (int element = 0; element < netlist.getElementCount(); element++) {
            CircuitTypCore type = netlist.getType(element);
            double[] parameters = netlist.getParameter(element);
            if (isDiodeFamily(type) || type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_MOSFET) {
                sink.value(parameters[0]);
                if (parameters.length > 8) sink.flag(parameters[8] >= 0.5);
            } else if (type == CircuitTypCore.LK_U && parameters.length > 14) {
                sink.value(parameters[14]);
            }
        }
    }

    private static void exportPotentials(PeriodicStateMonitor.Sink sink, double[] history, int nodes, String name) {
        sink.group(PeriodicStateMonitor.Unit.VOLT, "node " + name);
        for (int index = 0; index < nodes; index++) sink.value(history[index]);
        sink.group(PeriodicStateMonitor.Unit.AMPERE, "branch current " + name);
        for (int index = nodes; index < history.length; index++) sink.value(history[index]);
    }

    private static void exportCurrents(PeriodicStateMonitor.Sink sink, double[] history, String name) {
        sink.group(PeriodicStateMonitor.Unit.AMPERE, name);
        for (double value : history) sink.value(value);
    }


    // ---- Adaptive electrical sub-steps (ADR-0018) ----

    /**
     * Advances one grid step with error-controlled sub-steps that land exactly on the grid time.
     * A trial changes only the diode-family parameter 0, the source-limit parameter 14 and solver scratch.
     * Both are restored on a rejection. Histories, restart state and the accepted, forced and width statistics
     * change only at a commit. The rejected counter and the retry step change on each LTE rejection.
     * CONTROL, results and the trace see only the grid step.
     *
     * <p>The interval spans from the last published grid time to {@code time}. After a step change that gap
     * differs from {@code dt}, which stays the nominal step for the width clamps.
     */
    private void advanceAdaptive(double dt, double time, HeadlessSimulationEngine.StepTraceListener traceListener) {
        if (!Double.isNaN(publishedGridTime) && !(time > publishedGridTime)) {
            throw new IllegalArgumentException("The adaptive grid time must advance past " + publishedGridTime + ".");
        }
        double start = Double.isNaN(publishedGridTime) ? time - dt : publishedGridTime;
        double stepMax = Math.min(adaptive.resolvedMaxStep(dt), dt);
        double stepMin = Math.min(adaptive.resolvedMinStep(dt), stepMax);
        if (Double.isNaN(controllerStep)) controllerStep = stepMin;
        if (!committedOnce || !(start >= 0 && start < time)) {
            // The first integration at the start time uses the smallest step, so its offset from the exact start is bounded.
            trialLoop(time, stepMin, time, stepMin, stepMax, 0, true);
        } else {
            beginGridStep();
            double current = start;
            int located = 0;
            while (time - current > 0) {
                double remaining = time - current;
                controllerStep = Math.max(stepMin, Math.min(stepMax, controllerStep));
                double width = Math.min(controllerStep, remaining);
                // Land exactly on the grid: absorb a leftover below the minimum, or split what a full step cannot cover.
                boolean fits = remaining <= stepMax * (1 + LANDING_TOLERANCE);
                if (remaining - width < stepMin && fits) width = remaining;
                else if (remaining - width < stepMin) width = remaining / 2;
                SubStepOutcome outcome = trialLoop(current, width, time, stepMin, stepMax, located, false);
                current = outcome.end();
                located = outcome.located();
            }
            flushStaged();
        }
        for (int slot = 0; slot < diodeElements.length; slot++) gatePrevious[slot] = gateOn(slot);
        rememberDiscontinuityInputs();
        publishedGridTime = time;
        publishGrid(time, traceListener);
    }

    private record SubStepOutcome(double end, int located) { }

    private void beginGridStep() {
        Arrays.fill(locatedInStep, false);
        Arrays.fill(justToggled, false);
        Arrays.fill(stagedBySlot, -1);
        staged.clear();
        for (int slot = 0; slot < diodeElements.length; slot++) accountedOn[slot] = isOn(slot);
        if (discontinuityChanged()) restartPending = true;
    }

    /**
     * Runs trials of one sub-step until one commits, or until a crossing at the start time switches a device.
     *
     * @param startTime the committed time where the sub-step starts
     * @param width the first trial width
     * @param gridTime the grid time that ends the current grid step
     * @param located the located-event count of the grid step so far
     * @param first true for the first integration, which has no estimate and no event location
     * @return the committed end time and the located-event count of the grid step so far
     */
    private SubStepOutcome trialLoop(double startTime, double width, double gridTime, double stepMin, double stepMax,
            int located, boolean first) {
        int retries = 0;
        boolean[] pinned = null;
        boolean[] eventSlots = null;
        double eventTime = Double.NaN;
        double[] eventBefore = null;
        double[] eventAfter = null;
        boolean skipDetection = first;
        if (!first) width = predictCrossing(startTime, width, stepMin);
        while (true) {
            double remaining = gridTime - startTime;
            // Avoid a rounding-size final step after many sub-steps. A located event keeps its exact width.
            double endTolerance = nonlinearDevices != null && pinned == null ? 1e-8 * stepMin : 0;
            double end = first || remaining - width <= endTolerance ? gridTime : startTime + width;
            double h = first ? width : end - startTime;
            boolean restartTrial = first || restartPending;
            MatrixSolver used = restartTrial && matrixSolver.getSolverType() != SolverType.SOLVER_BE ? companionSolver : matrixSolver;
            MatrixSolver other = used == matrixSolver ? companionSolver : matrixSolver;
            saveTrialState();
            copyHistories(companionSolver);
            if (!first) computeThresholdMargin(matrixSolver.getPALT(), startMargin);
            try {
                solveFresh(used, h, end);
                if (!first) computeThresholdMargin(used.getP(), trialMargin);
                doDiodeIteration(used, numericalNetlist, h, end, pinned);
                if (!skipDetection && pinned == null) {
                    boolean[] candidate = new boolean[diodeElements.length];
                    boolean any = false;
                    boolean capReached = located >= MAX_LOCATED_PER_STEP;
                    for (int slot = 0; slot < diodeElements.length; slot++) {
                        if (capReached || locatedInStep[slot] || !toggled(slot) || !(startMargin[slot] * trialMargin[slot] < 0)
                                || gateDriven(slot, isOff(savedDiodeStates[slot]))) continue;
                        candidate[slot] = true;
                        any = true;
                    }
                    if (any) {
                        double epsilon = BOUNDARY_FRACTION * h;
                        double[] crossing = new double[diodeElements.length];
                        int best = -1;
                        for (int slot = 0; slot < candidate.length; slot++) {
                            if (!candidate[slot]) continue;
                            crossing[slot] = refine(used, slot, startMargin[slot], trialMargin[slot], startTime, end, epsilon);
                            if (best < 0 || crossing[slot] < crossing[best]) best = slot;
                        }
                        eventTime = crossing[best];
                        eventBefore = startMargin.clone();
                        eventAfter = trialMargin.clone();
                        // Candidates that cross together, such as two series devices of a bridge, switch together.
                        boolean[] together = new boolean[diodeElements.length];
                        int count = 0;
                        for (int slot = 0; slot < candidate.length && located + count < MAX_LOCATED_PER_STEP; slot++) {
                            if (candidate[slot] && crossing[slot] - eventTime <= epsilon) { together[slot] = true; count++; }
                        }
                        restoreTrialState();
                        eventSlots = together;
                        if (eventTime >= end - epsilon) {
                            // Every candidate crosses at the end of this sub-step, so the free trial is the step.
                            skipDetection = true;
                            continue;
                        }
                        if (eventTime > startTime + epsilon) {
                            pinned = new boolean[netlist.getElementCount()];
                            for (int slot = 0; slot < together.length; slot++) if (together[slot]) pinned[diodeElements[slot]] = true;
                            width = eventTime - startTime;
                            continue;
                        }
                        // The crossing is at the start: the device switches there, with no sub-step.
                        for (int slot = 0; slot < together.length; slot++) {
                            if (!together[slot]) continue;
                            accept(slot, eventTime);
                            toggleDiode(slot);
                        }
                        restartPending = true;
                        return new SubStepOutcome(startTime, located + count);
                    }
                }
                double error = 0;
                boolean forced = false;
                if (!first) {
                    solveFresh(other, h, end);
                    error = estimateError(used, other, h);
                    forced = error > 1;
                    if (!(error <= 1 || h <= stepMin || retries >= MAX_RETRIES)) {
                        restoreTrialState();
                        rejectedSubSteps++;
                        retries++;
                        // A shorter step can move an event to a later sub-step, so location repeats there.
                        width = Math.max(stepMin, Math.min(h * SAFETY * Math.sqrt(1 / error), h * SAFETY));
                        pinned = null;
                        eventSlots = null;
                        skipDetection = false;
                        continue;
                    }
                }
                // Accept the trial.
                boolean[] atEnd = pinned == null ? eventSlots : null;
                int newlyLocated = 0;
                if (atEnd != null) {
                    for (int slot = 0; slot < atEnd.length; slot++) if (atEnd[slot]) { accept(slot, end); newlyLocated++; }
                }
                boolean switched = changedInTrial();
                commitUsed(used, h, end);
                recordSubStep(startTime, h, error, forced, used.getSolverType() == SolverType.SOLVER_BE);
                reconcile(end);
                if (first) {
                    restartPending = true;
                } else if (pinned != null) {
                    System.arraycopy(eventBefore, 0, startMargin, 0, startMargin.length);
                    System.arraycopy(eventAfter, 0, trialMargin, 0, trialMargin.length);
                    for (int slot = 0; slot < eventSlots.length; slot++) {
                        if (!eventSlots[slot]) continue;
                        accept(slot, eventTime);
                        toggleDiode(slot);
                        newlyLocated++;
                    }
                    restartPending = true;
                    controllerStep = Math.max(stepMin, Math.min(controllerStep, gridTime - end));
                } else {
                    restartPending = switched || atEnd != null;
                    double growth = error <= 0 ? Double.POSITIVE_INFINITY : SAFETY * Math.sqrt(1 / error);
                    controllerStep = Math.min(stepMax, Math.max(stepMin, h * growth));
                }
                updateMarginHistory(end);
                return new SubStepOutcome(end, located + newlyLocated);
            } catch (TransientSemiconductors.Nonconvergence failure) {
                restoreTrialState();
                if (first || h <= stepMin || retries >= MAX_RETRIES) throw failure;
                rejectedSubSteps++;
                retries++;
                width = Math.max(stepMin, h / 2);
                pinned = null;
                eventSlots = null;
                skipDetection = false;
            }
        }
    }

    /** Shortens the next sub-step so that it ends where a committed margin trend predicts a diode crossing. */
    private double predictCrossing(double startTime, double width, double stepMin) {
        if (Double.isNaN(previousMarginTime) || committedMarginTime == previousMarginTime) return width;
        double span = committedMarginTime - previousMarginTime;
        double target = width;
        for (int slot = 0; slot < diodeElements.length; slot++) {
            if (locatedInStep[slot]) continue;
            CircuitTypCore type = netlist.getType(diodeElements[slot]);
            if (type != CircuitTypCore.LK_D && !isOn(slot) && !gateOn(slot)) continue;
            double last = committedMargin[slot];
            double slope = (last - previousMargin[slot]) / span;
            if (!(last * slope < 0)) continue;
            double predicted = committedMarginTime - last / slope - startTime;
            if (predicted > stepMin && predicted < target) target = predicted;
        }
        return target;
    }

    /** Copies the committed histories into the companion solver. They are scratch there. */
    private void copyHistories(MatrixSolver target) {
        System.arraycopy(matrixSolver.getPALT(), 0, target.getPALT(), 0, matrixSolver.getMatrixSize());
        System.arraycopy(matrixSolver.getPALTALT(), 0, target.getPALTALT(), 0, matrixSolver.getMatrixSize());
        System.arraycopy(matrixSolver.getIALT(), 0, target.getIALT(), 0, computedCurrents.length);
        System.arraycopy(matrixSolver.getIALTALT(), 0, target.getIALTALT(), 0, computedCurrents.length);
    }

    /** Builds and solves unconditionally, so a rejected trial leaves no cached matrix behind. */
    private void solveFresh(MatrixSolver solver, double width, double time) {
        if (nonlinearDevices != null) {
            nonlinearDevices.solve(solver, numericalNetlist, width, time);
            return;
        }
        solver.buildMatrixA(numericalNetlist, width, time, false);
        solver.buildVectorB(numericalNetlist, width, time, false);
        solver.solve();
    }

    /** Commits the used trial. Its type decides the current formulas, so a restart commits Backward Euler. */
    private void commitUsed(MatrixSolver used, double width, double time) {
        componentCurrentCalculator.calculateComponentCurrents(used, numericalNetlist, computedCurrents, 0.0, width, time, true);
        commitNonlinearDevices(used, width);
        if (used != matrixSolver) System.arraycopy(used.getP(), 0, matrixSolver.getP(), 0, matrixSolver.getMatrixSize());
        matrixSolver.shiftHistory();
        System.arraycopy(computedCurrents, 0, matrixSolver.getIALT(), 0, computedCurrents.length);
        rememberMatrixInputs(width);
        committedOnce = true;
    }

    private void commitNonlinearDevices(MatrixSolver solver, double width) {
        if (nonlinearDevices != null) nonlinearDevices.commit(solver, numericalNetlist, width, computedCurrents);
    }

    private boolean isNonlinearDevice(int element) {
        return nonlinearDevices != null && nonlinearDevices.includes(element);
    }

    private boolean changedInTrial() {
        for (int slot = 0; slot < diodeElements.length; slot++) if (toggled(slot)) return true;
        for (int slot = 0; slot < sourceElements.length; slot++) {
            if (netlist.getParameter(sourceElements[slot])[14] != savedSourceStates[slot]) return true;
        }
        return false;
    }

    private void recordSubStep(double startTime, double width, double error, boolean forced, boolean backwardEuler) {
        acceptedSubSteps++;
        if (forced) forcedSubSteps++;
        shortestSubStep = Double.isNaN(shortestSubStep) ? width : Math.min(shortestSubStep, width);
        longestSubStep = Double.isNaN(longestSubStep) ? width : Math.max(longestSubStep, width);
        if (subStepListener != null) subStepListener.accepted(startTime, width, error, forced, backwardEuler);
    }

    private void updateMarginHistory(double end) {
        System.arraycopy(committedMargin, 0, previousMargin, 0, committedMargin.length);
        previousMarginTime = committedMarginTime;
        computeThresholdMargin(matrixSolver.getPALT(), committedMargin);
        committedMarginTime = end;
    }

    /**
     * Largest normalized difference between the Trapezoidal and Backward Euler solutions of one sub-step.
     * Both start from the same committed histories and the same converged topology. The difference estimates
     * the Backward Euler local error, so it is not divided by 3. Only reads parameters.
     */
    private double estimateError(MatrixSolver used, MatrixSolver other, double width) {
        double worst = nonlinearDevices == null ? 0
                : nonlinearDevices.adaptiveError(used, other, numericalNetlist, width, adaptive.tolerance());
        double[] oldPotentials = matrixSolver.getPALT();
        for (int element = 0; element < netlist.getElementCount(); element++) {
            CircuitTypCore type = netlist.getType(element);
            int x = numericalNetlist.getNodeX(element);
            int y = numericalNetlist.getNodeY(element);
            double value;
            double alternative;
            double previous;
            double floor;
            if (type == CircuitTypCore.LK_C) {
                value = used.getP()[x] - used.getP()[y];
                alternative = other.getP()[x] - other.getP()[y];
                previous = oldPotentials[x] - oldPotentials[y];
                floor = VOLT_FLOOR;
            } else if (type == CircuitTypCore.LK_L) {
                value = inductorCurrent(used, element, x, y, width);
                alternative = inductorCurrent(other, element, x, y, width);
                previous = matrixSolver.getIALT()[element];
                floor = AMPERE_FLOOR;
            } else if (type == CircuitTypCore.LK_LKOP2) {
                int branch = netlist.getNodeMax() + netlist.getVoltageSourceNumber(element);
                value = used.getP()[branch];
                alternative = other.getP()[branch];
                previous = matrixSolver.getIALT()[element];
                floor = AMPERE_FLOOR;
            } else {
                continue;
            }
            double scale = adaptive.tolerance() * Math.max(Math.abs(value), Math.abs(previous)) + floor;
            worst = Math.max(worst, Math.abs(alternative - value) / scale);
        }
        return worst;
    }

    /** The inductor current of one trial, with the formulas of the committed current calculation. */
    private double inductorCurrent(MatrixSolver solver, int element, int x, int y, double width) {
        double inductance = Math.max(netlist.getParameter(element)[0], 1e-12);
        double voltage = solver.getP()[x] - solver.getP()[y];
        double previous = matrixSolver.getIALT()[element];
        if (solver.getSolverType() == SolverType.SOLVER_BE) return previous + width / inductance * voltage;
        double oldVoltage = matrixSolver.getPALT()[x] - matrixSolver.getPALT()[y];
        return previous + width / (2 * inductance) * (voltage + oldVoltage);
    }

    /** @return the value to compare at a grid start, or NaN for an element that cannot jump there */
    private double discontinuityValue(int element) {
        CircuitTypCore type = netlist.getType(element);
        double[] parameters = netlist.getParameter(element);
        if (isDiodeFamily(type) && !isNonlinearDevice(element)) {
            return parameters.length > 8 ? parameters[8] : Double.NaN;
        }
        if (type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_MOSFET && !isNonlinearDevice(element)) return parameters[0];
        if ((type == CircuitTypCore.LK_U || type == CircuitTypCore.LK_I) && parameters.length > 1) {
            int source = (int) parameters[0];
            boolean held = source == SourceType.QUELLE_SIGNALGESTEUERT || source == SourceType.QUELLE_SIGNALGESTEUERT_NEW;
            return held ? parameters[1] : Double.NaN;
        }
        return Double.NaN;
    }

    private void rememberDiscontinuityInputs() {
        for (int index = 0; index < discontinuityElements.length; index++) {
            discontinuityValues[index] = discontinuityValue(discontinuityElements[index]);
        }
    }

    private boolean discontinuityChanged() {
        for (int index = 0; index < discontinuityElements.length; index++) {
            if (discontinuityValues[index] != discontinuityValue(discontinuityElements[index])) return true;
        }
        return false;
    }

    /** @return the solvers this session actually runs on */
    LinearSolverReport linearSolverReport() {
        return new LinearSolverReport(matrixSolver.getBackend(), matrixSolver.getMatrixSize(),
                matrixSolver.getStoredCoefficientCount(), matrixSolver.getCoefficientStorageBytes(),
                matrixSolver.getFactorizationCount(),
                companionSolver == null ? null : companionSolver.getBackend(),
                companionSolver == null ? 0 : companionSolver.getStoredCoefficientCount(),
                initializationBackend, initializationStoredCoefficients, matrixSolver.getSparseFactorization(),
                companionSolver == null ? null : companionSolver.getSparseFactorization(), initializationFactorization);
    }

    /** @return the constant-memory statistics, with the event counts of the location feature */
    AdaptiveStepReport adaptiveReport() {
        if (adaptive == null) return AdaptiveStepReport.empty();
        return new AdaptiveStepReport(acceptedSubSteps, rejectedSubSteps, forcedSubSteps, shortestSubStep, longestSubStep,
                locatedCount, unlocatedCount);
    }

    /** Streams each accepted sub-step to a test. Null removes the listener. */
    void setSubStepListenerForTest(SubStepListener listener) { subStepListener = listener; }

    /**
     * Commits one sub-step directly with the given method and no estimate, as the adaptive loop does after an accepted trial.
     * It restores nothing, so a twin can replay the widths of an adaptive run.
     */
    void directSubStepForTest(double width, double time, boolean backwardEuler) {
        MatrixSolver used = backwardEuler && matrixSolver.getSolverType() != SolverType.SOLVER_BE ? companionSolver : matrixSolver;
        saveTrialState();
        copyHistories(companionSolver);
        solveFresh(used, width, time);
        doDiodeIteration(used, numericalNetlist, width, time, null);
        commitUsed(used, width, time);
    }

    /** Runs one adaptive grid step as the session does. */
    void advanceAdaptiveForTest(double dt, double time) { advanceAdaptive(dt, time, null); }

    // Package-private seams for the rollback and sub-step contract tests.

    /** @return the solver whose histories the contract tests compare */
    MatrixSolver solverForTest() { return matrixSolver; }

    /** Runs one free trial and restores every parameter it may change, without committing. */
    void trialAndRestoreForTest(double width, double time) {
        saveTrialState();
        solveFree(width, time);
        restoreTrialState();
    }

    /** Forces the next solve to rebuild the matrix, as a rollback does. */
    void invalidateMatrixForTest() { matrixDt = Double.NaN; }

    /** Commits one sub-step directly, with {@code pinnedElement} held or -1 for none. */
    void subStepForTest(double width, double time, int pinnedElement) {
        boolean[] pinned = null;
        if (pinnedElement >= 0) {
            pinned = new boolean[netlist.getElementCount()];
            pinned[pinnedElement] = true;
        }
        subStep(width, time, pinned);
    }

    /** Flips one diode-family element and forces a matrix rebuild, as a located event does. */
    void toggleForTest(int element) {
        saveTrialState();
        for (int slot = 0; slot < diodeElements.length; slot++) if (diodeElements[slot] == element) toggleDiode(slot);
    }

    /** Publishes results and the trace once, as the grid commit does. */
    void publishForTest(double time, HeadlessSimulationEngine.StepTraceListener listener) { publishGrid(time, listener); }

    /** Limits the retained log for tests. Retained events are never overwritten. */
    void setEventRetention(int limit) { eventRetention = limit; }

    /** @return a detached window of the located events, at most {@code count} entries from {@code firstIndex} */
    List<ZeroCrossingEvent> events(int firstIndex, int count) {
        int from = Math.min(firstIndex, events.size());
        return List.copyOf(events.subList(from, Math.min(events.size(), (int) Math.min((long) from + count, Integer.MAX_VALUE))));
    }

    EventLocationReport eventReport() {
        return new EventLocationReport(locatedCount, unlocatedCount, firstUnlocatedTime, events.size(), droppedCount);
    }

    private boolean matrixInputsChanged(double dt) {
        if (dt != matrixDt) return true;
        for (int element = 0; element < matrixInputs.length; element++) {
            double[] parameters = netlist.getParameter(element);
            for (int slot = 0; slot < 4; slot++) {
                if (matrixInputs[element][slot] != matrixInput(parameters, slot)) return true;
            }
        }
        return false;
    }

    private void rememberMatrixInputs(double dt) {
        matrixDt = dt;
        for (int element = 0; element < matrixInputs.length; element++) {
            double[] parameters = netlist.getParameter(element);
            for (int slot = 0; slot < 4; slot++) matrixInputs[element][slot] = matrixInput(parameters, slot);
        }
    }

    private static double matrixInput(double[] parameters, int slot) {
        // Matrix stamps use resistance/inductance/mode, capacitance, source gain, and source limit state.
        int index = switch (slot) { case 0 -> 0; case 1 -> 6; case 2 -> 11; default -> 14; };
        return index < parameters.length ? parameters[index] : 0;
    }

    void copyNodeVoltagesTo(float[] values) {
        double[] nodeVoltages = netlist.getLastNodeVoltagesRef();
        for (int index = 0; index < values.length && index < nodeVoltages.length; index++) {
            values[index] = (float) nodeVoltages[index];
        }
    }

    /**
     * Resolves diode states and controlled source voltage limits before one history commit.
     *
     * <p>After the initial solve, checks each diode/thyristor/IGBT for voltage-based
     * state transitions. If any diode toggles, incrementally patches the A and b
     * matrices and re-solves until convergence (or max 10000 iterations).
     * Controlled voltage sources update their limits in this same retry loop.
     *
     * <p>Voltage-based criteria (matching proven code):
     * <ul>
     *   <li>ON→OFF: voltage below threshold or IGBT gate OFF, while currently ON (rD &lt; 10000)</li>
     *   <li>OFF→ON: {@code V_diode > stoergroesse * Uf} AND currently OFF (rD &gt; 10000)</li>
     * </ul>
     *
     * <p>For thyristors/IGBTs, OFF→ON also requires {@code params[8] > 0.5} (gate signal).
     *
     * @param netlist the circuit netlist
     * @param dt time step
     * @param time current simulation time
     * @return true if a diode or source limit changes during this step
     */
    private boolean doDiodeIteration(INetList netlist, double dt, double time) {
        return doDiodeIteration(netlist, dt, time, null);
    }

    /** @param pinned by element index, elements whose state this iteration must not change, or null to let all elements switch */
    private boolean doDiodeIteration(INetList netlist, double dt, double time, boolean[] pinned) {
        return doDiodeIteration(matrixSolver, netlist, dt, time, pinned);
    }

    private boolean doDiodeIteration(MatrixSolver matrixSolver, INetList netlist, double dt, double time, boolean[] pinned) {
        double stoergroesse = 1.0;
        int iterCount = 0;
        boolean anyChangedOverall = false;
        boolean changed = true;

        double[] p = matrixSolver.getP();
        double[] bVector = matrixSolver.getb();

        while (changed) {
            changed = false;

            for (int i = 0; i < netlist.getElementCount(); i++) {
                CircuitTypCore type = netlist.getType(i);
                if (type != CircuitTypCore.LK_D && type != CircuitTypCore.LK_THYR
                        && type != CircuitTypCore.LK_IGBT) {
                    continue;
                }
                if (isNonlinearDevice(i) || pinned != null && pinned[i]) {
                    continue;
                }

                double[] params = netlist.getParameter(i);
                int x = netlist.getNodeX(i);
                int y = netlist.getNodeY(i);
                double diodeVoltage = p[x] - p[y];
                double rD = params[0];
                double uf = params[1]; // Forward voltage — never overwritten
                double rOn = params[2];
                double rOff = params[3];

                // An IGBT also turns off when its gate is removed. A thyristor remains latched.
                boolean gateRemoved = type == CircuitTypCore.LK_IGBT && params[8] < 0.5;
                if ((diodeVoltage < stoergroesse * uf || gateRemoved) && (rD < 10000)) {
                    double aOLD = 1.0 / rD;
                    double bOLD = uf / rD;
                    params[0] = rOff; // Switch to OFF
                    double aNEW = 1.0 / params[0];
                    // Retain diode trace compatibility; controlled devices block without a source.
                    double bNEW = type == CircuitTypCore.LK_D ? uf / params[0] : 0;

                    // Incremental patch A matrix
                    matrixSolver.addCoefficient(x, x, (-aOLD + aNEW));
                    matrixSolver.addCoefficient(y, y, (-aOLD + aNEW));
                    matrixSolver.addCoefficient(x, y, (aOLD - aNEW));
                    matrixSolver.addCoefficient(y, x, (aOLD - aNEW));
                    // Incremental patch b vector
                    bVector[x] += (-bOLD + bNEW);
                    bVector[y] += (bOLD - bNEW);

                    changed = true;
                }
                // OFF → ON: diode voltage above threshold while currently OFF
                else if ((diodeVoltage > stoergroesse * uf) && (rD > 10000)) {
                    // For thyristors/IGBTs, also require gate signal
                    if ((type == CircuitTypCore.LK_THYR || type == CircuitTypCore.LK_IGBT)
                            && params[8] < 0.5) {
                        continue; // Gate not active — cannot turn ON
                    }

                    double aOLD = 1.0 / rD;
                    double bOLD = type == CircuitTypCore.LK_D ? uf / rD : 0;
                    params[0] = rOn; // Switch to ON
                    double aNEW = 1.0 / params[0];
                    double bNEW = uf / params[0];

                    // Incremental patch A matrix
                    matrixSolver.addCoefficient(x, x, (-aOLD + aNEW));
                    matrixSolver.addCoefficient(y, y, (-aOLD + aNEW));
                    matrixSolver.addCoefficient(x, y, (aOLD - aNEW));
                    matrixSolver.addCoefficient(y, x, (aOLD - aNEW));
                    // Incremental patch b vector
                    bVector[x] += (-bOLD + bNEW);
                    bVector[y] += (bOLD - bNEW);

                    changed = true;
                }
            }

            changed |= matrixSolver.updateControlledSourceLimits(netlist, time);
            if (changed) {
                anyChangedOverall = true;
                if (nonlinearDevices == null) {
                    matrixSolver.setMatrixChanged();
                    matrixSolver.solve();
                } else {
                    nonlinearDevices.solve(matrixSolver, numericalNetlist, dt, time);
                }
                iterCount++;

                if (iterCount > 2) {
                    stoergroesse *= 0.99; // Perturbation damping
                }
                if (iterCount > 10000) {
                    throw new RuntimeException(
                            "Diode iteration did not converge after 10000 iterations at t=" + time);
                }
            }
        }

        return anyChangedOverall;
    }

    private record NumericalNetlist(CircuitNetlist source, int[] order, TransientSemiconductors nonlinearDevices)
            implements INetList {
        @Override public boolean usesWireReference() { return order.length != 0; }
        @Override public int getNodeMax() { return source.getNodeMax(); }
        @Override public int getVoltageSourceMax() { return source.getVoltageSourceMax(); }
        @Override public int getElementCount() { return source.getElementCount(); }
        @Override public CircuitTypCore getType(int index) { return source.getType(index); }
        @Override public int getNodeX(int index) { return ordered(source.getNodeX(index)); }
        @Override public int getNodeY(int index) { return ordered(source.getNodeY(index)); }
        private int ordered(int index) { return order.length == 0 ? index : order[index]; }
        @Override public double[] getParameter(int index) {
            double[] placeholder = nonlinearDevices == null ? null : nonlinearDevices.placeholder(index);
            return placeholder != null ? placeholder : source.getParameter(index);
        }
        @Override public int getVoltageSourceNumber(int index) { return source.getVoltageSourceNumber(index); }
        @Override public double getSimulationTime() { return source.getSimulationTime(); }
        @Override public int getControlElementIndex(int index) { return source.getControlElementIndex(index); }
        @Override public gecko.core.circuit.waveform.PiecewiseLinearWaveform getPwlWaveform(int index) {
            return source.getPwlWaveform(index);
        }
        @Override public java.util.List<gecko.core.circuit.netlist.MutualCouplingRegistry.Coupling> getAllCouplings() {
            return source.getAllCouplings();
        }
    }

}

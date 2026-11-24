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

import gecko.core.allg.SolverType;
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

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType) {
        this(netlist, solverType, false);
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType, boolean zeroCrossing) {
        this(netlist, solverType, new SolverFeatures(0, zeroCrossing));
    }

    ElectricalTimeStep(CircuitNetlist netlist, SolverType solverType, SolverFeatures features) {
        this.zeroCrossing = features.zeroCrossing();
        this.netlist = netlist;
        nodeOrder = netlist.getNumericalNodeOrder();
        numericalNetlist = nodeOrder.length == 0 ? netlist : new NumericalNetlist(netlist, nodeOrder);
        matrixSolver = new MatrixSolver(solverType);
        matrixSolver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());
        computedCurrents = new double[netlist.getElementCount()];
        matrixInputs = new double[netlist.getElementCount()][4];
        publicPotentials = new double[matrixSolver.getMatrixSize()];
        InitialConditionSolver initialConditions = new InitialConditionSolver(solverType);
        if (features.dcOperatingPoint()) initialConditions.setDcOperatingPoint(matrixSolver, numericalNetlist);
        else initialConditions.setDialogInitialConditions(matrixSolver, numericalNetlist);
        int[] diodes = new int[netlist.getElementCount()];
        int[] sources = new int[netlist.getElementCount()];
        int diodeCount = 0;
        int sourceCount = 0;
        for (int element = 0; element < diodes.length; element++) {
            CircuitTypCore type = netlist.getType(element);
            if (isDiodeFamily(type)) diodes[diodeCount++] = element;
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
        if (!numericalNetlist.usesWireReference() || matrixInputsChanged(dt)) {
            matrixSolver.buildMatrixA(numericalNetlist, dt, time, false);
        }
        matrixSolver.buildVectorB(numericalNetlist, dt, time, false);
        matrixSolver.solve();
        doDiodeIteration(numericalNetlist, dt, time);

        // Compute against previous-step histories before committing the new state.
        componentCurrentCalculator.calculateComponentCurrents(
                matrixSolver, numericalNetlist, computedCurrents, 0.0, dt, time, true);
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
                crossing[slot] = refine(slot, startMargin[slot], trialMargin[slot], current, time, epsilon);
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

    private void solveStartTopology(double width, double time) {
        if (!numericalNetlist.usesWireReference() || matrixInputsChanged(width)) {
            matrixSolver.buildMatrixA(numericalNetlist, width, time, false);
        }
        matrixSolver.buildVectorB(numericalNetlist, width, time, false);
        matrixSolver.solve();
    }

    /** Commits histories for one sub-step. Public values stay unchanged until {@link #publishGrid}. */
    private void commitSubStep(double width, double time) {
        componentCurrentCalculator.calculateComponentCurrents(
                matrixSolver, numericalNetlist, computedCurrents, 0.0, width, time, true);
        matrixSolver.shiftHistory();
        System.arraycopy(computedCurrents, 0, matrixSolver.getIALT(), 0, computedCurrents.length);
        rememberMatrixInputs(width);
        committedOnce = true;
    }

    private void publishGrid(double time, HeadlessSimulationEngine.StepTraceListener traceListener) {
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
    private double refine(int slot, double gLow, double gHigh, double start, double end, double epsilon) {
        double low = start;
        double high = end;
        double crossing = detector.detectCrossings(new double[]{gLow}, new double[]{gHigh}, 0, start, end)
                .getFirst().getCrossingTime();
        double[] probe = new double[diodeElements.length];
        for (int round = 0; round < REFINEMENTS; round++) {
            if (!(crossing > start + epsilon && crossing < end - epsilon)) break;
            restoreTrialState();
            solveStartTopology(crossing - start, crossing);
            computeThresholdMargin(matrixSolver.getP(), probe);
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
     * Exports every electrical value that the next step reads, after a committed grid step. It only reads.
     * Solver histories come first, then capacitor currents, then the discrete states, which compare exactly.
     * The derived matrix cache, the clock and the event counters are not state and are not exported.
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
        double stoergroesse = 1.0;
        int iterCount = 0;
        boolean anyChangedOverall = false;
        boolean changed = true;

        double[] p = matrixSolver.getP();
        double[][] a = matrixSolver.getSystemMatrix();
        double[] bVector = matrixSolver.getb();

        while (changed) {
            changed = false;

            for (int i = 0; i < netlist.getElementCount(); i++) {
                CircuitTypCore type = netlist.getType(i);
                if (type != CircuitTypCore.LK_D && type != CircuitTypCore.LK_THYR
                        && type != CircuitTypCore.LK_IGBT) {
                    continue;
                }
                if (pinned != null && pinned[i]) {
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
                    a[x][x] += (-aOLD + aNEW);
                    a[y][y] += (-aOLD + aNEW);
                    a[x][y] += (aOLD - aNEW);
                    a[y][x] += (aOLD - aNEW);
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
                    a[x][x] += (-aOLD + aNEW);
                    a[y][y] += (-aOLD + aNEW);
                    a[x][y] += (aOLD - aNEW);
                    a[y][x] += (aOLD - aNEW);
                    // Incremental patch b vector
                    bVector[x] += (-bOLD + bNEW);
                    bVector[y] += (bOLD - bNEW);

                    changed = true;
                }
            }

            changed |= matrixSolver.updateControlledSourceLimits(netlist, time);
            if (changed) {
                anyChangedOverall = true;
                matrixSolver.setMatrixChanged();
                matrixSolver.solve();
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

    private record NumericalNetlist(CircuitNetlist source, int[] order) implements INetList {
        @Override public boolean usesWireReference() { return true; }
        @Override public int getNodeMax() { return source.getNodeMax(); }
        @Override public int getVoltageSourceMax() { return source.getVoltageSourceMax(); }
        @Override public int getElementCount() { return source.getElementCount(); }
        @Override public CircuitTypCore getType(int index) { return source.getType(index); }
        @Override public int getNodeX(int index) { return order[source.getNodeX(index)]; }
        @Override public int getNodeY(int index) { return order[source.getNodeY(index)]; }
        @Override public double[] getParameter(int index) { return source.getParameter(index); }
        @Override public int getVoltageSourceNumber(int index) { return source.getVoltageSourceNumber(index); }
        @Override public double getSimulationTime() { return source.getSimulationTime(); }
        @Override public int getControlElementIndex(int index) { return source.getControlElementIndex(index); }
        @Override public java.util.List<gecko.core.circuit.netlist.MutualCouplingRegistry.Coupling> getAllCouplings() {
            return source.getAllCouplings();
        }
    }

}

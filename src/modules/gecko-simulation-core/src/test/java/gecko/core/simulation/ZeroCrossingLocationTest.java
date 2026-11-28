package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.MatrixSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real solver steps against closed-form switching times. */
class ZeroCrossingLocationTest {
    static final double VPK = 10;
    static final double FREQ = 50;
    static final double PERIOD = 1 / FREQ;
    static final double OMEGA = 2 * Math.PI * FREQ;
    static final double UF = 0.7;
    static final double RON = 1e-3;
    static final double ROFF = 1e8;
    static final double LOAD = 10;

    static double[] sine(double phaseDegrees) {
        double[] source = new double[21];
        source[0] = SourceType.QUELLE_SIN_NEW;
        source[2] = FREQ;
        source[4] = phaseDegrees;
        source[20] = VPK;
        return source;
    }

    static double[] diode(double forwardVoltage) {
        double[] diode = new double[21];
        diode[0] = ROFF;
        diode[1] = forwardVoltage;
        diode[2] = RON;
        diode[3] = ROFF;
        return diode;
    }

    static double[] value(double first) {
        double[] parameters = new double[21];
        parameters[0] = first;
        return parameters;
    }

    /** Sine source, one diode, then a resistor, or an inductor and a resistor when inductance is positive. */
    static CircuitNetlist rectifier(double forwardVoltage, double inductance) {
        boolean inductive = inductance > 0;
        CircuitTypCore[] types = inductive
                ? new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_D, CircuitTypCore.LK_L, CircuitTypCore.LK_R}
                : new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_D, CircuitTypCore.LK_R};
        int[] nodeX = inductive ? new int[]{1, 1, 2, 3} : new int[]{1, 1, 2};
        int[] nodeY = inductive ? new int[]{0, 2, 3, 0} : new int[]{0, 2, 0};
        int[] sources = inductive ? new int[]{1, -1, -1, -1} : new int[]{1, -1, -1};
        double[][] parameters = inductive
                ? new double[][]{sine(0), diode(forwardVoltage), value(inductance), value(LOAD)}
                : new double[][]{sine(0), diode(forwardVoltage), value(LOAD)};
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, sources, parameters, inductive ? 3 : 2, 1, types.length);
        return netlist;
    }

    /** Two sources a quarter period apart feed one load through two diodes. */
    static CircuitNetlist commutation() {
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_U, CircuitTypCore.LK_D,
                CircuitTypCore.LK_D, CircuitTypCore.LK_R},
                new int[]{1, 2, 1, 2, 3}, new int[]{0, 0, 3, 3, 0}, new int[]{1, 2, -1, -1, -1},
                new double[][]{sine(0), sine(90), diode(UF), diode(UF), value(LOAD)}, 3, 2, 5);
        return netlist;
    }

    /** One source feeds many diode branches whose thresholds all fall inside one large step. */
    static CircuitNetlist ladder(int branches) {
        CircuitTypCore[] types = new CircuitTypCore[1 + 2 * branches];
        int[] nodeX = new int[types.length];
        int[] nodeY = new int[types.length];
        int[] sources = new int[types.length];
        double[][] parameters = new double[types.length][];
        Arrays.fill(sources, -1);
        types[0] = CircuitTypCore.LK_U;
        nodeX[0] = 1;
        sources[0] = 1;
        parameters[0] = sine(0);
        for (int branch = 0; branch < branches; branch++) {
            int diode = 1 + 2 * branch;
            types[diode] = CircuitTypCore.LK_D;
            nodeX[diode] = 1;
            nodeY[diode] = 2 + branch;
            parameters[diode] = diode(0.5 + 0.5 * branch);
            types[diode + 1] = CircuitTypCore.LK_R;
            nodeX[diode + 1] = 2 + branch;
            nodeY[diode + 1] = 0;
            parameters[diode + 1] = value(LOAD);
        }
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, sources, parameters, 1 + branches, 1, types.length);
        return netlist;
    }

    static void run(ElectricalTimeStep step, int steps, double dt) {
        for (int index = 0; index < steps; index++) step.advanceInterval(dt, index * dt, null);
    }

    static double analyticTurnOn(int period, double forwardVoltage) {
        return period * PERIOD + Math.asin(forwardVoltage / VPK) / OMEGA;
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void halfWaveRectifierLocatesTurnOnAndTurnOffWithinATenthOfTheStep(SolverType method) {
        double dt = PERIOD / 50;
        CircuitNetlist netlist = rectifier(UF, 0);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method, true);
        double lowest = 0;
        for (int index = 0; index < 150; index++) {
            double time = index * dt;
            step.advanceInterval(dt, time, null);
            double current = netlist.getLastComponentCurrents()[1];
            lowest = Math.min(lowest, current);
            // The published row is the final sub-step at the grid time, so it matches the algebraic solution.
            double source = VPK * Math.sin(OMEGA * time);
            if (netlist.getParameter(1)[0] < 10000) {
                assertEquals((source - UF) / (LOAD + RON), current, 1e-7, "step " + index);
            }
        }
        assertTrue(lowest >= -1e-6, "reverse leakage is explicit, not a conduction tail: " + lowest);
        assertTrue(lowest >= -(VPK + UF) / ROFF * 1.01 - 1e-12);

        List<ZeroCrossingEvent> events = step.events(0, 100);
        assertEquals(6, events.size(), "two events per period over three periods");
        for (int period = 0; period < 3; period++) {
            ZeroCrossingEvent on = events.get(2 * period);
            ZeroCrossingEvent off = events.get(2 * period + 1);
            assertEquals(ZeroCrossingEvent.Direction.RISING, on.getDirection());
            assertEquals(ZeroCrossingEvent.Direction.FALLING, off.getDirection());
            assertEquals(1, on.getSignalIndex(), "events name the diode element");
            assertEquals(analyticTurnOn(period, UF), on.getCrossingTime(), dt / 10, "turn-on " + period);
            double analyticOff = period * PERIOD + (Math.PI - Math.asin(UF / VPK)) / OMEGA;
            assertEquals(analyticOff, off.getCrossingTime(), dt / 10, "turn-off " + period);
        }
        EventLocationReport report = step.eventReport();
        assertEquals(6, report.located());
        assertEquals(0, report.unlocated());
        assertTrue(Double.isNaN(report.firstUnlocatedTime()));
        assertEquals(6, report.retained());
        assertEquals(0, report.dropped());
    }

    /** First zero of the analytic RL current after conduction starts at the turn-on time. */
    static double analyticExtinction(double inductance) {
        double resistance = LOAD + RON;
        double impedance = Math.hypot(resistance, OMEGA * inductance);
        double phase = Math.atan2(OMEGA * inductance, resistance);
        double tau = inductance / resistance;
        double on = analyticTurnOn(0, UF);
        java.util.function.DoubleUnaryOperator particular = time -> VPK / impedance * Math.sin(OMEGA * time - phase) - UF / resistance;
        java.util.function.DoubleUnaryOperator current = time ->
                particular.applyAsDouble(time) - particular.applyAsDouble(on) * Math.exp(-(time - on) / tau);
        double low = PERIOD / 4;
        double high = PERIOD;
        assertTrue(current.applyAsDouble(low) > 0 && current.applyAsDouble(high) < 0);
        for (int iteration = 0; iteration < 200; iteration++) {
            double middle = (low + high) / 2;
            if (current.applyAsDouble(middle) > 0) low = middle; else high = middle;
        }
        return (low + high) / 2;
    }

    @Test
    void inductiveLoadExtinctionMatchesTheAnalyticTimeWithTrapezoidalIntegration() {
        double dt = PERIOD / 50;
        double inductance = 20e-3;
        CircuitNetlist netlist = rectifier(UF, inductance);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, true);
        run(step, 45, dt); // stops before the trapezoidal ringing of the open diode reaches the next turn-on
        List<ZeroCrossingEvent> events = step.events(0, 10);
        assertEquals(2, events.size());
        assertEquals(analyticTurnOn(0, UF), events.get(0).getCrossingTime(), dt / 10);
        assertEquals(ZeroCrossingEvent.Direction.FALLING, events.get(1).getDirection());
        assertEquals(analyticExtinction(inductance), events.get(1).getCrossingTime(), dt / 10);
        assertEquals(0, step.eventReport().unlocated());
    }

    static void assertSameState(ElectricalTimeStep expected, CircuitNetlist expectedNetlist,
            ElectricalTimeStep actual, CircuitNetlist actualNetlist, String message) {
        MatrixSolver left = expected.solverForTest();
        MatrixSolver right = actual.solverForTest();
        assertArrayEquals(left.getPALT(), right.getPALT(), message + " pALT");
        assertArrayEquals(left.getPALTALT(), right.getPALTALT(), message + " pALTALT");
        assertArrayEquals(left.getPALTALTALT(), right.getPALTALTALT(), message + " pALTALTALT");
        assertArrayEquals(left.getIALT(), right.getIALT(), message + " iALT");
        assertArrayEquals(left.getIALTALT(), right.getIALTALT(), message + " iALTALT");
        assertArrayEquals(left.getIALTALTALT(), right.getIALTALTALT(), message + " iALTALTALT");
        for (int element = 0; element < expectedNetlist.getElementCount(); element++) {
            assertArrayEquals(expectedNetlist.getParameter(element), actualNetlist.getParameter(element),
                    message + " parameters of element " + element);
        }
        assertArrayEquals(expectedNetlist.getLastNodeVoltages(), actualNetlist.getLastNodeVoltages(), message + " published potentials");
        assertArrayEquals(expectedNetlist.getLastComponentCurrents(), actualNetlist.getLastComponentCurrents(), message + " published currents");
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void aTrialLeavesNoStateAndTheSplitStepEqualsDirectSubSteps(SolverType method) {
        double dt = PERIOD / 50;
        double inductance = 20e-3;
        double extinction = analyticExtinction(inductance);
        int splitStep = (int) Math.floor(extinction / dt) + 1;
        double gridTime = splitStep * dt;

        CircuitNetlist withTrialNetlist = rectifier(UF, inductance);
        CircuitNetlist withoutTrialNetlist = rectifier(UF, inductance);
        ElectricalTimeStep withTrial = new ElectricalTimeStep(withTrialNetlist, method, true);
        ElectricalTimeStep withoutTrial = new ElectricalTimeStep(withoutTrialNetlist, method, true);
        run(withTrial, splitStep, dt);
        run(withoutTrial, splitStep, dt);
        assertSameState(withoutTrial, withoutTrialNetlist, withTrial, withTrialNetlist, "same prefix");
        double[][] before = {withTrial.solverForTest().getPALT().clone(), withTrial.solverForTest().getIALT().clone()};
        double[] parameterBefore = withTrialNetlist.getParameter(1).clone();
        assertTrue(parameterBefore[0] < 10000, "the diode conducts, so the trial really switches it off");

        // A trial that really switches the diode must restore parameter 0 and every history.
        withTrial.trialAndRestoreForTest(dt, gridTime);
        assertArrayEquals(before[0], withTrial.solverForTest().getPALT());
        assertArrayEquals(before[1], withTrial.solverForTest().getIALT());
        assertArrayEquals(parameterBefore, withTrialNetlist.getParameter(1));
        assertSameState(withoutTrial, withoutTrialNetlist, withTrial, withTrialNetlist, "after rollback");

        // Identical sub-steps afterwards, one side with the trial and one without, must agree to the bit.
        withoutTrial.invalidateMatrixForTest();
        double eventTime = (splitStep - 1) * dt + 0.37 * dt;
        for (var pair : List.of(new Object[]{withTrial, withTrialNetlist}, new Object[]{withoutTrial, withoutTrialNetlist})) {
            ElectricalTimeStep side = (ElectricalTimeStep) pair[0];
            side.subStepForTest(eventTime - (splitStep - 1) * dt, eventTime, 1);
            side.toggleForTest(1);
            side.subStepForTest(gridTime - eventTime, gridTime, -1);
            side.publishForTest(gridTime, null);
        }
        assertSameState(withoutTrial, withoutTrialNetlist, withTrial, withTrialNetlist, "after sub-steps");
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void theProductionSplitEqualsTheSameSubStepsRunDirectlyAndPublishesOnce(SolverType method) {
        double dt = PERIOD / 50;
        double inductance = 20e-3;
        int splitStep = (int) Math.floor(analyticExtinction(inductance) / dt) + 1;
        double gridTime = splitStep * dt;
        CircuitNetlist productionNetlist = rectifier(UF, inductance);
        CircuitNetlist directNetlist = rectifier(UF, inductance);
        ElectricalTimeStep production = new ElectricalTimeStep(productionNetlist, method, true);
        ElectricalTimeStep direct = new ElectricalTimeStep(directNetlist, method, true);
        run(production, splitStep, dt);
        run(direct, splitStep, dt);
        List<double[]> traces = new ArrayList<>();
        production.advanceInterval(dt, gridTime, (time, volts, currents, circuit) -> {
            traces.add(new double[]{time, volts[1], currents[1]});
            assertArrayEquals(circuit.getLastNodeVoltages(), volts, "the trace sees the published row");
        });
        assertEquals(1, traces.size(), "one trace call for the split step");
        assertEquals(gridTime, traces.get(0)[0]);
        assertEquals(2, production.eventReport().located());
        double eventTime = production.events(1, 1).get(0).getCrossingTime();
        assertTrue(eventTime > gridTime - dt && eventTime < gridTime);

        double intervalStart = gridTime - dt; // the production split computes its start the same way
        direct.subStepForTest(eventTime - intervalStart, eventTime, 1);
        direct.toggleForTest(1);
        direct.subStepForTest(gridTime - eventTime, gridTime, -1);
        direct.publishForTest(gridTime, null);
        assertSameState(direct, directNetlist, production, productionNetlist, "direct comparison");
        assertEquals(directNetlist.getLastComponentCurrents()[1], traces.get(0)[2]);
    }

    @Test
    void aRolledBackControlledSourceLimitIsRestored() {
        double[][] parameters = new double[4][21];
        parameters[0][1] = 2;
        parameters[1][0] = 5;
        parameters[1][11] = 3;
        parameters[1][12] = -4;
        parameters[1][13] = 5;
        parameters[2][0] = 10;
        parameters[3][0] = 0.25;
        parameters[3][6] = 0.25;
        parameters[3][7] = 0.25;
        CircuitNetlist circuit = new CircuitNetlist();
        circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                CircuitTypCore.LK_C}, new int[]{1, 2, 2, 2}, new int[]{0, 0, 0, 0}, new int[]{1, 2, -1, -1},
                parameters, 2, 2, 4);
        circuit.setControlElementIndex(1, 0);
        ElectricalTimeStep step = new ElectricalTimeStep(circuit, SolverType.SOLVER_BE, true);
        step.advanceInterval(0.125, 0, null);
        circuit.getParameter(0)[1] = 2; // 3 * 2 V exceeds the 5 V limit, so a trial sets parameter 14 to 1.
        double before = circuit.getParameter(1)[14];
        step.trialAndRestoreForTest(0.125, 0.125);
        assertEquals(before, circuit.getParameter(1)[14]);
        step.advanceInterval(0.125, 0.125, null);
        assertEquals(1, circuit.getParameter(1)[14], "the committed step keeps the limit");
    }

    @Test
    void twoDiodesCommutatingInOneStepGiveSortedEventsWithAtMostOnePerDevice() {
        double dt = PERIOD / 20;
        CircuitNetlist netlist = commutation();
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, true);
        List<List<ZeroCrossingEvent>> perStep = new ArrayList<>();
        int seen = 0;
        for (int index = 0; index < 20; index++) {
            step.advanceInterval(dt, index * dt, null);
            var all = step.events(0, 1000);
            perStep.add(all.subList(seen, all.size()));
            seen = all.size();
        }
        double crossing = 3.0 / 8 * PERIOD;
        int commutationStep = (int) Math.floor(crossing / dt) + 1;
        List<ZeroCrossingEvent> atCommutation = perStep.get(commutationStep);
        assertEquals(2, atCommutation.size(), "turn-on of one diode and turn-off of the other: " + atCommutation);
        for (ZeroCrossingEvent event : atCommutation) assertEquals(crossing, event.getCrossingTime(), dt / 10);
        assertNotEquals(atCommutation.get(0).getSignalIndex(), atCommutation.get(1).getSignalIndex());
        for (var events : perStep) {
            for (int index = 1; index < events.size(); index++) {
                assertTrue(events.get(index - 1).getCrossingTime() <= events.get(index).getCrossingTime(), "sorted");
                assertNotEquals(events.get(index - 1).getSignalIndex(), events.get(index).getSignalIndex());
            }
        }
        assertEquals(0, step.eventReport().unlocated());
    }

    @Test
    void sixteenEventsAreLocatedAndTheRestOfAManyCrossingStepIsReportedUnlocated() {
        double dt = PERIOD / 4;
        CircuitNetlist netlist = ladder(17);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, true);
        run(step, 2, dt);
        EventLocationReport report = step.eventReport();
        assertEquals(ElectricalTimeStep.MAX_LOCATED_PER_STEP, report.located());
        assertEquals(1, report.unlocated());
        assertEquals(dt, report.firstUnlocatedTime(), 1e-15, "the unlocated transition commits at the grid time");
        List<ZeroCrossingEvent> events = step.events(0, 100);
        for (int branch = 0; branch < events.size(); branch++) {
            assertEquals(1 + 2 * branch, events.get(branch).getSignalIndex());
            double analytic = Math.asin((0.5 + 0.5 * branch) / VPK) / OMEGA;
            assertEquals(analytic, events.get(branch).getCrossingTime(), dt / 10);
        }
        for (int branch = 0; branch < 17; branch++) {
            assertEquals(RON, netlist.getParameter(1 + 2 * branch)[0], "every diode conducts after the step");
        }
    }

    @Test
    void theEventLogIsBoundedAndNeverOverwritesRetainedEvents() {
        double dt = PERIOD / 4;
        ElectricalTimeStep full = new ElectricalTimeStep(ladder(17), SolverType.SOLVER_BE, true);
        run(full, 2, dt);
        ElectricalTimeStep capped = new ElectricalTimeStep(ladder(17), SolverType.SOLVER_BE, true);
        capped.setEventRetention(3);
        run(capped, 2, dt);
        EventLocationReport report = capped.eventReport();
        assertEquals(16, report.located());
        assertEquals(3, report.retained());
        assertEquals(13, report.dropped());
        List<ZeroCrossingEvent> retained = capped.events(0, 100);
        for (int index = 0; index < 3; index++) {
            assertEquals(full.events(0, 100).get(index).getCrossingTime(), retained.get(index).getCrossingTime());
        }
        assertEquals(2, capped.events(1, 100).size());
        assertEquals(0, capped.events(10, 5).size());
        assertEquals(2, capped.events(1, 2).size());
    }

    @Test
    void noEventIsLocatedAtTheFirstTimestampOrWhenTheFeatureIsOff() {
        CircuitNetlist startsOn = rectifier(0, 0);
        ElectricalTimeStep step = new ElectricalTimeStep(startsOn, SolverType.SOLVER_BE, true);
        step.advanceInterval(PERIOD / 50, 0, null);
        assertEquals(0, step.eventReport().located());
        assertEquals(0, step.eventReport().unlocated());

        ElectricalTimeStep off = new ElectricalTimeStep(rectifier(UF, 0), SolverType.SOLVER_BE, false);
        run(off, 150, PERIOD / 50);
        assertEquals(EventLocationReport.empty(), off.eventReport());
        assertTrue(off.events(0, 10).isEmpty());
    }

    @Test
    void eventsOnTheGridPointNeverCreateZeroWidthSteps() {
        // The turn-on falls on the grid point t = dt, where the source equals the threshold.
        double dt = PERIOD / 50;
        CircuitNetlist netlist = rectifier(VPK * Math.sin(OMEGA * dt), 0);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, true);
        run(step, 6, dt);
        assertEquals(0, step.eventReport().unlocated());
        for (var event : step.events(0, 10)) {
            assertTrue(Double.isFinite(event.getCrossingTime()));
            assertTrue(event.getCrossingTime() >= 0);
        }
        for (double current : netlist.getLastComponentCurrents()) assertTrue(Double.isFinite(current));
    }

    /** Sine source, a gated device with its gate held on, then a resistor. */
    static CircuitNetlist gated(CircuitTypCore type) {
        double[] device = diode(UF);
        device[8] = 1;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, type, CircuitTypCore.LK_R},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1},
                new double[][]{sine(0), device, value(LOAD)}, 2, 1, 3);
        return netlist;
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_IGBT", "LK_THYR"})
    void gatedDevicesLocateLikeADiodeWhileTheGateIsHeldOn(CircuitTypCore type) {
        double dt = PERIOD / 50;
        ElectricalTimeStep step = new ElectricalTimeStep(gated(type), SolverType.SOLVER_BE, true);
        run(step, 40, dt);
        List<ZeroCrossingEvent> events = step.events(0, 10);
        assertEquals(2, events.size());
        assertEquals(analyticTurnOn(0, UF), events.get(0).getCrossingTime(), dt / 10);
        assertEquals((Math.PI - Math.asin(UF / VPK)) / OMEGA, events.get(1).getCrossingTime(), dt / 10);
        assertEquals(0, step.eventReport().unlocated());
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_IGBT", "LK_THYR"})
    void gateRemovalIsNotLocatedBecauseItOccursOnTheGridPoint(CircuitTypCore type) {
        double dt = PERIOD / 50;
        CircuitNetlist netlist = gated(type);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, true);
        run(step, 10, dt);
        assertEquals(RON, netlist.getParameter(1)[0], "conducting before the gate edge");
        netlist.getParameter(1)[8] = 0;
        step.advanceInterval(dt, 10 * dt, null);
        assertEquals(1, step.eventReport().located(), "only the natural turn-on is an event");
        assertEquals(0, step.eventReport().unlocated(), "the gate edge is a grid event, not a missed one");
        // The IGBT blocks at the gate edge. The thyristor stays latched until its current ends.
        assertEquals(type == CircuitTypCore.LK_IGBT ? ROFF : RON, netlist.getParameter(1)[0]);
    }

    @Test
    void anEventJustBeforeTheGridPointCommitsTheTrialWithoutAZeroWidthStep() {
        double dt = PERIOD / 50;
        double threshold = VPK * Math.sin(OMEGA * dt) * (1 - 1e-5);
        CircuitNetlist netlist = rectifier(threshold, 0);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, true);
        run(step, 2, dt);
        assertEquals(1, step.eventReport().located());
        assertEquals(dt, step.events(0, 1).get(0).getCrossingTime(), 1e-3 * dt);
        assertEquals(RON, netlist.getParameter(1)[0]);
        assertEquals(0, step.eventReport().unlocated());
        assertTrue(Double.isFinite(netlist.getLastComponentCurrents()[1]));
    }

    @Test
    void anInvalidIntervalIsRejectedBeforeAnyStateChanges() {
        ElectricalTimeStep step = new ElectricalTimeStep(rectifier(UF, 0), SolverType.SOLVER_BE, true);
        assertThrows(IllegalArgumentException.class, () -> step.advanceInterval(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> step.advanceInterval(Double.NaN, 0, null));
        assertThrows(IllegalArgumentException.class, () -> step.advanceInterval(1e-3, -1e-3, null));
        assertThrows(IllegalArgumentException.class, () -> step.advanceInterval(1e-3, Double.POSITIVE_INFINITY, null));
        assertEquals(EventLocationReport.empty(), step.eventReport());
    }

    /** Four diodes and a load across a sine source: D1 and D4 conduct together, then D2 and D3. */
    static CircuitNetlist bridge(double phaseDegrees) {
        double[][] parameters = {sine(phaseDegrees), diode(UF), diode(UF), diode(UF), diode(UF), value(LOAD)};
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_D, CircuitTypCore.LK_D,
                CircuitTypCore.LK_D, CircuitTypCore.LK_D, CircuitTypCore.LK_R},
                new int[]{1, 1, 0, 3, 3, 2}, new int[]{0, 2, 2, 1, 0, 3}, new int[]{1, -1, -1, -1, -1, -1},
                parameters, 3, 1, 6);
        return netlist;
    }

    /** Result of comparing every committed diode transition with the reported events. */
    record Accounting(int transitions, int explained, long located, long unlocated) { }

    /**
     * Steps a netlist and checks each grid step. A device whose committed state changed must have a located
     * event or a larger unlocated count. A device with an event must have changed state. Returns totals.
     */
    static Accounting audit(CircuitNetlist netlist, SolverType method, double dt, int steps) {
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method, true);
        int transitions = 0;
        int explained = 0;
        for (int index = 0; index < steps; index++) {
            boolean[] before = new boolean[netlist.getElementCount()];
            for (int element = 0; element < before.length; element++) before[element] = netlist.getParameter(element)[0] < 10000;
            EventLocationReport old = step.eventReport();
            step.advanceInterval(dt, index * dt, null);
            EventLocationReport now = step.eventReport();
            var fresh = step.events(old.retained(), now.retained() - old.retained());
            java.util.Set<Integer> logged = new java.util.HashSet<>();
            for (var event : fresh) assertTrue(logged.add(event.getSignalIndex()), "one event per device and step");
            if (index == 0) continue;
            long newUnlocated = now.unlocated() - old.unlocated();
            int changedUnexplained = 0;
            for (int element = 0; element < before.length; element++) {
                if (netlist.getType(element) != CircuitTypCore.LK_D) continue;
                boolean changed = before[element] != (netlist.getParameter(element)[0] < 10000);
                if (changed) transitions++;
                if (changed && logged.contains(element)) explained++;
                else if (changed) changedUnexplained++;
                if (!changed) assertFalse(logged.contains(element), "a logged event needs a committed change: step " + index + " device " + element);
            }
            assertTrue(changedUnexplained <= newUnlocated, "step " + index + ": " + changedUnexplained
                    + " silent transitions, unlocated rose by " + newUnlocated);
            explained += changedUnexplained;
        }
        return new Accounting(transitions, explained, step.eventReport().located(), step.eventReport().unlocated());
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void fullBridgePairsAreAllLocatedIncludingPairsNearTheGridPoint(SolverType method) {
        double dt = PERIOD / 50;
        // The second phase puts the paired turn-on of D1 and D4 just before a grid point.
        double nearGridPoint = Math.toDegrees(Math.asin(2 * UF / VPK) - OMEGA * (5 * dt - 1e-7));
        for (double phase : new double[]{0, nearGridPoint, -27.95035375268849, 10, 45, -60, 90, 133.3}) {
            Accounting accounting = audit(bridge(phase), method, dt, 150);
            assertEquals(24, accounting.transitions(), "phase " + phase);
            assertEquals(accounting.transitions(), accounting.located() + accounting.unlocated(), "phase " + phase);
            assertEquals(0, accounting.unlocated(), "every crossing of this bridge has a known time: phase " + phase);
        }
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void ringingThatSwitchesADeviceBackAndForthNeverLeavesAFalseEventOrASilentChange(SolverType method) {
        // The trapezoidal inductor rings after the diode opens and switches it on and off again. Each audit
        // step rejects a logged event without a committed change, and a committed change without an event
        // or an unlocated count.
        double dt = PERIOD / 50;
        for (double forward : new double[]{0, UF}) {
            Accounting accounting = audit(rectifier(forward, 20e-3), method, dt, 150);
            assertTrue(accounting.located() >= 5, "the regular turn-on and turn-off events stay located: " + accounting);
            if (method == SolverType.SOLVER_TRZ) {
                assertTrue(accounting.unlocated() > 0, "ringing transitions are reported, not hidden: " + accounting);
            }
        }
    }

    @Test
    void devicesThatChangeDuringAPinnedSubStepAreCountedNotDropped() {
        // Both diodes of the ladder cross inside one step. The first one pins, the second changes in the same sub-step
        // when the two thresholds differ by less than the boundary tolerance, so it is located together or counted.
        double dt = PERIOD / 4;
        CircuitNetlist netlist = ladder(3);
        netlist.getParameter(3)[1] = netlist.getParameter(1)[1] * (1 + 1e-9);
        Accounting accounting = audit(netlist, SolverType.SOLVER_BE, dt, 2);
        assertEquals(3, accounting.transitions());
        assertEquals(accounting.transitions(), accounting.located() + accounting.unlocated());
    }
}

package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.preparation.SolverFeatures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static gecko.core.simulation.ZeroCrossingLocationTest.*;
import static org.junit.jupiter.api.Assertions.*;

/** Element-level contract of the adaptive electrical sub-step (ADR-0018). */
class AdaptiveStepTest {
    static final double R = 1e3;
    static final double C = 1e-6;
    static final double TAU = R * C;
    static final double GRID = 100e-6;
    static final double MIN = 0.1e-6;

    static SolverFeatures adaptive(double tolerance, double min, double max) {
        return new SolverFeatures(0, false, false, null, new AdaptiveStepSettings(tolerance, min, max));
    }

    /** DC 1 V, a resistor and a capacitor to ground. Node 2 is the capacitor voltage. */
    static CircuitNetlist rc() {
        double[] source = new double[21];
        source[0] = SourceType.QUELLE_DC_NEW;
        source[1] = 1;
        double[] capacitor = new double[13];
        capacitor[0] = capacitor[6] = capacitor[7] = C;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_C},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1},
                new double[][]{source, value(R), capacitor}, 2, 1, 3);
        return netlist;
    }

    /** DC 1 V into a series resistor, inductor and capacitor. */
    static CircuitNetlist rlc() {
        double[] source = new double[21];
        source[0] = SourceType.QUELLE_DC_NEW;
        source[1] = 1;
        double[] capacitor = new double[13];
        capacitor[0] = capacitor[6] = capacitor[7] = 1e-6;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_L, CircuitTypCore.LK_C},
                new int[]{1, 1, 2, 3}, new int[]{0, 2, 3, 0}, new int[]{1, -1, -1, -1},
                new double[][]{source, value(10), value(1e-3), capacitor}, 3, 1, 4);
        return netlist;
    }

    record RcRun(double maxError, long accepted, long rejected, long forced, double maxNonForcedEstimate, double seconds) { }

    static RcRun rcRun(SolverType method, double tolerance) {
        CircuitNetlist netlist = rc();
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method, adaptive(tolerance, MIN, GRID));
        double[] worst = {0};
        step.setSubStepListenerForTest((start, width, error, forced, be) -> { if (!forced) worst[0] = Math.max(worst[0], error); });
        double maxError = 0;
        long begin = System.nanoTime();
        for (int index = 0; index <= 50; index++) {
            double time = index * GRID;
            step.advanceInterval(GRID, time, null);
            maxError = Math.max(maxError, Math.abs(netlist.getLastNodeVoltages()[2] - (1 - Math.exp(-time / TAU))));
        }
        double seconds = (System.nanoTime() - begin) * 1e-9;
        var report = step.adaptiveReport();
        return new RcRun(maxError, report.accepted(), report.rejected(), report.forced(), worst[0], seconds);
    }

    @Test
    void trapezoidalRcStaysWithinOneMillivoltAtEveryGridSampleWithFarFewerStepsThanTheFixedMinimum() {
        var run = rcRun(SolverType.SOLVER_TRZ, 1e-3);
        // The fixed reference takes one step per minimum step over the same 5 ms.
        CircuitNetlist netlist = rc();
        ElectricalTimeStep fixed = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ);
        long begin = System.nanoTime();
        int fixedSteps = (int) Math.round(5e-3 / MIN);
        for (int index = 0; index <= fixedSteps; index++) fixed.advance(MIN, index * MIN, null);
        double fixedSeconds = (System.nanoTime() - begin) * 1e-9;
        System.out.printf("ADAPTIVE-RC-TRZ accepted=%d rejected=%d forced=%d maxError=%.3e adaptiveSeconds=%.4f "
                        + "fixedSteps=%d fixedSeconds=%.4f ratio=%.1f%n",
                run.accepted(), run.rejected(), run.forced(), run.maxError(), run.seconds(), fixedSteps + 1, fixedSeconds,
                (fixedSteps + 1.0) / run.accepted());
        assertTrue(run.maxError() <= 1e-3, "error " + run.maxError());
        assertTrue(run.accepted() * 5 <= fixedSteps, "accepted " + run.accepted());
        assertTrue(run.accepted() <= 10_000);
        assertEquals(0, run.forced());
        assertTrue(run.maxNonForcedEstimate() <= 1);
    }

    @Test
    void backwardEulerKeepsTheLocalEstimateBelowOneAndATighterToleranceReducesTheGlobalError() {
        var loose = rcRun(SolverType.SOLVER_BE, 1e-3);
        var tight = rcRun(SolverType.SOLVER_BE, 1e-4);
        System.out.printf("ADAPTIVE-RC-BE looseError=%.3e looseAccepted=%d tightError=%.3e tightAccepted=%d%n",
                loose.maxError(), loose.accepted(), tight.maxError(), tight.accepted());
        assertTrue(loose.maxNonForcedEstimate() <= 1);
        assertTrue(tight.maxNonForcedEstimate() <= 1);
        assertTrue(tight.maxError() < loose.maxError(), loose.maxError() + " then " + tight.maxError());
        assertTrue(tight.accepted() > loose.accepted());
    }

    static void assertSameHistories(ElectricalTimeStep expected, CircuitNetlist expectedNetlist,
            ElectricalTimeStep actual, CircuitNetlist actualNetlist, String message) {
        var left = expected.solverForTest();
        var right = actual.solverForTest();
        assertArrayEquals(left.getPALT(), right.getPALT(), message + " pALT");
        assertArrayEquals(left.getPALTALT(), right.getPALTALT(), message + " pALTALT");
        assertArrayEquals(left.getIALT(), right.getIALT(), message + " iALT");
        assertArrayEquals(left.getIALTALT(), right.getIALTALT(), message + " iALTALT");
        for (int element = 0; element < expectedNetlist.getElementCount(); element++) {
            assertArrayEquals(expectedNetlist.getParameter(element), actualNetlist.getParameter(element),
                    message + " parameters of element " + element);
        }
    }

    record SubStep(double start, double width, boolean backwardEuler) { }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void everyAcceptedSubStepEqualsTheSameWidthRunDirectlyAfterRejectedTrials(SolverType method) {
        CircuitNetlist netlist = rlc();
        CircuitNetlist twinNetlist = rlc();
        var features = adaptive(1e-9, 1e-7, 50e-6);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method, features);
        ElectricalTimeStep twin = new ElectricalTimeStep(twinNetlist, method, features);
        List<SubStep> accepted = new ArrayList<>();
        step.setSubStepListenerForTest((start, width, error, forced, be) -> accepted.add(new SubStep(start, width, be)));
        for (int index = 0; index < 4; index++) step.advanceInterval(50e-6, index * 50e-6, null);
        var report = step.adaptiveReport();
        assertTrue(report.rejected() > 0, "tolerance 1e-9 must reject: " + report);
        assertEquals(accepted.size(), report.accepted());
        assertTrue(accepted.stream().anyMatch(SubStep::backwardEuler));
        for (SubStep sub : accepted) twin.directSubStepForTest(sub.width(), sub.start() + sub.width(), sub.backwardEuler());
        assertSameHistories(twin, twinNetlist, step, netlist, "twin");
        // Every sub-step lands on the grid and the trace sees one row per grid step.
        double end = 0;
        for (SubStep sub : accepted.subList(1, accepted.size())) end = sub.start() + sub.width();
        assertEquals(3 * 50e-6, end, 1e-15);
    }

    @Test
    void theGridStepPublishesOnceAndNoTrialPublishes() {
        CircuitNetlist netlist = rc();
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, adaptive(1e-6, MIN, GRID));
        List<Double> traced = new ArrayList<>();
        for (int index = 0; index < 20; index++) step.advanceInterval(GRID, index * GRID, (time, volts, currents, circuit) -> traced.add(time));
        assertEquals(20, traced.size());
        for (int index = 0; index < 20; index++) assertEquals(index * GRID, traced.get(index));
        assertTrue(step.adaptiveReport().accepted() > 20, "sub-steps are not published");
    }

    @Test
    void aTrialThatSwitchesADiodeRestoresTheControlledSourceLimitAndDiodeState() {
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
        ElectricalTimeStep step = new ElectricalTimeStep(circuit, SolverType.SOLVER_BE, adaptive(1e-3, 1e-3, 0.125));
        step.advanceInterval(0.125, 0, null);
        circuit.getParameter(0)[1] = 2;
        double before = circuit.getParameter(1)[14];
        step.trialAndRestoreForTest(0.125, 0.125);
        assertEquals(before, circuit.getParameter(1)[14]);
        step.advanceInterval(0.125, 0.125, null);
        assertEquals(1, circuit.getParameter(1)[14], "the committed step keeps the limit");
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void halfWaveRectifierEventsStayWithinATenthOfTheGridWhileAdapting(SolverType method) {
        double dt = PERIOD / 50;
        CircuitNetlist netlist = rectifier(UF, 0);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method, adaptive(1e-3, dt / 1000, dt));
        run(step, 150, dt);
        List<ZeroCrossingEvent> events = step.events(0, 100);
        assertEquals(6, events.size());
        for (int period = 0; period < 3; period++) {
            assertEquals(analyticTurnOn(period, UF), events.get(2 * period).getCrossingTime(), dt / 10, "turn-on " + period);
            double analyticOff = period * PERIOD + (Math.PI - Math.asin(UF / VPK)) / OMEGA;
            assertEquals(analyticOff, events.get(2 * period + 1).getCrossingTime(), dt / 10, "turn-off " + period);
        }
        var report = step.adaptiveReport();
        assertEquals(6, report.located());
        assertEquals(0, report.unlocated());
        assertTrue(report.accepted() > 150);
    }

    @Test
    void inductiveExtinctionStaysWithinATenthOfTheGridWhileAdapting() {
        double dt = PERIOD / 50;
        double inductance = 20e-3;
        CircuitNetlist netlist = rectifier(UF, inductance);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, adaptive(1e-3, dt / 1000, dt));
        run(step, 45, dt);
        List<ZeroCrossingEvent> events = step.events(0, 10);
        assertEquals(2, events.size());
        assertEquals(analyticTurnOn(0, UF), events.get(0).getCrossingTime(), dt / 10);
        assertEquals(analyticExtinction(inductance), events.get(1).getCrossingTime(), dt / 10);
        assertEquals(0, step.adaptiveReport().unlocated());
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void aTightToleranceRejectsWithoutLosingEventsOrDiodeState(SolverType method) {
        double dt = PERIOD / 50;
        CircuitNetlist netlist = rectifier(UF, 20e-3);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method, adaptive(1e-6, dt / 1000, dt));
        run(step, 45, dt);
        var report = step.adaptiveReport();
        assertTrue(report.rejected() > 0, report.toString());
        List<ZeroCrossingEvent> events = step.events(0, 10);
        assertEquals(2, events.size(), report.toString());
        assertEquals(analyticTurnOn(0, UF), events.get(0).getCrossingTime(), dt / 10);
        assertEquals(0, report.unlocated());
    }

    @Test
    void aStepThatCannotShrinkIsForcedAndCountedAndNeverRepeats() {
        CircuitNetlist netlist = rlc();
        // The clamps leave no room to shrink, so every estimate above one is forced through at the minimum.
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, adaptive(1e-9, 50e-6, 50e-6));
        List<Double> widths = new ArrayList<>();
        step.setSubStepListenerForTest((start, width, error, forced, be) -> widths.add(width));
        for (int index = 0; index < 3; index++) step.advanceInterval(50e-6, index * 50e-6, null);
        var report = step.adaptiveReport();
        assertTrue(report.forced() > 0, report.toString());
        assertEquals(0, report.rejected(), "a step at the minimum is accepted at once");
        assertEquals(widths.size(), report.accepted());
        assertTrue(widths.stream().allMatch(width -> width > 0));
        assertEquals(3, report.accepted(), "the first step, then one forced sub-step for each of the two later grid steps");
    }

    @Test
    void aShortRetryBoundCannotLoopBecauseEachRetryShrinksTheStepAndAcceptanceIsForced() {
        CircuitNetlist netlist = rlc();
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, adaptive(1e-12, 1e-12, 50e-6));
        step.setSubStepListenerForTest((start, width, error, forced, be) -> assertTrue(width > 0 && Double.isFinite(width)));
        for (int index = 0; index < 3; index++) step.advanceInterval(50e-6, index * 50e-6, null);
        var report = step.adaptiveReport();
        assertTrue(report.minStep() >= 1e-12 * (1 - 1e-9), report.toString());
        assertTrue(report.rejected() <= 30L * report.accepted(), report.toString());
    }

    @Test
    void aRestartSubStepUsesTheBackwardEulerStampsAndHistoryOfAPureBackwardEulerSolver() {
        // A trapezoidal run restarts with Backward Euler after the first integration.
        CircuitNetlist trapezoidalNetlist = rlc();
        CircuitNetlist eulerNetlist = rlc();
        var features = adaptive(1e-3, 1e-7, 50e-6);
        ElectricalTimeStep trapezoidal = new ElectricalTimeStep(trapezoidalNetlist, SolverType.SOLVER_TRZ, features);
        ElectricalTimeStep euler = new ElectricalTimeStep(eulerNetlist, SolverType.SOLVER_BE, features);
        trapezoidal.advanceInterval(50e-6, 0, null);
        euler.advanceInterval(50e-6, 0, null);
        assertSameHistories(euler, eulerNetlist, trapezoidal, trapezoidalNetlist, "after the first integration");
        List<SubStep> restart = new ArrayList<>();
        double[][] committed = new double[3][];
        trapezoidal.setSubStepListenerForTest((start, width, error, forced, be) -> {
            if (!restart.isEmpty()) return;
            restart.add(new SubStep(start, width, be));
            committed[0] = trapezoidal.solverForTest().getPALT().clone();
            committed[1] = trapezoidal.solverForTest().getIALT().clone();
            committed[2] = trapezoidalNetlist.getParameter(3).clone();
        });
        trapezoidal.advanceInterval(50e-6, 50e-6, null);
        SubStep first = restart.getFirst();
        assertTrue(first.backwardEuler(), "the first sub-step after the start is a restart");
        // The same width on a solver that is Backward Euler by construction must give the same bits.
        euler.directSubStepForTest(first.width(), first.start() + first.width(), false);
        assertArrayEquals(euler.solverForTest().getPALT(), committed[0], "restart potentials are Backward Euler");
        assertArrayEquals(euler.solverForTest().getIALT(), committed[1], "restart currents are Backward Euler");
        assertArrayEquals(eulerNetlist.getParameter(3), committed[2], "the capacitor history term is Backward Euler");
        trapezoidal.setSubStepListenerForTest(null);
    }

    @Test
    void theEventCapAndTheBoundedLogHoldWhileAdapting() {
        double dt = PERIOD / 4;
        CircuitNetlist netlist = ladder(17);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, adaptive(1e-3, dt / 1000, dt));
        step.setEventRetention(3);
        run(step, 2, dt);
        var report = step.eventReport();
        assertEquals(ElectricalTimeStep.MAX_LOCATED_PER_STEP, report.located());
        assertEquals(1, report.unlocated());
        assertEquals(3, report.retained());
        assertEquals(13, report.dropped());
        assertEquals(report.located(), step.adaptiveReport().located());
        for (int branch = 0; branch < 17; branch++) assertEquals(RON, netlist.getParameter(1 + 2 * branch)[0]);
        List<ZeroCrossingEvent> events = step.events(0, 10);
        for (int branch = 0; branch < events.size(); branch++) {
            assertEquals(Math.asin((0.5 + 0.5 * branch) / VPK) / OMEGA, events.get(branch).getCrossingTime(), dt / 10);
        }
    }

    @Test
    void twoDiodesCommutatingInOneGridStepGiveSortedEventsWhileAdapting() {
        double dt = PERIOD / 20;
        CircuitNetlist netlist = commutation();
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, adaptive(1e-3, dt / 1000, dt));
        run(step, 20, dt);
        var events = step.events(0, 100);
        double crossing = 3.0 / 8 * PERIOD;
        long atCommutation = events.stream().filter(event -> Math.abs(event.getCrossingTime() - crossing) <= dt / 10).count();
        assertEquals(2, atCommutation, events.toString());
        for (int index = 1; index < events.size(); index++) {
            assertTrue(events.get(index - 1).getCrossingTime() <= events.get(index).getCrossingTime(), "sorted");
        }
        assertEquals(0, step.eventReport().unlocated());
    }

    @Test
    void gearShichmanIsRejectedBeforeAnyAllocation() {
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new ElectricalTimeStep(rc(), SolverType.SOLVER_GS, adaptive(1e-3, MIN, GRID)));
        assertEquals(SimulationTiming.GS_ADAPTIVE_MESSAGE, failure.getMessage());
    }

    @Test
    void anAdaptiveStepWithTheFeatureOffIsTheLegacyStep() {
        CircuitNetlist a = rc();
        CircuitNetlist b = rc();
        ElectricalTimeStep off = new ElectricalTimeStep(a, SolverType.SOLVER_TRZ, new SolverFeatures(0, false, false, null, null));
        ElectricalTimeStep legacy = new ElectricalTimeStep(b, SolverType.SOLVER_TRZ);
        for (int index = 0; index < 50; index++) {
            off.advanceInterval(GRID, index * GRID, null);
            legacy.advanceInterval(GRID, index * GRID, null);
        }
        assertSameHistories(legacy, b, off, a, "off");
        assertEquals(AdaptiveStepReport.empty(), off.adaptiveReport());
    }
}

package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

/**
 * An adaptive interval spans from the last published grid time to the new one. After a step change that gap differs
 * from the new nominal step, and the RC response must still follow the analytic solution at every timestamp.
 */
class AdaptiveStepGridChangeTest {
    private static final double MIN = 1e-5;
    private static final double MAX = 0.1;
    private static final double TOLERANCE = 1e-4;
    private static final AdaptiveStepSettings SETTINGS = new AdaptiveStepSettings(1e-3, MIN, MAX);

    /** 1 V source, 1 ohm, 1 farad: v(t) = 1 - exp(-t). */
    private static CircuitModel rc() {
        var model = new CircuitModel();
        model.addCircuitComponent(component(4, "V", new double[]{401, 1}, new String[]{"1"}, "0"));
        model.addCircuitComponent(component(1, "R", new double[]{1}, new String[]{"1"}, "2"));
        model.addCircuitComponent(component(3, "C", new double[]{1, 0}, new String[]{"2"}, "0"));
        return model;
    }

    private static PreparedCircuit prepared(CircuitModel model, double step, double duration) {
        model.setTimeStep(step);
        model.setSimulationDuration(duration);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(new SolverOverrides(
                SolverType.SOLVER_TRZ, null, null, new SolverFeatures(0, false, false, null, SETTINGS)), Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result).execution();
    }

    private record Sub(double start, double width) { }

    /** The capacitor voltage is the signal whose value follows the analytic response. */
    private static void assertRc(SimulationSession.Observation observation, double... expectedTimes) {
        assertArrayEquals(expectedTimes, observation.times(), 1e-12);
        float[][] values = observation.values();
        int row = -1;
        for (int candidate = 0; candidate < values.length && row < 0; candidate++) {
            boolean matches = true;
            for (int index = 0; index < expectedTimes.length; index++) {
                matches &= Math.abs(values[candidate][index] - (1 - Math.exp(-expectedTimes[index]))) < TOLERANCE;
            }
            if (matches) row = candidate;
        }
        assertTrue(row >= 0, "no signal follows 1 - exp(-t) at " + java.util.Arrays.toString(expectedTimes)
                + " got " + java.util.Arrays.deepToString(values));
    }

    /** Sub-steps must tile the elapsed gap with no overlap or skip, and respect both clamps of the new grid. */
    private static void assertTiles(List<Sub> subs, double from, double to, double nominal) {
        double cursor = from;
        for (Sub sub : subs) {
            assertEquals(cursor, sub.start(), 1e-12, "no overlap or gap at " + cursor);
            assertTrue(sub.width() > 0, "progress");
            assertTrue(sub.width() <= Math.min(MAX, nominal) * (1 + 1e-9), "max clamp of the new grid " + sub.width());
            cursor += sub.width();
        }
        assertEquals(to, cursor, 1e-12, "lands on the grid time");
    }

    /** Runs to the end of a phase, extends with the new step, and checks the first transition interval. */
    private void extendAndCheck(double step0, double end0, double step1, double end1, double... times) {
        try (var session = SimulationSession.open(prepared(rc(), step0, end0), ExecutionOptions.defaults())) {
            session.run();
            double published = session.status().time();
            double next = session.status().nextTime();
            var subs = new ArrayList<Sub>();
            session.setSubStepListenerForTest((start, width, error, forced, be) -> subs.add(new Sub(start, width)));
            session.extend(end1, step1);
            assertEquals(next, session.status().nextTime(), 0, "the retained next timestamp stays");
            session.step();
            assertTiles(subs, published, next, step1);
            session.run();
            assertRc(session.observe(0, 10000), times);
            assertEquals(times.length, session.status().committedSteps(), "one record per grid step");
        }
    }

    @Test
    void aLargerStepAfterExtendFollowsTheAnalyticRcAtEveryTimestamp() {
        extendAndCheck(0.1, 0.2, 0.2, 0.6, 0, 0.1, 0.2, 0.3, 0.5);
    }

    @Test
    void aSmallerStepAfterExtendFollowsTheAnalyticRcAtEveryTimestamp() {
        extendAndCheck(0.2, 0.4, 0.1, 1.0, 0, 0.2, 0.4, 0.6, 0.7, 0.8, 0.9, 1.0);
    }

    @Test
    void repeatedPausesAndStepChangesStayOnTheAnalyticRc() {
        try (var session = SimulationSession.open(prepared(rc(), 0.1, 0.2), ExecutionOptions.defaults())) {
            session.run();
            session.extend(0.6, 0.2);
            session.run();
            session.extend(1.0, 0.05);
            session.run();
            session.extend(1.4, 0.05);
            session.run();
            var observation = session.observe(0, 10000);
            double[] times = observation.times();
            assertEquals(0.7, times[5], 1e-12);
            assertRc(observation, times);
            for (int index = 1; index < times.length; index++) assertTrue(times[index] > times[index - 1]);
        }
    }

    @Test
    void warmupToMainWithADifferentStepFollowsTheAnalyticRc() {
        var model = rc();
        model.setPreSimulationTime(0.2);
        model.setPreSimulationTimeStep(0.1);
        try (var session = SimulationSession.open(prepared(model, 0.2, 0.4), ExecutionOptions.defaults())) {
            var subs = new ArrayList<Sub>();
            session.setSubStepListenerForTest((start, width, error, forced, be) -> subs.add(new Sub(start, width)));
            session.run();
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());
            var observation = session.observe(0, 10000);
            assertRc(observation, observation.times());
            assertEquals(0.3, observation.times()[3], 1e-12, "the first main time keeps the retained timestamp");
            double[] times = observation.times();
            var transition = subs.stream().filter(sub -> sub.start() >= 0.2 - 1e-12 && sub.start() < 0.3 - 1e-12).toList();
            assertTiles(transition, 0.2, 0.3, 0.2);
            assertEquals(times.length, session.status().committedSteps());
        }
    }

    @Test
    void aSameGridContinuationAndAFreshRunTakeTheSameSubSteps() {
        var whole = new ArrayList<Sub>();
        try (var session = SimulationSession.open(prepared(rc(), 0.1, 0.6), ExecutionOptions.defaults())) {
            session.setSubStepListenerForTest((start, width, error, forced, be) -> whole.add(new Sub(start, width)));
            session.run();
        }
        var parts = new ArrayList<Sub>();
        try (var session = SimulationSession.open(prepared(rc(), 0.1, 0.2), ExecutionOptions.defaults())) {
            session.setSubStepListenerForTest((start, width, error, forced, be) -> parts.add(new Sub(start, width)));
            session.run();
            session.extend(0.6);
            session.run();
        }
        assertEquals(whole, parts, "same-grid continuation is deterministic and unchanged");
    }

    @Test
    void aPreparedEngineRunReportsEventMetadataForAnAdaptiveOnlyCircuit() {
        var result = new HeadlessSimulationEngine().runSimulation(prepared(rc(), 0.1, 0.5), ExecutionOptions.defaults(),
                () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        var metadata = result.getMetadata();
        for (String key : List.of("zeroCrossingLocated", "zeroCrossingUnlocated", "zeroCrossingFirstUnlocatedTime",
                "zeroCrossingRetained", "zeroCrossingDropped")) {
            assertTrue(metadata.containsKey(key), key);
        }
        assertEquals(0L, metadata.get("zeroCrossingLocated"), "an RC circuit has no switching event");
        assertTrue(metadata.get("adaptiveStep") instanceof AdaptiveStepReport);
    }
}

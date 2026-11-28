package gecko.core.simulation;

import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The logging interval counts from the first main step, never from the unrecorded search steps. */
class PeriodicSteadyStateLoggingTest {
    private static CircuitModel constant(double duration) {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(duration);
        model.addControlComponent(component(3, "one", new double[]{1}, new String[0], "one"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"one"}));
        return model;
    }

    /** Period 0.3 s is 3 steps, and the constant circuit converges after 3 periods, so the search is 9 steps. */
    private static PreparedCircuit prepared(double duration, double period) {
        var preparation = new CircuitPreparation();
        var features = new SolverFeatures(0, false, false, new PeriodicSteadyStateSettings(period, 1e-4, 10));
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(constant(duration)),
                new PreparationRequest(new SolverOverrides(SolverType.SOLVER_BE, null, null, features), Map.of())));
        return ready.execution();
    }

    private static double[] times(PreparedCircuit circuit, int interval) {
        try (var session = SimulationSession.open(circuit, new ExecutionOptions(true, interval))) {
            session.run();
            return session.observe(0, 10000).times();
        }
    }

    @Test
    void theFirstMainRecordIsTheFirstMainStepWhenTheSearchLengthIsNotDivisibleByTheInterval() {
        var circuit = prepared(0.5, 0.3);
        double[] every = times(circuit, 1);
        assertEquals(0.9, every[0], 1e-9, "the main phase starts after 9 search steps");
        double[] second = times(circuit, 2);
        assertEquals(0.9, second[0], 1e-9);
        assertEquals((every.length + 1) / 2, second.length);
        for (int row = 0; row < second.length; row++) assertEquals(every[2 * row], second[row], 1e-9);
    }

    @Test
    void thePreparedEngineRecordsTheSameRowsAsTheSession() {
        var circuit = prepared(0.5, 0.3);
        var result = new HeadlessSimulationEngine().runSimulation(circuit, new ExecutionOptions(true, 2), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        double[] expected = times(circuit, 2);
        double[] actual = result.getTimeArray();
        assertEquals(0.9, actual[0], 1e-9);
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) assertEquals(expected[row], actual[row], 1e-9);
    }

    @Test
    void anAlignedSearchStillRecordsTheFirstMainStep() {
        assertEquals(0.6, times(prepared(0.5, 0.2), 2)[0], 1e-9);
    }

    @Test
    void aContinuationKeepsTheLoggingPhaseOfAnUninterruptedRun() {
        for (int interval : new int[]{2, 3}) {
            double[] whole = times(prepared(1.0, 0.3), interval);
            var circuit = prepared(0.5, 0.3);
            try (var session = SimulationSession.open(circuit, new ExecutionOptions(true, interval))) {
                session.run();
                session.extend(session.status().endTime() + 0.5);
                session.run();
                double[] split = session.observe(0, 10000).times();
                assertEquals(whole.length, split.length, "interval " + interval);
                for (int row = 0; row < whole.length; row++) assertEquals(whole[row], split[row], 1e-9, "interval " + interval);
            }
        }
    }

    @Test
    void theSearchRecordsNoRowsWhenLoggingIsSparse() {
        try (var session = SimulationSession.open(prepared(0.5, 0.3), new ExecutionOptions(true, 3))) {
            for (int step = 0; step < 9; step++) session.step();
            assertEquals(0, session.observe(0, 100).times().length);
        }
    }
}

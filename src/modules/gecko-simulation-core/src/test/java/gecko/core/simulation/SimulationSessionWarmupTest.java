package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class SimulationSessionWarmupTest {
    @Test
    void warmupKeepsHistoryAndItsLastCallbackBeforeTheMainSegmentStarts() {
        var ready = prepare(model());
        try (var session = SimulationSession.open(ready, ExecutionOptions.defaults())) {
            assertEquals(SimulationSession.Phase.WARMUP, session.status().phase());
            var phases = new ArrayList<SimulationSession.Phase>();
            session.run(() -> false, sample -> phases.add(session.status().phase()));
            assertEquals(List.of(SimulationSession.Phase.WARMUP, SimulationSession.Phase.WARMUP,
                    SimulationSession.Phase.MAIN, SimulationSession.Phase.MAIN,
                    SimulationSession.Phase.MAIN, SimulationSession.Phase.MAIN), phases);
            assertEquals(6, session.status().committedSteps());
            assertEquals(4, session.status().segmentSteps());
            assertArrayEquals(new double[]{0, 0.2, 0.4, 0.525, 0.65, 0.775}, session.observe(0, 10).times(), 1e-12);
            assertArrayEquals(new double[]{0.4, 0.525, 0.65, 0.775}, session.observeCurrentSegment(0, 10).times(), 1e-12);
            assertArrayEquals(new float[]{0.425f, 0.55f, 0.675f, 0.8f},
                    session.observeCurrentSegment(0, 10).values()[1], 1e-6f);
        }
    }

    @Test
    void theEngineReturnsMainSegmentStepsAndAbsoluteTimestampsAfterOvershoot() {
        var engine = new HeadlessSimulationEngine();
        var result = engine.runSimulation(prepare(model()), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(4, result.getTotalTimeSteps());
        assertEquals(0.775, result.getSimulatedTime(), 1e-12);
        assertArrayEquals(new double[]{0.4, 0.525, 0.65, 0.775}, result.getTimeArray(), 1e-12);
        assertArrayEquals(new float[]{0.425f, 0.55f, 0.675f, 0.8f}, result.getSignalData(1), 1e-6f);
        assertEquals(4, engine.getCurrentStep());
        assertEquals(0.8, engine.getEndTime(), 1e-12);
    }

    @Test
    void aDisabledNegativeWarmupIgnoresItsUnusedInvalidStep() {
        var model = model();
        model.setPreSimulationTime(-0.3);
        model.setPreSimulationTimeStep(Double.NaN);
        try (var session = SimulationSession.open(prepare(model), ExecutionOptions.defaults())) {
            assertEquals(SimulationSession.Phase.MAIN, session.status().phase());
            session.run();
            assertEquals(5, session.status().segmentSteps());
            assertEquals(5, session.status().committedSteps());
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.375, 0.5}, session.observeCurrentSegment(0, 10).times());
        }
    }

    @Test
    void anEnabledWarmupRejectsAnInvalidStepBeforeExecution() {
        for (double step : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var model = model();
            model.setPreSimulationTimeStep(step);
            var preparation = new CircuitPreparation();
            var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
            var invalid = assertInstanceOf(PreparationResult.Inspectable.class, outcome);
            assertEquals(ReadinessReport.Outcome.INVALID, invalid.report().outcome());
            assertTrue(invalid.report().diagnostics().stream().anyMatch(finding ->
                    finding.code().equals("INVALID_SETTING") && finding.parameterPath().equals("preSimulationTimeStep")));
            var settings = new gecko.core.allg.SolverSettingsCore(SolverType.SOLVER_BE, 0.125, 0.5)
                    .withPreCalculation(0.3, step);
            assertThrows(IllegalArgumentException.class,
                    () -> SimulationSession.fromModel(model, settings, ExecutionOptions.defaults()));
        }
    }

    @Test
    void aFailedWarmupCannotPoisonTheNextDisabledWarmupRun() {
        var failing = model();
        failing.addCircuitComponent(component(4, "source1", new double[]{401, 10}, new String[]{"1"}, "0"));
        failing.addCircuitComponent(component(4, "source2", new double[]{401, 20}, new String[]{"1"}, "0"));
        var engine = new HeadlessSimulationEngine();
        var failure = engine.runSimulation(prepare(failing), ExecutionOptions.defaults(), () -> false);
        assertEquals(SimulationResult.Status.FAILED, failure.getStatus());
        var valid = model();
        valid.setPreSimulationTime(-1);
        valid.setPreSimulationTimeStep(Double.NaN);
        var result = engine.runSimulation(prepare(valid), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(5, result.getTotalTimeSteps());
        assertArrayEquals(new double[]{0, 0.125, 0.25, 0.375, 0.5}, result.getTimeArray());
        assertEquals(0.5, result.getSimulatedTime());
    }

    @Test
    void warmupOvershootCanProduceAnEmptyMainSegmentWithoutAFabricatedStep() {
        var model = model();
        model.setSimulationDuration(0.05);
        var engine = new HeadlessSimulationEngine();
        var result = engine.runSimulation(prepare(model), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(0, result.getTotalTimeSteps());
        assertEquals(0, result.getSimulatedTime());
        assertEquals(0, result.getTimeArray().length);
        try (var session = SimulationSession.open(prepare(model), ExecutionOptions.defaults())) {
            session.run();
            assertEquals(2, session.status().committedSteps());
            assertEquals(0, session.status().segmentSteps());
            assertEquals(SimulationSession.Phase.MAIN, session.status().phase());
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());
            assertArrayEquals(new double[]{0, 0.2}, session.observe(0, 10).times(), 1e-12);
        }
    }

    private static CircuitModel model() {
        var model = new CircuitModel();
        model.setSolverType(SolverType.SOLVER_BE);
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.5);
        model.setPreSimulationTime(0.3);
        model.setPreSimulationTimeStep(0.2);
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(3, "constant", new double[]{1}, new String[0], "drive"));
        model.addControlComponent(component(10, "integrator", new double[]{0, 1}, new String[]{"drive"}, "integrated"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock", "integrated"}));
        return model;
    }

    private static PreparedCircuit prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults())).execution();
    }
}

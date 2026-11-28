package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class SimulationSessionTimingReviewTest {
    @Test
    void anUnrepresentableMainClockRejectsBeforeTheTransitionCommits() {
        var model = clock(Math.scalb(1.0, -55), Math.scalb(1.0, -50));
        model.setPreSimulationTime(Math.nextDown(1.0));
        model.setPreSimulationTimeStep(0.5);
        try (var session = open(model, new ExecutionOptions(false, 1))) {
            assertEquals(0, session.step().time());
            assertEquals(0.5, session.step().time());
            assertEquals(1, session.status().nextTime());
            assertThrows(IllegalArgumentException.class, session::step);
            assertEquals(2, session.status().committedSteps());
            assertEquals(SimulationSession.Phase.WARMUP, session.status().phase());
            assertEquals(SimulationSession.State.FAILED, session.status().state());
        }
    }

    @Test
    void aOneSampleContinuationUsesRemainingSpanInsteadOfElapsedTime() {
        try (var session = open(clock(1, 1), ExecutionOptions.defaults())) {
            session.run();
            assertEquals(2, session.status().nextTime());
            assertDoesNotThrow(() -> session.extend(2, 1e-10));
            session.run();
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());
            assertEquals(3, session.status().committedSteps());
            assertEquals(1, session.status().segmentSteps());
            assertArrayEquals(new double[]{2}, session.observeCurrentSegment(0, 10).times());
            assertTrue(Double.isFinite(session.status().time()));
            assertTrue(Double.isFinite(session.status().nextTime()));
            assertTrue(Double.isFinite(session.status().endTime()));
        }
    }

    @Test
    void extendingWarmupRetainsTheConfiguredMainDuration() {
        var model = clock(0.25, 0.25);
        model.setPreSimulationTime(0.25);
        model.setPreSimulationTimeStep(0.25);
        try (var session = open(model, ExecutionOptions.defaults())) {
            assertDoesNotThrow(() -> session.extend(1));
            assertDoesNotThrow(() -> session.run(() -> session.status().committedSteps() >= 8));
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());
            assertEquals(1.25, session.status().endTime());
            assertEquals(6, session.status().committedSteps());
            assertEquals(1, session.status().segmentSteps());
            assertArrayEquals(new double[]{0, 0.25, 0.5, 0.75, 1, 1.25}, session.observe(0, 10).times());
            assertArrayEquals(new double[]{1.25}, session.observeCurrentSegment(0, 10).times());
        }
    }

    @Test
    void crossingABinary64BoundaryCannotCommitAStagnantStep() {
        double initialStep = Math.scalb(Math.nextDown(1.0), -1);
        try (var session = open(clock(initialStep, initialStep), ExecutionOptions.defaults())) {
            session.run();
            assertDoesNotThrow(() -> session.extend(Math.nextUp(1.0), Math.scalb(1.0, -53)));
            assertEquals(Math.nextDown(1.0), session.step().time());
            assertEquals(1, session.status().nextTime());
            assertThrows(IllegalArgumentException.class, session::step);
            assertEquals(3, session.status().committedSteps());
            assertEquals(Math.nextDown(1.0), session.status().time());
            assertEquals(SimulationSession.State.FAILED, session.status().state());
        }
    }

    private static CircuitModel clock(double step, double duration) {
        var model = new CircuitModel();
        model.setTimeStep(step);
        model.setSimulationDuration(duration);
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock"}));
        return model;
    }

    private static SimulationSession open(CircuitModel model, ExecutionOptions options) {
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        return SimulationSession.open(ready.execution(), options);
    }
}

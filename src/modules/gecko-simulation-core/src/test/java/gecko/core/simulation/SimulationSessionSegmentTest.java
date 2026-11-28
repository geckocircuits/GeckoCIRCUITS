package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class SimulationSessionSegmentTest {
    @Test
    void anExtensionStartsACurrentSegmentWithoutChangingCumulativeObservations() {
        try (var session = open(0.125)) {
            session.run();
            assertEquals(SimulationSession.Phase.MAIN, session.status().phase());
            assertEquals(2, session.status().segmentSteps());
            var original = session.observeCurrentSegment(0, 10);
            session.extend(0.5, 0.25);
            assertEquals(0, session.status().segmentSteps());
            assertEquals(2, session.status().committedSteps());
            session.run();
            assertEquals(2, session.status().segmentSteps());
            assertEquals(4, session.status().committedSteps());
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.5}, session.observe(0, 10).times());
            assertArrayEquals(new double[]{0.25, 0.5}, session.observeCurrentSegment(0, 10).times());
            assertArrayEquals(new double[]{0, 0.125}, original.times());
        }
    }

    @Test
    void aPausedResumeRetainsItsCurrentRecording() {
        try (var session = open(0.5)) {
            session.run(() -> false, sample -> session.pause());
            assertEquals(1, session.status().segmentSteps());
            var first = session.observeCurrentSegment(0, 10);
            session.run();
            assertEquals(5, session.status().segmentSteps());
            assertEquals(5, session.status().committedSteps());
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.375, 0.5}, session.observeCurrentSegment(0, 10).times());
            assertArrayEquals(new double[]{0}, first.times());
        }
    }

    @Test
    void theDesktopCursorAtTheEndStartsANewSegmentEvenWhenPaused() {
        try (var session = open(0.25)) {
            session.run(() -> false, sample -> { if (sample.time() == 0.125) session.pause(); });
            assertEquals(SimulationSession.State.PAUSED, session.status().state());
            assertEquals(session.status().endTime(), session.status().nextTime());
            session.extend(0.5);
            assertEquals(0, session.status().segmentSteps());
            session.run();
            assertEquals(3, session.status().segmentSteps());
            assertEquals(5, session.status().committedSteps());
            assertArrayEquals(new double[]{0.25, 0.375, 0.5}, session.observeCurrentSegment(0, 10).times());
        }
    }

    private static SimulationSession open(double duration) {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(duration);
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        return SimulationSession.open(ready.execution(), ExecutionOptions.defaults());
    }
}

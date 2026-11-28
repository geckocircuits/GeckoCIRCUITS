package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class SimulationSessionContinuationTest {
    @Test
    void aChangedStepKeepsElectricalHistoryAndPreviousRecordingTimestamps() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.125);
        model.setSolverType(SolverType.SOLVER_BE);
        model.setDataContainerSignals(new String[]{"ground", "source", "capacitor"});
        model.addCircuitComponent(component(4, "source", new double[]{401, 10}, new String[]{"1"}, "0"));
        model.addCircuitComponent(component(1, "resistor", new double[]{1}, new String[]{"1"}, "2"));
        model.addCircuitComponent(component(3, "capacitor", new double[]{1, 0}, new String[]{"2"}, "0"));
        try (var session = open(model)) {
            session.run();
            var previous = session.observe(0, 2);
            assertArrayEquals(new double[]{0, 0.125}, previous.times());
            session.extend(0.5, 0.25);
            assertEquals(0.25, session.status().nextTime());
            assertEquals(0.25, session.status().stepWidth());
            session.run();
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.5}, session.observe(0, 4).times());
            assertArrayEquals(new float[]{1.1111111111111112f, 2.0987654320987654f,
                    3.6790123456790123f, 4.94320987654321f}, session.observe(0, 4).values()[2], 1e-6f);
            assertArrayEquals(new double[]{0, 0.125}, previous.times());
        }
    }

    @Test
    void delayHistoryAndSegmentRecordingsSurviveMultipleStepChanges() {
        try (var session = open(delay(0.5, 1, 1))) {
            session.run();
            var firstRecording = SimulationResult.builder().dataContainer(session.recording()).build();
            session.extend(2, 0.25);
            session.run();
            assertArrayEquals(new double[]{0, 0.5, 1, 1.5, 1.75, 2}, session.observe(0, 6).times());
            assertArrayEquals(new float[]{0, 0, 0, 0.25f, 0.5f, 0.75f}, session.observe(0, 6).values()[1]);
            var secondRecording = SimulationResult.builder().dataContainer(session.recording()).build();
            session.extend(3, 0.5);
            session.run();
            assertArrayEquals(new double[]{0, 0.5, 1, 1.5, 1.75, 2, 2.25, 2.75}, session.observe(0, 8).times());
            assertArrayEquals(new float[]{0, 0.5f, 1, 1.5f, 1.75f, 2, 2.25f, 2.75f}, session.observe(0, 8).values()[0]);
            assertArrayEquals(new double[]{0, 0.5, 1}, firstRecording.getTimeArray());
            assertArrayEquals(new double[]{0, 0.5, 1, 1.5, 1.75, 2}, secondRecording.getTimeArray());
            assertArrayEquals(new float[]{0, 0, 0, 0.25f, 0.5f, 0.75f}, secondRecording.getSignalData(1));
        }
    }

    @Test
    void rejectedSettingsLeaveTheOldGridAndHistoryUsable() {
        try (var session = open(delay(0.5, 1, 1))) {
            session.run();
            var before = session.status();
            for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MIN_VALUE}) {
                assertThrows(IllegalArgumentException.class, () -> session.extend(2, invalid));
                assertEquals(before, session.status());
            }
            assertThrows(IllegalArgumentException.class, () -> session.extend(0.9, 0.25));
            assertThrows(IllegalArgumentException.class, () -> session.extend(1.25, 0.25));
            assertEquals(before, session.status());
            session.extend(2);
            session.run();
            assertArrayEquals(new double[]{0, 0.5, 1, 1.5, 2}, session.observe(0, 5).times());
            assertArrayEquals(new float[]{0, 0, 0, 0.5f, 1}, session.observe(0, 5).values()[1]);
        }
    }

    @Test
    void anOversizedDelayTransitionCannotPublishTheNewStepOrRecording() {
        try (var session = open(delay(1, 1, 1000))) {
            session.run();
            var before = session.status();
            var recording = session.recording();
            var failure = assertThrows(IllegalArgumentException.class, () -> session.extend(2, 1e-9));
            assertTrue(failure.getMessage().contains("Delay history"), failure.getMessage());
            assertEquals(before, session.status());
            assertSame(recording, session.recording());
            assertArrayEquals(new double[]{0, 1}, session.observe(0, 2).times());
            session.extend(3);
            session.run();
            assertArrayEquals(new double[]{0, 1, 2, 3}, session.observe(0, 4).times());
            assertArrayEquals(new float[]{0, 0, 0, 0}, session.observe(0, 4).values()[1]);
        }
    }

    @Test
    void aBeforeStartChangeUsesTheSelectedGridForInitialization() {
        try (var session = open(delay(0.5, 0.5, 1))) {
            session.extend(1, 0.25);
            assertEquals(0, session.status().committedSteps());
            session.run();
            assertArrayEquals(new double[]{0, 0.25, 0.5, 0.75, 1}, session.observe(0, 5).times());
            assertArrayEquals(new float[]{0, 0.25f, 0.5f, 0.75f, 1}, session.observe(0, 5).values()[0]);
            assertArrayEquals(new float[]{0, 0, 0, 0, 0}, session.observe(0, 5).values()[1]);
        }
    }

    @Test
    void aPausedSegmentCanExtendOnANewGridWithoutResettingTheDelay() {
        try (var session = open(delay(0.5, 1, 1))) {
            session.run(() -> false, sample -> session.pause());
            assertEquals(SimulationSession.State.PAUSED, session.status().state());
            assertEquals(1, session.status().committedSteps());
            session.extend(2, 0.25);
            assertEquals(0.5, session.status().nextTime());
            session.run();
            assertArrayEquals(new double[]{0, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2}, session.observe(0, 8).times());
            assertArrayEquals(new float[]{0, 0, 0, 0, 0, 0.5f, 0.75f, 1}, session.observe(0, 8).values()[1]);
        }
    }

    @Test
    void outputWindowKeepsTheAbsoluteIntervalGridAndPreWindowControlHistory() {
        var fullModel = delay(0.25, 1, 0.75);
        var croppedModel = delay(0.25, 1, 0.75);
        croppedModel.setOutputStartTime(0.8);
        var logging = new ExecutionOptions(true, 2);
        try (var full = open(fullModel, logging); var cropped = open(croppedModel, logging)) {
            full.run();
            var early = new java.util.ArrayList<SimulationSession.Sample>();
            cropped.run(() -> false, sample -> {
                early.add(sample);
                if (early.size() == 2) cropped.pause();
            });
            assertEquals(2, early.size());
            assertFalse(early.getFirst().inRecordingWindow());
            assertEquals(0, cropped.observe(0, 10).times().length);
            cropped.run();
            assertArrayEquals(new double[]{1}, cropped.observe(0, 10).times());
            full.extend(2, 0.25);
            cropped.extend(2, 0.25);
            full.run();
            cropped.run();
            var original = full.observe(0, 10);
            var saved = cropped.observe(0, 10);
            assertArrayEquals(new double[]{1, 1.5, 2}, saved.times());
            for (int row = 0; row < saved.values().length; row++) {
                assertArrayEquals(java.util.Arrays.copyOfRange(original.values()[row], 2, 5), saved.values()[row]);
            }
            assertEquals(0.25f, saved.values()[1][0]);
        }
    }

    @Test
    void aRoundedTimestampBeforeTheBoundaryIsNotExported() {
        var fullModel = delay(0.1, 1.5, 0.5);
        var croppedModel = delay(0.1, 1.5, 0.5);
        croppedModel.setOutputStartTime(1);
        try (var full = open(fullModel); var cropped = open(croppedModel)) {
            var fullSamples = new java.util.ArrayList<SimulationSession.Sample>();
            full.run(() -> false, fullSamples::add);
            assertTrue(fullSamples.get(10).time() < 1, "The prior grid sample rounds below the boundary.");
            var retained = fullSamples.stream().filter(sample -> sample.time() >= 1).toList();
            cropped.run();
            var observed = cropped.observe(0, 20);
            assertEquals(retained.size(), observed.times().length);
            assertEquals(retained.getFirst().time(), observed.times()[0]);
            assertTrue(java.util.Arrays.stream(observed.times()).allMatch(time -> time >= 1));
            for (int i = 0; i < retained.size(); i++) {
                assertEquals(retained.get(i).time(), observed.times()[i], 1e-14);
                assertEquals(retained.get(i).values()[1], observed.values()[1][i]);
            }
        }
    }

    private static CircuitModel delay(double dt, double duration, double delay) {
        var model = new CircuitModel();
        model.setTimeStep(dt);
        model.setSimulationDuration(duration);
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(25, "delay", new double[]{delay}, new String[]{"clock"}, "delayed"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock", "delayed"}));
        return model;
    }

    private static SimulationSession open(CircuitModel model) {
        return open(model, ExecutionOptions.defaults());
    }

    private static SimulationSession open(CircuitModel model, ExecutionOptions options) {
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        return SimulationSession.open(ready.execution(), options);
    }
}

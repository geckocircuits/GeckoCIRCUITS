package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class SessionHostTest {
    @Test
    void concurrentReadersDoNotChangeResults() throws Exception {
        var circuit = prepare(model(0.125 * 2000));
        var expected = new ArrayList<SimulationSession.Sample>();
        String[] names;
        try (var reference = SimulationSession.open(circuit, ExecutionOptions.defaults())) {
            assertEquals(SimulationSession.State.COMPLETED, reference.run().state());
            names = reference.signalNames();
            for (int first = 0; first < reference.status().committedSteps(); first += 10000) {
                addSamples(reference.observe(first, 10000), expected);
            }
        }
        var reader = Executors.newSingleThreadExecutor();
        try (var host = SessionHost.open(circuit, ExecutionOptions.defaults())) {
            assertArrayEquals(names, host.signalNames());
            var run = host.run();
            var observation = host.observe(0, 10000);
            var samples = reader.submit(() -> {
                var drained = new ArrayList<SimulationSession.Sample>();
                long deadline = System.nanoTime() + SECONDS.toNanos(10);
                while (!run.isDone()) {
                    assertNotNull(host.status());
                    host.drainSamples(drained::add);
                    assertTrue(System.nanoTime() < deadline, "The run must complete before the reader deadline.");
                }
                host.drainSamples(drained::add);
                return drained;
            });
            assertEquals(SimulationSession.State.COMPLETED, run.get(10, SECONDS).state());
            var actual = samples.get(10, SECONDS);
            assertSamplesEqual(expected, actual);
            var observed = new ArrayList<SimulationSession.Sample>();
            addSamples(observation.get(10, SECONDS), observed);
            assertSamplesEqual(expected, observed);
            assertEquals(0, host.drainSamples(sample -> fail("No sample must be delivered twice.")));
        } finally {
            reader.shutdownNow();
            assertTrue(reader.awaitTermination(10, SECONDS));
        }
    }

    @Test
    void pauseBeforeRunIsSticky() throws Exception {
        try (var host = open(0.25)) {
            host.pause();
            var paused = host.run().get(10, SECONDS);
            assertEquals(SimulationSession.State.PAUSED, paused.state());
            assertEquals(0, paused.committedSteps());
            assertEquals(SimulationSession.State.COMPLETED, host.run().get(10, SECONDS).state());
            assertEquals(3, host.status().committedSteps());
        }
    }

    @Test
    void runUntilStopsAtTarget() throws Exception {
        try (var host = open(2.5)) {
            var paused = host.runUntil(0.125 * 10).get(10, SECONDS);
            assertEquals(SimulationSession.State.PAUSED, paused.state());
            assertTrue(paused.time() >= 1.25);
            assertTrue(paused.time() < 1.25 + 0.125);
            assertEquals(11, paused.committedSteps());
            assertEquals(SimulationSession.State.COMPLETED, host.run().get(10, SECONDS).state());
        }
    }

    @Test
    void pauseDuringRunStopsAtCommittedStep() throws Exception {
        var reader = Executors.newSingleThreadExecutor();
        var entered = new CountDownLatch(1);
        var paused = new CountDownLatch(1);
        try (var host = open(0.125 * 1_000_000)) {
            var request = reader.submit(() -> {
                entered.countDown();
                long deadline = System.nanoTime() + SECONDS.toNanos(10);
                while (host.status().committedSteps() == 0) {
                    assertTrue(System.nanoTime() < deadline, "The run must commit its first step.");
                    Thread.onSpinWait();
                }
                assertEquals(SimulationSession.State.RUNNING, host.status().state());
                host.pause();
                paused.countDown();
            });
            assertTrue(entered.await(10, SECONDS));
            var run = host.run();
            assertTrue(paused.await(10, SECONDS));
            request.get(10, SECONDS);
            var status = run.get(10, SECONDS);
            assertEquals(SimulationSession.State.PAUSED, status.state());
            assertTrue(status.committedSteps() > 0);
            assertTrue(status.committedSteps() < 1_000_001);
            assertEquals((status.committedSteps() - 1) * 0.125, status.time());
            assertEquals(status.time() + 0.125, status.nextTime());
            var samples = new ArrayList<SimulationSession.Sample>();
            assertEquals(status.committedSteps(), host.drainSamples(samples::add));
            assertEquals(status.time(), samples.getLast().time());
            assertEquals(7, samples.getLast().values()[0]);
        } finally {
            reader.shutdownNow();
            assertTrue(reader.awaitTermination(10, SECONDS));
        }
    }

    @Test
    void failureCompletesFutureExceptionally() throws Exception {
        var model = new CircuitModel();
        model.setTimeStep(Math.scalb(1.0, -55));
        model.setSimulationDuration(Math.scalb(1.0, -50));
        model.setPreSimulationTime(Math.nextDown(1.0));
        model.setPreSimulationTimeStep(0.5);
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock"}));
        try (var host = SessionHost.open(prepare(model), new ExecutionOptions(false, 1))) {
            var run = host.run();
            var failure = assertThrows(ExecutionException.class, () -> run.get(10, SECONDS));
            assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            assertEquals(SimulationSession.State.FAILED, host.status().state());
            assertEquals(2, host.status().committedSteps());
        }
    }

    @Test
    void closeIsIdempotent() {
        var host = open(0.25);
        host.close();
        host.close();
        assertEquals(SimulationSession.State.CLOSED, host.status().state());
        assertThrows(RejectedExecutionException.class, host::run);
    }

    @Test
    void stepsAndExtensionRetainHistory() throws Exception {
        try (var host = open(0.125)) {
            assertEquals(0, host.step().get(10, SECONDS).time());
            assertEquals(0.125, host.step().get(10, SECONDS).time());
            assertEquals(SimulationSession.State.COMPLETED, host.status().state());
            var extended = host.extend(0.5, 0.25).get(10, SECONDS);
            assertEquals(SimulationSession.State.PAUSED, extended.state());
            assertEquals(0.25, extended.stepWidth());
            assertEquals(0.5, extended.endTime());
            assertEquals(2, extended.committedSteps());
            assertEquals(SimulationSession.State.COMPLETED, host.run().get(10, SECONDS).state());
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.5}, host.observe(0, 10).get(10, SECONDS).times());
            assertArrayEquals(new float[]{7, 7, 7, 7}, host.observe(0, 10).get(10, SECONDS).values()[0]);
            assertEquals(4, host.drainSamples(sample -> assertEquals(7, sample.values()[0])));
        }
    }

    @Test
    void samplesIncludeStepsOutsideTheLoggingInterval() throws Exception {
        try (var host = SessionHost.open(prepare(model(0.5)), new ExecutionOptions(true, 2))) {
            assertNotNull(host.step().get(10, SECONDS));
            assertNotNull(host.step().get(10, SECONDS));
            assertEquals(SimulationSession.State.COMPLETED, host.run().get(10, SECONDS).state());
            var samples = new ArrayList<SimulationSession.Sample>();
            assertEquals(5, host.drainSamples(samples::add));
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.375, 0.5},
                    samples.stream().mapToDouble(SimulationSession.Sample::time).toArray());
            assertArrayEquals(new double[]{0, 0.25, 0.5}, host.observe(0, 10).get(10, SECONDS).times());
        }
    }

    private static SessionHost open(double duration) {
        return SessionHost.open(prepare(model(duration)), ExecutionOptions.defaults());
    }

    private static CircuitModel model(double duration) {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(duration);
        model.addControlComponent(component(3, "constant", new double[]{7}, new String[0], "signal"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"signal"}));
        return model;
    }

    private static PreparedCircuit prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults())).execution();
    }

    private static void addSamples(SimulationSession.Observation observation, List<SimulationSession.Sample> samples) {
        double[] times = observation.times();
        float[][] rows = observation.values();
        for (int index = 0; index < times.length; index++) {
            float[] values = new float[rows.length];
            for (int row = 0; row < rows.length; row++) values[row] = rows[row][index];
            samples.add(new SimulationSession.Sample(times[index], values));
        }
    }

    private static void assertSamplesEqual(List<SimulationSession.Sample> expected, List<SimulationSession.Sample> actual) {
        assertArrayEquals(expected.stream().mapToDouble(SimulationSession.Sample::time).toArray(),
                actual.stream().mapToDouble(SimulationSession.Sample::time).toArray());
        for (int index = 0; index < expected.size(); index++) {
            assertArrayEquals(expected.get(index).values(), actual.get(index).values());
        }
    }
}

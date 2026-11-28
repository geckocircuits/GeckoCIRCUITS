package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class SimulationSessionConcurrencyTest {
    @Test
    void completionStatusCanPrecedeFinalSampleDelivery() throws Exception {
        var finalSampleEntered = new CountDownLatch(1);
        var releaseFinalSample = new CountDownLatch(1);
        var delivered = new java.util.concurrent.ConcurrentLinkedQueue<SimulationSession.Sample>();
        var executor = Executors.newSingleThreadExecutor();
        try (var session = open()) {
            try {
                var run = executor.submit(() -> session.run(() -> false, sample -> {
                    if (sample.time() == 0.25) {
                        finalSampleEntered.countDown();
                        await(releaseFinalSample);
                    }
                    delivered.add(sample);
                }));
                assertTrue(finalSampleEntered.await(5, TimeUnit.SECONDS));
                assertEquals(SimulationSession.State.COMPLETED, session.status().state());
                assertEquals(3, session.status().committedSteps());
                assertEquals(2, delivered.size());
                assertFalse(run.isDone());
                releaseFinalSample.countDown();
                assertEquals(SimulationSession.State.COMPLETED, run.get(5, TimeUnit.SECONDS).state());
                assertArrayEquals(new double[]{0, 0.125, 0.25},
                        delivered.stream().mapToDouble(SimulationSession.Sample::time).toArray());
            } finally {
                releaseFinalSample.countDown();
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void anAdmittedRunAcknowledgesPauseBeforeItsFirstStepAndCanResume() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try (var session = open()) {
            try {
                var run = executor.submit(() -> session.run(() -> {
                    entered.countDown();
                    await(release);
                    return false;
                }));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(SimulationSession.State.RUNNING, session.status().state());
                assertEquals(0, session.status().committedSteps());
                executor.submit(session::pause).get(2, TimeUnit.SECONDS);
                assertEquals(SimulationSession.State.RUNNING, session.status().state());
                release.countDown();
                var paused = run.get(5, TimeUnit.SECONDS);
                assertEquals(SimulationSession.State.PAUSED, paused.state());
                assertEquals(0, paused.committedSteps());
                assertEquals(SimulationSession.State.COMPLETED, session.run().state());
                assertArrayEquals(new double[]{0, 0.125, 0.25}, session.observe(0, 3).times());
            } finally { release.countDown(); }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void aRunRemainsRunningDuringDeliveryAndPausesAtTheCommittedBoundary() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try (var session = open()) {
            try {
                var run = executor.submit(() -> session.run(() -> false, sample -> {
                    assertThrows(IllegalStateException.class, session::step);
                    assertThrows(IllegalStateException.class, session::run);
                    entered.countDown();
                    await(release);
                }));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(SimulationSession.State.RUNNING, session.status().state());
                assertEquals(1, session.status().committedSteps());
                executor.submit(session::pause).get(2, TimeUnit.SECONDS);
                release.countDown();
                var paused = run.get(5, TimeUnit.SECONDS);
                assertEquals(SimulationSession.State.PAUSED, paused.state());
                assertEquals(1, paused.committedSteps());
                assertEquals(SimulationSession.State.COMPLETED, session.run().state());
                assertArrayEquals(new float[]{7, 7, 7}, session.observe(0, 3).values()[0]);
                assertArrayEquals(new double[]{0, 0.125, 0.25}, session.observe(0, 3).times());
            } finally { release.countDown(); }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static SimulationSession open() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addControlComponent(component(3, "constant", new double[]{7}, new String[0], "signal"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"signal"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        return SimulationSession.open(ready.execution(), ExecutionOptions.defaults());
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }
}

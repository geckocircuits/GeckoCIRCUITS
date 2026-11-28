package gecko.core.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class PauseCancellationTest {
    private SimulationConfig config() {
        return SimulationConfig.builder().stepWidth(1e-6).simulationDuration(0.005)
                .enableDataLogging(false).build();
    }

    private void pauseAtFirstProgress(HeadlessSimulationEngine engine, CountDownLatch paused) {
        engine.setProgressListener((time, end, step) -> {
            if (step == 1000) {
                assertTrue(engine.pause());
                paused.countDown();
            }
        });
    }

    @Test
    void pausedEngineDoesNotAdvanceAndResumesFromTheSameStep() throws Exception {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        CountDownLatch requested = new CountDownLatch(1);
        CountDownLatch observed = new CountDownLatch(2);
        AtomicInteger observedStep = new AtomicInteger();
        pauseAtFirstProgress(engine, requested);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<SimulationResult> run = executor.submit(() -> engine.runSimulation(config(), () -> {
                if (engine.isPaused()) {
                    observedStep.set(engine.getCurrentStep());
                    observed.countDown();
                }
                return false;
            }));
            try {
                assertTrue(requested.await(2, TimeUnit.SECONDS));
                assertTrue(observed.await(2, TimeUnit.SECONDS));
                assertEquals(1000, observedStep.get());
                assertFalse(run.isDone());
                assertNotNull(engine.getDetailedProgress());
                assertTrue(engine.resume());
                assertEquals(SimulationResult.Status.SUCCESS, run.get(2, TimeUnit.SECONDS).getStatus());
            } finally {
                engine.cancel();
            }
        }
    }

    @Test
    void cancellationWakesAPausedEngineAndAllowsReuse() throws Exception {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        CountDownLatch paused = new CountDownLatch(1);
        pauseAtFirstProgress(engine, paused);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<SimulationResult> run = executor.submit(() -> engine.runSimulation(config()));
            assertTrue(paused.await(2, TimeUnit.SECONDS));
            engine.cancel();
            SimulationResult result = run.get(2, TimeUnit.SECONDS);
            assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
            assertEquals(1000, result.getTotalTimeSteps());
            assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
            engine.setProgressListener(null);
            assertTrue(engine.runSimulation(config()).isSuccess());
        }
    }

    @Test
    void durableCancellationIsObservedWhilePaused() throws Exception {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        CountDownLatch paused = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        pauseAtFirstProgress(engine, paused);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<SimulationResult> run = executor.submit(() -> engine.runSimulation(config(), cancelled::get));
            assertTrue(paused.await(2, TimeUnit.SECONDS));
            cancelled.set(true);
            SimulationResult result = run.get(2, TimeUnit.SECONDS);
            assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
            assertEquals(1000, result.getTotalTimeSteps());
        }
    }

    @Test
    void interruptionWhilePausedCancelsWithoutLosingTheInterrupt() throws Exception {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        CountDownLatch paused = new CountDownLatch(1);
        AtomicReference<Thread> runner = new AtomicReference<>();
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        pauseAtFirstProgress(engine, paused);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<SimulationResult> run = executor.submit(() -> {
                runner.set(Thread.currentThread());
                SimulationResult result = engine.runSimulation(config());
                interruptPreserved.set(Thread.currentThread().isInterrupted());
                return result;
            });
            assertTrue(paused.await(2, TimeUnit.SECONDS));
            runner.get().interrupt();
            assertEquals(SimulationResult.Status.CANCELLED, run.get(2, TimeUnit.SECONDS).getStatus());
            assertTrue(interruptPreserved.get());
            assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
        }
    }

    @Test
    void cancellingFromAPausedCallbackCannotBeLost() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.setProgressListener((time, end, step) -> {
            assertTrue(engine.pause());
            engine.cancel();
            assertFalse(engine.resume());
        });
        SimulationResult result = engine.runSimulation(config());
        assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
        assertEquals(1000, result.getTotalTimeSteps());
    }

    @Test
    void pauseAndResumeFromTheSameCallbackDoesNotLoseTheWakeup() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.setProgressListener((time, end, step) -> {
            assertTrue(engine.pause());
            assertTrue(engine.resume());
        });
        assertTrue(engine.runSimulation(config()).isSuccess());
    }
}

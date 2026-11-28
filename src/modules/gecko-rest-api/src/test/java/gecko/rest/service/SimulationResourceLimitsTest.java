package gecko.rest.service;

import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparedCircuit;
import gecko.rest.support.PreparationCircuitFixtures;
import gecko.core.simulation.SimulationResult;
import gecko.rest.config.SimulationJobLimits;
import gecko.rest.model.BatchSimulationRequest;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Timeout;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class SimulationResourceLimitsTest {
    @TempDir
    Path temporaryDirectory;

    private String circuitFile;

    @BeforeEach
    void createCircuit() throws IOException {
        circuitFile = Files.write(temporaryDirectory.resolve("ready.ipes"),
                PreparationCircuitFixtures.resistorCircuit(10)).toString();
    }

    private SimulationRequest request() { return new SimulationRequest(circuitFile, 0.001, 1e-6); }

    private BatchSimulationRequest batch(int count) {
        BatchSimulationRequest request = new BatchSimulationRequest();
        request.setCircuitFile(circuitFile);
        request.setParameterSets(Collections.nCopies(count, Map.of("R.1.resistance", 10.0)));
        return request;
    }

    private SimulationService service(ExecutorService simulations, SimulationJobLimits limits) {
        return new SimulationService(simulations, HeadlessSimulationEngine::new, SseEmitter::new, null,
                limits, Runnable::run);
    }

    private void assertFull(Runnable request) {
        assertEquals(429, assertThrows(ResponseStatusException.class, request::run).getStatusCode().value());
    }

    @Test
    void retainedResultsArePreservedWhenFurtherAdmissionIsRejected() {
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setRetained(1);
        SimulationService service = service(new ManualExecutor(), limits);
        try {
            String id = service.submitSimulation(request()).getSimulationId();
            service.addResults(id, "saved", new double[]{42});
            service.updateStatus(id, SimulationResponse.SimulationStatus.COMPLETED);
            assertFull(() -> service.submitSimulation(request()));
            assertArrayEquals(new double[]{42}, service.getSignalData(id, "saved"));
            assertEquals(1, service.getAllSimulations().size());
        } finally { service.shutdown(); }
    }

    @Test
    void aBatchThatExceedsCapacityCannotLeavePartialChildJobs() {
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setRetained(2);
        SimulationService service = service(new ManualExecutor(), limits);
        try {
            String existing = service.submitSimulation(request()).getSimulationId();
            assertFull(() -> service.submitBatch(batch(2)));
            assertEquals(Set.of(existing), service.getAllSimulations().keySet());
        } finally { service.shutdown(); }
    }

    @Test
    void retainedBatchCountIsBoundedIndependentlyOfChildCapacity() {
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setBatches(1);
        SimulationService service = service(new ManualExecutor(), limits);
        try {
            String id = service.submitBatch(batch(1)).getBatchId();
            assertFull(() -> service.submitBatch(batch(1)));
            assertNotNull(service.getBatch(id));
            assertEquals(1, service.getAllSimulations().size());
        } finally { service.shutdown(); }
    }

    @Test
    void progressSubscriberLimitIsReleasedOnCompletion() {
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setStreamsPerJob(1);
        SimulationService service = new SimulationService(new ManualExecutor(), HeadlessSimulationEngine::new,
                timeout -> new CompletingEmitter(), null, limits, Runnable::run);
        try {
            String id = service.submitSimulation(request()).getSimulationId();
            SseEmitter first = service.registerProgressStream(id, 1000);
            assertFull(() -> service.registerProgressStream(id, 1000));
            first.complete();
            assertNotNull(service.registerProgressStream(id, 1000));
        } finally { service.shutdown(); }
    }

    @Test
    void aFullExecutionQueueReturns429WithoutRunningMoreEngines() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger engines = new AtomicInteger();
        SimulationService service = new SimulationService(executor, () -> {
            engines.incrementAndGet();
            return new HeadlessSimulationEngine() {
                @Override
                public SimulationResult runSimulation(PreparedCircuit circuit, ExecutionOptions options, BooleanSupplier cancelled) {
                    started.countDown();
                    try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return SimulationResult.failed("stopped");
                }
            };
        }, SseEmitter::new, null, new SimulationJobLimits(), Runnable::run);
        try {
            service.submitSimulation(request());
            assertTrue(started.await(2, TimeUnit.SECONDS));
            service.submitSimulation(request());
            assertFull(() -> service.submitSimulation(request()));
            assertFull(() -> service.submitSimulation(request()));
            assertEquals(1, executor.getQueue().size());
            assertEquals(1, engines.get());
            assertEquals(2, service.getAllSimulations().size(), "rejected requests must not consume retained capacity");
        } finally {
            release.countDown();
            service.shutdown();
        }
    }

    @Test
    void rejectedTerminalDeliveryClosesSubscribersInsteadOfLeavingThemWaiting() {
        AtomicInteger closed = new AtomicInteger();
        SimulationService service = new SimulationService(new ManualExecutor(), HeadlessSimulationEngine::new,
                timeout -> new SseEmitter(timeout) {
                    @Override public void completeWithError(Throwable error) { closed.incrementAndGet(); }
                }, null, new SimulationJobLimits(), task -> { throw new RejectedExecutionException(); });
        try {
            String id = service.submitSimulation(request()).getSimulationId();
            service.registerProgressStream(id, 1000);
            service.cancelSimulation(id);
            service.cancelSimulation(id);
            assertEquals(1, closed.get());
            assertEquals(SimulationResponse.SimulationStatus.FAILED, service.getSimulation(id).getStatus());
        } finally { service.shutdown(); }
    }

    @Test
    void blockedProgressCannotBlockStatusCancellationOrSimulationAdmission() throws Exception {
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch terminal = new CountDownLatch(1);
        AtomicInteger sent = new AtomicInteger();
        ExecutorService delivery = Executors.newSingleThreadExecutor();
        SimulationService service = new SimulationService(new ManualExecutor(), HeadlessSimulationEngine::new,
                timeout -> new SseEmitter(timeout) {
                    @Override
                    public void send(SseEventBuilder event) throws IOException {
                        if (sent.incrementAndGet() == 1) {
                            sending.countDown();
                            try { release.await(); } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IOException(e);
                            }
                        }
                    }
                    @Override public void complete() { terminal.countDown(); }
                }, null, new SimulationJobLimits(), delivery);
        try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
            String id = service.submitSimulation(request()).getSimulationId();
            service.registerProgressStream(id, 1000);
            service.broadcastProgress(id, 0.1, 0.0001, 0.001);
            assertTrue(sending.await(2, TimeUnit.SECONDS));
            for (int i = 0; i < 10000; i++) { service.broadcastProgress(id, 0.5, 0.0005, 0.001); }
            assertNotNull(caller.submit(() -> service.getSimulation(id)).get(1, TimeUnit.SECONDS));
            assertEquals(SimulationResponse.SimulationStatus.FAILED,
                    caller.submit(() -> service.cancelSimulation(id)).get(1, TimeUnit.SECONDS).getStatus());
            assertNotNull(caller.submit(() -> service.submitSimulation(request())).get(1, TimeUnit.SECONDS));
            release.countDown();
            assertTrue(terminal.await(2, TimeUnit.SECONDS));
            assertEquals(3, sent.get(), "one in flight, one coalesced update, one terminal event");
        } finally {
            release.countDown();
            service.shutdown();
        }
    }

    private static class CompletingEmitter extends SseEmitter {
        private Runnable completion;
        @Override public void onCompletion(Runnable callback) { completion = callback; }
        @Override public void complete() { if (completion != null) { completion.run(); } }
        @Override public void send(SseEventBuilder event) { }
    }

    private static class ManualExecutor extends AbstractExecutorService {
        private boolean shutdown;
        private final List<Runnable> tasks = new ArrayList<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() { shutdown = true; return List.copyOf(tasks); }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return shutdown; }
    }
}

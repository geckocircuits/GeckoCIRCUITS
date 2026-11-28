package gecko.rest.service;

import gecko.core.allg.SolverType;
import gecko.core.preparation.CircuitSnapshot;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.rest.config.SimulationJobLimits;
import gecko.rest.model.BatchSimulationRequest;
import gecko.rest.model.BatchSimulationResponse;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.circuit.CircuitParameterUpdate;
import gecko.rest.model.SimulationResponse.SimulationStatus;
import gecko.rest.support.PreparationCircuitFixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class CircuitPreparationAdmissionContractTest {

    @Test
    void invalidFinalBatchRowPublishesNoWorkAndConsumesNoCapacity() throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorCircuit(10);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        CircuitFileService sources = new CircuitFileService();
        String sourceId = upload(sources, content);
        ManualExecutor executor = new ManualExecutor();
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setRetained(2);
        limits.setBatches(1);
        SimulationService service = service(executor, sources, limits);

        CircuitPreparationException rejected = assertThrows(CircuitPreparationException.class,
                () -> service.submitBatch(batch(sourceId, List.of(
                        Map.of("R.1.resistance", 20.0), Map.of("R.1.unknown", 30.0)))));

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, rejected.getStatusCode()),
                () -> assertFalse(rejected.report().diagnostics().isEmpty()),
                () -> assertTrue(service.getAllSimulations().isEmpty(), "Rejected Batch must publish no child jobs"),
                () -> assertEquals(0, executor.size(), "Rejected Batch must queue no workers"));

        BatchSimulationResponse accepted = assertDoesNotThrow(() -> service.submitBatch(batch(sourceId,
                List.of(Map.of("R.1.resistance", 10.0), Map.of("R.1.resistance", 20.0)))));
        assertNotNull(service.getBatch(accepted.getBatchId()));
        assertEquals(2, service.getAllSimulations().size());
        executor.runAll();
        assertEquals(2, service.getBatchStatus(accepted.getBatchId()).getCompleted());
        for (String id : accepted.getSimulationIds()) {
            assertEquals(SimulationStatus.COMPLETED, service.getSimulation(id).getStatus());
        }
    }

    @Test
    void acceptedStoredJobKeepsItsCircuitAfterStoredSettingsChangeAndRemoval() throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorCircuit(10);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        CircuitFileService sources = new CircuitFileService();
        String sourceId = upload(sources, content);
        ManualExecutor executor = new ManualExecutor();
        SimulationService service = service(executor, sources, new SimulationJobLimits());
        SimulationRequest request = new SimulationRequest();
        request.setCircuitId(sourceId);
        String jobId = service.submitSimulation(request).getSimulationId();

        sources.updateCircuitParameters(sourceId, new CircuitParameterUpdate(0.1, 0.1, "trapezoidal"));
        assertEquals(0.1, sources.resolveSource(sourceId, null).snapshot().toModel().getTimeStep());
        assertTrue(sources.deleteCircuit(sourceId));
        assertEquals(SimulationStatus.PENDING, service.getSimulation(jobId).getStatus());

        executor.runNext();
        assertTrace(service, jobId, "current", 0.6);
        assertArrayEquals(new double[]{0.0, 0.125, 0.25}, service.getSignalData(jobId, "time"), 1e-9);
    }

    @Test
    void acceptedPathJobKeepsCapturedBytesWhenTheFileChanges(@TempDir Path directory) throws Exception {
        byte[] original = PreparationCircuitFixtures.resistorCircuit(10);
        byte[] replacement = PreparationCircuitFixtures.resistorCircuit(20);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(original));
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(replacement));
        Path file = directory.resolve("queued-source.ipes");
        Files.write(file, original);
        ManualExecutor executor = new ManualExecutor();
        SimulationService service = service(executor, new CircuitFileService(), new SimulationJobLimits());
        String jobId = service.submitSimulation(new SimulationRequest(file.toString(), 0.25, 0.125))
                .getSimulationId();

        Files.write(file, replacement);
        executor.runNext();

        assertTrace(service, jobId, "current", 0.6);
        assertArrayEquals(new double[]{0.0, 0.125, 0.25}, service.getSignalData(jobId, "time"), 1e-9);
    }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void capacityIsCheckedAgainAfterSourcePreparationWithoutPartialPublication(boolean batchAdmission)
            throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorCircuit(10);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        PausingSource sources = new PausingSource();
        String sourceId = upload(sources, content);
        ManualExecutor executor = new ManualExecutor();
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setRetained(batchAdmission ? 2 : 1);
        SimulationService service = service(executor, sources, limits);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<?> paused = callers.submit(() -> {
                if (batchAdmission) return service.submitBatch(batch(sourceId,
                        List.of(Map.of("R.1.resistance", 10.0), Map.of("R.1.resistance", 20.0))));
                return service.submitSimulation(storedRequest(sourceId));
            });
            assertTrue(sources.captured.await(5, TimeUnit.SECONDS), "Source capture did not reach the barrier");
            String acceptedId = callers.submit(() -> service.submitSimulation(storedRequest(sourceId)))
                    .get(5, TimeUnit.SECONDS).getSimulationId();
            assertEquals(1, service.getAllSimulations().size());
            assertEquals(1, executor.size());

            sources.release.countDown();
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> paused.get(5, TimeUnit.SECONDS));
            ResponseStatusException rejected = assertInstanceOf(ResponseStatusException.class, failure.getCause());
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, rejected.getStatusCode());
            assertEquals(java.util.Set.of(acceptedId), service.getAllSimulations().keySet());
            assertEquals(1, executor.size());
            executor.runAll();
            assertTrace(service, acceptedId, "current", 0.6);
        } finally {
            sources.release.countDown();
            callers.shutdownNow();
            assertTrue(callers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void captureDuringStoredUpdateObservesACompleteEntryAndKeepsItsOwnedSettings() throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorCircuit(10);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        CircuitFileService sources = new CircuitFileService();
        String sourceId = upload(sources, content);
        CircuitSnapshot before = sources.resolveSource(sourceId, null).snapshot();
        CountDownLatch readingUpdate = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);
        CircuitParameterUpdate update = new CircuitParameterUpdate(0.5, 0.25, "trapezoidal") {
            @Override
            public Double getTimeStep() {
                readingUpdate.countDown();
                await(releaseUpdate);
                return super.getTimeStep();
            }
        };
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<?> updating = callers.submit(() -> sources.updateCircuitParameters(sourceId, update));
            assertTrue(readingUpdate.await(5, TimeUnit.SECONDS), "Update did not reach its input barrier");
            CircuitSnapshot during = callers.submit(() -> sources.resolveSource(sourceId, null).snapshot())
                    .get(5, TimeUnit.SECONDS);
            Settings captured = settings(during);
            assertTrue(captured.equals(new Settings(0.25, 0.125, SolverType.SOLVER_BE))
                    || captured.equals(new Settings(0.5, 0.25, SolverType.SOLVER_TRZ)),
                    "Capture must contain one complete settings entry: " + captured);

            releaseUpdate.countDown();
            updating.get(5, TimeUnit.SECONDS);

            assertEquals(new Settings(0.5, 0.25, SolverType.SOLVER_TRZ),
                    settings(sources.resolveSource(sourceId, null).snapshot()));
            assertEquals(new Settings(0.25, 0.125, SolverType.SOLVER_BE), settings(before));
            assertEquals(captured, settings(during));
        } finally {
            releaseUpdate.countDown();
            callers.shutdownNow();
            assertTrue(callers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void batchChildrenOwnIndependentResistorAndGainOverridesFromOneBaseCapture() throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorGainCircuit(10, 1);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        AtomicInteger captures = new AtomicInteger();
        CircuitFileService sources = new CircuitFileService() {
            @Override
            public ResolvedSource resolveSource(String circuitId, String circuitFile) {
                captures.incrementAndGet();
                return super.resolveSource(circuitId, circuitFile);
            }
        };
        String sourceId = upload(sources, content);
        ManualExecutor executor = new ManualExecutor();
        SimulationService service = service(executor, sources, new SimulationJobLimits());
        Map<String, Double> first = new HashMap<>(Map.of("R.1.resistance", 10.0, "gain.gain", 2.0));
        Map<String, Double> second = new HashMap<>(Map.of("R.1.resistance", 20.0, "gain.gain", 3.0));
        List<Map<String, Double>> rows = new ArrayList<>(List.of(first, second));

        BatchSimulationResponse accepted = service.submitBatch(batch(sourceId, rows));
        assertEquals(1, captures.get(), "One Batch must resolve one base source");
        first.put("R.1.resistance", 100.0);
        first.put("gain.gain", 100.0);
        second.clear();
        rows.clear();
        executor.runAll();

        assertTrace(service, accepted.getSimulationIds().get(0), "current", 0.6);
        assertTrace(service, accepted.getSimulationIds().get(0), "scaled", 1.2);
        assertTrace(service, accepted.getSimulationIds().get(1), "current", 0.3);
        assertTrace(service, accepted.getSimulationIds().get(1), "scaled", 0.9);
        String unchangedBase = service.submitSimulation(storedRequest(sourceId)).getSimulationId();
        executor.runNext();
        assertTrace(service, unchangedBase, "current", 0.6);
        assertTrace(service, unchangedBase, "scaled", 0.6);
    }
    @ParameterizedTest
    @ValueSource(strings = {"null row", "too many rows", "no variations",
            "linear below minimum", "linear above maximum", "linear blank path", "linear null path",
            "linear nonfinite start", "linear nonfinite end", "log zero start", "log negative end",
            "negative concurrency", "excessive concurrency", "invalid time step", "invalid duration",
            "unknown solver", "null override", "nonfinite override"})
    void malformedBatchInputPublishesNothingAndLeavesCapacityAvailable(String invalidCase) throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorCircuit(10);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        CircuitFileService sources = new CircuitFileService();
        String sourceId = upload(sources, content);
        ManualExecutor executor = new ManualExecutor();
        SimulationJobLimits limits = new SimulationJobLimits();
        limits.setRetained(2);
        limits.setBatches(1);
        SimulationService service = service(executor, sources, limits);
        BatchSimulationRequest request = batch(sourceId, List.of(Map.of("R.1.resistance", 10.0)));
        String expectedCode = "INVALID_BATCH";
        switch (invalidCase) {
            case "null row" -> request.setParameterSets(java.util.Arrays.asList(Map.of(), null));
            case "too many rows" -> request.setParameterSets(java.util.Collections.nCopies(101, Map.of()));
            case "no variations" -> request.setParameterSets(null);
            case "linear below minimum", "linear above maximum", "linear blank path", "linear null path",
                    "linear nonfinite start", "linear nonfinite end" -> {
                request.setParameterSets(null);
                BatchSimulationRequest.LinearSweep sweep = new BatchSimulationRequest.LinearSweep();
                sweep.setParameterPath(invalidCase.equals("linear blank path") ? "  "
                        : invalidCase.equals("linear null path") ? null : "R.1.resistance");
                sweep.setPoints(invalidCase.equals("linear below minimum") ? 1
                        : invalidCase.equals("linear above maximum") ? 1001 : 2);
                sweep.setStartValue(invalidCase.equals("linear nonfinite start") ? Double.NaN : 10.0);
                sweep.setEndValue(invalidCase.equals("linear nonfinite end") ? Double.POSITIVE_INFINITY : 20.0);
                request.setLinearSweep(sweep);
            }
            case "log zero start", "log negative end" -> {
                request.setParameterSets(null);
                BatchSimulationRequest.LogSweep sweep = new BatchSimulationRequest.LogSweep();
                sweep.setParameterPath("R.1.resistance");
                sweep.setPoints(2);
                sweep.setStartValue(invalidCase.equals("log zero start") ? 0.0 : 10.0);
                sweep.setEndValue(invalidCase.equals("log negative end") ? -1.0 : 20.0);
                request.setLogSweep(sweep);
            }
            case "negative concurrency" -> request.setMaxConcurrent(-1);
            case "excessive concurrency" -> request.setMaxConcurrent(101);
            case "invalid time step", "invalid duration", "unknown solver" -> {
                BatchSimulationRequest.SimulationSettings settings = new BatchSimulationRequest.SimulationSettings();
                if (invalidCase.equals("invalid time step")) settings.setTimeStep(0.0);
                if (invalidCase.equals("invalid duration")) settings.setSimulationTime(Double.NaN);
                if (invalidCase.equals("unknown solver")) settings.setSolverType("unknown");
                request.setSimulationSettings(settings);
                expectedCode = "INVALID_SETTING";
            }
            case "null override", "nonfinite override" -> {
                Map<String, Double> row = new HashMap<>();
                row.put("R.1.resistance", invalidCase.equals("null override") ? null : Double.NaN);
                request.setParameterSets(List.of(Map.of("R.1.resistance", 10.0), row));
                expectedCode = "INVALID_PARAMETER";
            }
            default -> fail("Unknown test case: " + invalidCase);
        }

        CircuitPreparationException rejected = assertThrows(CircuitPreparationException.class,
                () -> service.submitBatch(request));
        assertEquals(HttpStatus.BAD_REQUEST, rejected.getStatusCode());
        String code = expectedCode;
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item -> item.code().equals(code)),
                rejected.report().toString());
        assertTrue(service.getAllSimulations().isEmpty());
        assertEquals(0, executor.size());
        BatchSimulationResponse accepted = service.submitBatch(batch(sourceId,
                List.of(Map.of("R.1.resistance", 10.0), Map.of("R.1.resistance", 20.0))));
        executor.runAll();
        assertEquals(2, service.getBatchStatus(accepted.getBatchId()).getCompleted());
        assertTrace(service, accepted.getSimulationIds().get(0), "current", 0.6);
        assertTrace(service, accepted.getSimulationIds().get(1), "current", 0.3);
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptedBatchKeepsItsBaseAfterStoredRemovalOrPathReplacement(boolean stored, @TempDir Path directory)
            throws Exception {
        byte[] original = PreparationCircuitFixtures.resistorCircuit(10, 6);
        byte[] replacement = PreparationCircuitFixtures.resistorCircuit(10, 12);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(original));
        PreparationResult.Ready changed = assertInstanceOf(PreparationResult.Ready.class,
                PreparationCircuitFixtures.prepare(replacement));
        var changedSource = changed.circuit().toModel().getCircuitComponents().stream()
                .filter(component -> component.getName().equals("source")).findFirst().orElseThrow();
        assertEquals(12.0, ((Number) changedSource.getParameters().get("dcValue")).doubleValue());
        CircuitFileService sources = new CircuitFileService();
        ManualExecutor executor = new ManualExecutor();
        SimulationService service = service(executor, sources, new SimulationJobLimits());
        Path file = directory.resolve("batch-source.ipes");
        String sourceId;
        if (stored) {
            sourceId = upload(sources, original);
        } else {
            Files.write(file, original);
            sourceId = file.toString();
        }
        BatchSimulationRequest request = batch(sourceId,
                List.of(Map.of("R.1.resistance", 10.0), Map.of("R.1.resistance", 20.0)));
        if (!stored) {
            BatchSimulationRequest.SimulationSettings settings = new BatchSimulationRequest.SimulationSettings();
            settings.setSimulationTime(0.25);
            settings.setTimeStep(0.125);
            request.setSimulationSettings(settings);
        }
        BatchSimulationResponse accepted = service.submitBatch(request);

        if (stored) {
            sources.updateCircuitParameters(sourceId, new CircuitParameterUpdate(0.1, 0.1, "trapezoidal"));
            assertTrue(sources.deleteCircuit(sourceId));
        } else {
            Files.write(file, replacement);
        }
        executor.runAll();

        assertEquals(2, service.getBatchStatus(accepted.getBatchId()).getCompleted());
        assertTrace(service, accepted.getSimulationIds().get(0), "current", 0.6);
        assertTrace(service, accepted.getSimulationIds().get(1), "current", 0.3);
        for (String child : accepted.getSimulationIds()) {
            assertArrayEquals(new double[]{0.0, 0.125, 0.25}, service.getSignalData(child, "time"), 1e-9);
        }
    }
    private static Settings settings(CircuitSnapshot snapshot) {
        var model = snapshot.toModel();
        return new Settings(model.getSimulationDuration(), model.getTimeStep(), model.getSolverType());
    }

    private record Settings(double duration, double timeStep, SolverType solver) { }
    private static SimulationRequest storedRequest(String sourceId) {
        SimulationRequest request = new SimulationRequest();
        request.setCircuitId(sourceId);
        return request;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Barrier timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class PausingSource extends CircuitFileService {
        private final AtomicBoolean firstCapture = new AtomicBoolean(true);
        private final CountDownLatch captured = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public ResolvedSource resolveSource(String circuitId, String circuitFile) {
            ResolvedSource source = super.resolveSource(circuitId, circuitFile);
            if (firstCapture.getAndSet(false)) {
                captured.countDown();
                await(release);
            }
            return source;
        }
    }
    private static void assertTrace(SimulationService service, String jobId, String signal, double expected) {
        assertEquals(SimulationStatus.COMPLETED, service.getSimulation(jobId).getStatus(),
                service.getSimulation(jobId).getErrorMessage());
        double[] values = service.getSignalData(jobId, signal);
        assertNotNull(values, "Missing " + signal);
        assertEquals(3, values.length);
        for (double value : values) assertEquals(expected, value, 1e-6);
    }
    private static String upload(CircuitFileService sources, byte[] content) {
        var loaded = sources.loadCircuit(Base64.getEncoder().encodeToString(content), "uploaded-only.ipes");
        assertEquals("loaded", loaded.status(), loaded.errorMessage());
        return loaded.circuitId();
    }

    private static SimulationService service(ManualExecutor executor, CircuitFileService sources,
            SimulationJobLimits limits) {
        return new SimulationService(executor, HeadlessSimulationEngine::new, SseEmitter::new,
                null, limits, Runnable::run, sources);
    }

    private static BatchSimulationRequest batch(String sourceId, List<Map<String, Double>> rows) {
        BatchSimulationRequest request = new BatchSimulationRequest();
        request.setCircuitFile(sourceId);
        request.setMaxConcurrent(1);
        request.setParameterSets(rows);
        return request;
    }

    private static final class ManualExecutor extends AbstractExecutorService {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;
        @Override
        public synchronized void execute(Runnable command) {
            if (shutdown) { throw new RejectedExecutionException(); }
            tasks.add(command);
        }
        private synchronized int size() { return tasks.size(); }
        private synchronized Runnable take() { return tasks.remove(); }
        private void runNext() { take().run(); }
        private void runAll() { while (size() > 0) { runNext(); } }
        @Override
        public synchronized void shutdown() { shutdown = true; }
        @Override
        public synchronized List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            return pending;
        }
        @Override
        public synchronized boolean isShutdown() { return shutdown; }
        @Override
        public synchronized boolean isTerminated() { return shutdown && tasks.isEmpty(); }
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
    }
}

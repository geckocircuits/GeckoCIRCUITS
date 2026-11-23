package gecko.rest.service;

import gecko.core.allg.SolverType;
import gecko.core.preparation.*;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationProgress;
import gecko.core.simulation.SimulationResult;
import gecko.core.signal.TimedSignal;
import gecko.rest.config.SimulationJobLimits;
import gecko.rest.model.BatchJobStatus;
import gecko.rest.model.BatchSimulationRequest;
import gecko.rest.model.BatchSimulationResponse;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import gecko.rest.model.SimulationResponse.SimulationStatus;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Owns simulation scheduling, state transitions, and progress subscriptions.
 * Each job commits state under its monitor and delivers ordered notifications separately.
 */
@Service
public class SimulationService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    static final String CANCELLED_BY_USER = "Cancelled by user";
    private static final int DEFAULT_BATCH_CONCURRENCY = 4;

    private final Map<String, SimulationJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, BatchJob> batches = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final Supplier<HeadlessSimulationEngine> engineFactory;
    private final LongFunction<SseEmitter> emitterFactory;
    private final SimulationJobLimits limits;
    private final Executor deliveryExecutor;
    private final Object admissionMonitor = new Object();
    private final CircuitFileService circuitFiles;
    private final CircuitPreparation preparation = new CircuitPreparation();

    @Autowired(required = false)
    private WebSocketProgressService webSocketProgressService;

    public SimulationService() {
        this(new SimulationJobLimits());
    }

    public SimulationService(SimulationJobLimits limits) { this(limits, new CircuitFileService()); }

    /**
     * Creates bounded execution and delivery pools with a shared circuit source registry.
     *
     * @param limits the execution, delivery, and retained-result capacity limits.
     * @param circuitFiles the source adapter used to capture input before job publication.
     */
    @Autowired
    public SimulationService(SimulationJobLimits limits, CircuitFileService circuitFiles) {
        this(new ThreadPoolExecutor(limits.getWorkers(), limits.getWorkers(), 0, TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(limits.getQueued())), HeadlessSimulationEngine::new,
                SseEmitter::new, null, limits,
                new ThreadPoolExecutor(limits.getDeliveryWorkers(), limits.getDeliveryWorkers(), 0, TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(limits.getRetained()),
                        Thread.ofPlatform().daemon().name("gecko-progress-", 0).factory()), circuitFiles);
    }

    SimulationService(ExecutorService executorService, Supplier<HeadlessSimulationEngine> engineFactory) {
        this(executorService, engineFactory, SseEmitter::new, null);
    }

    SimulationService(ExecutorService executorService, Supplier<HeadlessSimulationEngine> engineFactory,
                      LongFunction<SseEmitter> emitterFactory, WebSocketProgressService webSocketProgressService) {
        this(executorService, engineFactory, emitterFactory, webSocketProgressService,
                new SimulationJobLimits(), Runnable::run);
    }

    SimulationService(ExecutorService executorService, Supplier<HeadlessSimulationEngine> engineFactory,
                      LongFunction<SseEmitter> emitterFactory, WebSocketProgressService webSocketProgressService,
                      SimulationJobLimits limits, Executor deliveryExecutor) {
        this(executorService, engineFactory, emitterFactory, webSocketProgressService, limits, deliveryExecutor,
                new CircuitFileService());
    }

    SimulationService(ExecutorService executorService, Supplier<HeadlessSimulationEngine> engineFactory,
                      LongFunction<SseEmitter> emitterFactory, WebSocketProgressService webSocketProgressService,
                      SimulationJobLimits limits, Executor deliveryExecutor, CircuitFileService circuitFiles) {
        this.circuitFiles = circuitFiles;
        this.executorService = executorService;
        this.engineFactory = engineFactory;
        this.emitterFactory = emitterFactory;
        this.webSocketProgressService = webSocketProgressService;
        this.limits = limits;
        this.deliveryExecutor = deliveryExecutor;
    }

    /**
     * Captures and prepares one circuit before publishing or dispatching its Simulation job.
     * Stored sources supply omitted settings, and explicit request settings take precedence.
     *
     * @param request the source selection, optional settings, and numeric overrides.
     * @return a detached snapshot of the accepted job.
     * @throws CircuitPreparationException for invalid source selection, invalid overrides, or failed Headless readiness.
     * @throws ResponseStatusException when capacity prevents admission.
     */
    public SimulationResponse submitSimulation(SimulationRequest request) {
        PreparedCircuit circuit = prepare(request, circuitFiles.resolveSource(request.getCircuitId(), request.getCircuitFile()));
        synchronized (admissionMonitor) {
            requireCapacity(1, false);
            SimulationJob job = createJob(circuit, null);
            if (!dispatch(job) && !executorService.isShutdown()) {
                jobs.remove(job.response.getSimulationId(), job);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Simulation queue is full");
            }
            return snapshot(job);
        }
    }

    private void requireCapacity(int requestedJobs, boolean batch) {
        if (requestedJobs > limits.getRetained() - jobs.size()
                || (batch && batches.size() >= limits.getBatches())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Simulation capacity is full. Existing results are retained.");
        }
    }

    private PreparedCircuit prepare(SimulationRequest request, CircuitFileService.ResolvedSource source) {
        if (!source.stored() && (request.getTimeStep() == null || request.getSimulationTime() == null)) {
            throw CircuitPreparationException.failure(HttpStatus.BAD_REQUEST, "INVALID_SETTING", Diagnostic.Category.CONFIGURATION,
                    "Path requests require time step and simulation time", request.getTimeStep() == null ? "timeStep" : "simulationTime");
        }
        PreparationResult result = preparation.prepare(source.snapshot(), new PreparationRequest(
                new SolverOverrides(request.getSolverType() == null ? null : parseSolverType(request.getSolverType()),
                        request.getTimeStep(), request.getSimulationTime()),
                request.getParameters() == null ? Map.of() : request.getParameters()));
        if (result instanceof PreparationResult.Ready ready) return ready.execution();
        throw new CircuitPreparationException(result instanceof PreparationResult.Rejected ? HttpStatus.BAD_REQUEST
                : HttpStatus.UNPROCESSABLE_ENTITY, result.report());
    }

    private SimulationJob createJob(PreparedCircuit circuit, BatchJob batch) {
        var principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        SimulationJob job = new SimulationJob(circuit, batch, deliveryExecutor,
                principal == null || !principal.isAuthenticated() ? null : principal.getName());
        jobs.put(job.response.getSimulationId(), job);
        return job;
    }

    /** Checks the immutable owner captured before publication of a single or batch job. */
    public boolean isOwnedBy(String simulationId, String owner) {
        SimulationJob job = jobs.get(simulationId);
        return job != null && owner != null && owner.equals(job.owner);
    }

    private boolean dispatch(SimulationJob job) {
        try {
            executorService.execute(() -> runSimulation(job));
            return true;
        } catch (RejectedExecutionException e) {
            synchronized (job) {
                if (applyFailureResult(job.response, executorService.isShutdown()
                        ? "Simulation executor is shut down" : "Simulation queue is full")) {
                    publishTerminal(job);
                }
            }
            return false;
        }
    }

    private void runSimulation(SimulationJob job) {
        try {
            synchronized (job) {
                if (isTerminal(job.response)) {
                    return;
                }
            }
            HeadlessSimulationEngine engine = engineFactory.get();
            engine.setProgressListener((currentTime, endTime, currentStep) -> {
                double progress = endTime > 0 ? currentTime / endTime : 0;
                broadcastProgress(job.response.getSimulationId(), progress, currentTime, endTime);
            });
            synchronized (job) {
                if (isTerminal(job.response)) {
                    return;
                }
                job.engine = engine;
                markRunning(job.response);
            }
            // The durable signal also covers cancellation before the engine starts.
            SimulationResult result = engine.runSimulation(job.circuit, job.options, job::isCancelled);
            synchronized (job) {
                boolean committed = result.isSuccess()
                        ? applySuccessfulResult(job.response, result)
                        : applyFailureResult(job.response, result.getErrorMessage());
                if (committed) {
                    publishTerminal(job);
                }
            }
        } catch (Exception e) {
            synchronized (job) {
                if (applyFailureResult(job.response, "Simulation error: " + e.getMessage())) {
                    publishTerminal(job);
                    logger.error("Simulation {} failed", job.response.getSimulationId(), e);
                }
            }
        } finally {
            synchronized (job) {
                job.engine = null;
            }
            releaseBatchSlot(job);
        }
    }

    public SimulationResponse getSimulation(String simulationId) {
        SimulationJob job = jobs.get(simulationId);
        return job == null ? null : snapshot(job);
    }

    /**
     * Captures detached status metadata without reading retained waveform arrays.
     *
     * @param simulationId the retained job identifier.
     * @return metadata with empty results, or null when the job is absent.
     */
    public SimulationResponse getSimulationStatus(String simulationId) {
        SimulationJob job = jobs.get(simulationId);
        return job == null ? null : snapshot(job, false);
    }

    private SimulationResponse snapshot(SimulationJob job) {
        return snapshot(job, true);
    }

    private SimulationResponse snapshot(SimulationJob job, boolean includeResults) {
        synchronized (job) {
            refreshProgress(job);
            SimulationResponse source = job.response;
            SimulationResponse result = new SimulationResponse(source.getSimulationId());
            result.setStatus(source.getStatus());
            result.setStartTime(source.getStartTime());
            result.setEndTime(source.getEndTime());
            result.setErrorMessage(source.getErrorMessage());
            result.setProgressDetails(source.getProgressDetails());
            if (includeResults) result.setResults(source.getResults());
            return result;
        }
    }

    private void refreshProgress(SimulationJob job) {
        if (job.engine != null && job.response.getStatus() == SimulationStatus.RUNNING) {
            SimulationProgress detailed = job.engine.getDetailedProgress();
            if (detailed != null) {
                job.response.setProgressDetails(new SimulationResponse.ProgressDetails(
                        detailed.getPreCalcProgress(), detailed.getMainSimProgress(), detailed.getCurrentTime(),
                        detailed.getEndTime(), detailed.getCurrentStep(), detailed.getTotalSteps(),
                        detailed.getEstimatedRemainingMs()));
            }
        }
    }

    public Map<String, SimulationResponse> getAllSimulations() {
        Map<String, SimulationResponse> result = new HashMap<>();
        jobs.forEach((id, job) -> result.put(id, snapshot(job)));
        return result;
    }

    public SimulationResponse cancelSimulation(String simulationId) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) {
            return null;
        }
        synchronized (job) {
            if (markCancelled(job.response)) {
                if (job.engine != null) {
                    job.engine.cancel();
                }
                publishTerminal(job);
            }
            return snapshot(job);
        }
    }

    public double getSimulationProgress(String simulationId) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) {
            return -1;
        }
        synchronized (job) {
            if (job.response.getStatus() == SimulationStatus.COMPLETED) {
                return 100.0;
            }
            if (job.response.getStatus() == SimulationStatus.RUNNING && job.engine != null) {
                return job.engine.getProgress() * 100.0;
            }
            return 0.0;
        }
    }

    public double[] getSignalData(String simulationId, String signalName) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) {
            return null;
        }
        synchronized (job) {
            return job.response.getResult(signalName);
        }
    }

    /**
     * Captures one retained signal and its recorded clock under the same job monitor.
     *
     * @param simulationId the retained job identifier.
     * @param signalName the requested signal name.
     * @return independent timed samples, or null when the job or signal is absent.
     * @throws IllegalArgumentException if the retained signal has no valid matching clock.
     */
    public TimedSignal getTimedSignalData(String simulationId, String signalName) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) return null;
        synchronized (job) {
            double[] values = job.response.getResult(signalName);
            if (values == null || values.length == 0) return null;
            return new TimedSignal(job.response.getResult("time"), values);
        }
    }

    /** Updates a nonterminal job through the existing controller-facing interface. */
    public void updateStatus(String simulationId, SimulationStatus status) {
        SimulationJob job = jobs.get(simulationId);
        if (job != null) {
            synchronized (job) {
                if (!isTerminal(job.response)) {
                    job.response.setStatus(status);
                    if (isTerminal(job.response)) {
                        job.response.setEndTime(Instant.now());
                        publishTerminal(job);
                    }
                }
            }
        }
    }

    public void addResults(String simulationId, String signalName, double[] signalData) {
        SimulationJob job = jobs.get(simulationId);
        if (job != null) {
            synchronized (job) {
                job.response.addResult(signalName, signalData);
            }
        }
    }

    public Map<SimulationStatus, Long> getStatistics() {
        Map<SimulationStatus, Long> result = new EnumMap<>(SimulationStatus.class);
        for (SimulationStatus status : SimulationStatus.values()) {
            result.put(status, 0L);
        }
        for (SimulationJob job : jobs.values()) {
            synchronized (job) {
                result.merge(job.response.getStatus(), 1L, Long::sum);
            }
        }
        return result;
    }

    public boolean isRunning(String simulationId) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) {
            return false;
        }
        synchronized (job) {
            return job.engine != null && job.response.getStatus() == SimulationStatus.RUNNING;
        }
    }

    public SseEmitter registerProgressStream(String simulationId, long timeoutMs) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) {
            return null;
        }
        synchronized (job) {
            if (!isTerminal(job.response) && job.emitters.size() >= limits.getStreamsPerJob()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many progress streams for this job");
            }
            SseEmitter emitter = emitterFactory.apply(timeoutMs);
            if (isTerminal(job.response)) {
                String data = "{\"status\":\"" + job.response.getStatus()
                        + "\",\"simulationId\":\"" + simulationId + "\"}";
                try {
                    deliveryExecutor.execute(() -> {
                        try {
                            emitter.send(SseEmitter.event().name("status").data(data));
                            emitter.complete();
                        } catch (Exception ignored) {
                            // The subscriber disconnected before receiving the terminal state.
                        }
                    });
                } catch (RejectedExecutionException full) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Progress delivery is busy");
                }
            } else {
                job.emitters.add(emitter);
                emitter.onCompletion(() -> removeEmitter(job, emitter));
                emitter.onTimeout(() -> removeEmitter(job, emitter));
                emitter.onError(error -> removeEmitter(job, emitter));
            }
            return emitter;
        }
    }

    private void removeEmitter(SimulationJob job, SseEmitter emitter) {
        synchronized (job) {
            job.emitters.remove(emitter);
        }
    }

    /**
     * Sends a progress ratio to SSE subscribers and its percentage to STOMP subscribers.
     *
     * @param simulationId the retained job identifier.
     * @param progressRatio the completion ratio from zero to one.
     * @param currentTime the current simulation time in seconds.
     * @param endTime the requested end time in seconds.
     */
    public void broadcastProgress(String simulationId, double progressRatio, double currentTime, double endTime) {
        SimulationJob job = jobs.get(simulationId);
        if (job == null) {
            return;
        }
        synchronized (job) {
            if (isTerminal(job.response)) {
                return;
            }
            refreshProgress(job);
            String data = String.format(Locale.ROOT,
                    "{\"progress\":%.3f,\"currentTime\":%.6f,\"endTime\":%.6f,\"simulationId\":\"%s\"}",
                    progressRatio, currentTime, endTime, simulationId);
            List<SseEmitter> emitters = new ArrayList<>(job.emitters);
            SimulationResponse.ProgressDetails details = job.response.getProgressDetails();
            job.mailbox.progress(() -> {
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(data));
                    } catch (Exception e) {
                        removeEmitter(job, emitter);
                    }
                }
                if (webSocketProgressService != null) {
                    try {
                        webSocketProgressService.broadcastProgress(simulationId, progressRatio * 100.0, currentTime, endTime,
                                details == null ? 0 : details.currentStep(), details == null ? 0 : details.totalSteps(),
                                "RUNNING");
                    } catch (Exception e) {
                        logger.debug("WebSocket progress delivery failed for {}", simulationId, e);
                    }
                }
            });
        }
    }

    /** Publishes committed terminal state at most once, including manual completions. */
    public void completeProgressStreams(String simulationId, boolean success) {
        SimulationJob job = jobs.get(simulationId);
        if (job != null) {
            synchronized (job) {
                publishTerminal(job);
            }
        }
    }

    private void publishTerminal(SimulationJob job) {
        if (!isTerminal(job.response) || job.terminalPublished) {
            return;
        }
        job.terminalPublished = true;
        boolean success = job.response.getStatus() == SimulationStatus.COMPLETED;
        String simulationId = job.response.getSimulationId();
        List<SseEmitter> emitters = new ArrayList<>(job.emitters);
        job.emitters.clear();
        String errorMessage = job.response.getErrorMessage();
        job.mailbox.terminal(() -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(success ? "complete" : "error")
                            .data("{\"simulationId\":\"" + simulationId + "\"}"));
                    emitter.complete();
                } catch (Exception ignored) {
                    // Delivery is attempted once for each subscribed transport.
                }
            }
            if (webSocketProgressService != null) {
                try {
                    webSocketProgressService.broadcastCompletion(simulationId, success, errorMessage);
                } catch (Exception e) {
                    logger.debug("WebSocket terminal delivery failed for {}", simulationId, e);
                }
            }
        }, () -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.completeWithError(new RejectedExecutionException("Progress delivery is busy. Poll job status."));
                } catch (RuntimeException ignored) {
                    // The subscriber can already be disconnected.
                }
            }
            logger.debug("Terminal delivery rejected for {}. Final status remains available.", simulationId);
        });
    }

    public void clearAll() {
        synchronized (admissionMonitor) {
            for (BatchJob batch : batches.values()) {
                synchronized (batch) {
                    batch.cancelled = true;
                }
            }
            for (String simulationId : jobs.keySet()) {
                cancelSimulation(simulationId);
            }
            jobs.clear();
            batches.clear();
        }
    }

    @PreDestroy
    public void shutdown() {
        clearAll();
        executorService.shutdownNow();
        if (deliveryExecutor instanceof ExecutorService pool) {
            pool.shutdownNow();
        }
    }

    SolverType parseSolverType(String value) {
        return CircuitFileService.parseSolverType(value);
    }

    boolean markRunning(SimulationResponse response) {
        synchronized (response) {
            if (response.getStatus() != SimulationStatus.PENDING) {
                return false;
            }
            response.setStatus(SimulationStatus.RUNNING);
            return true;
        }
    }

    boolean markCancelled(SimulationResponse response) {
        synchronized (response) {
            if (isTerminal(response)) {
                return false;
            }
            response.setErrorMessage(CANCELLED_BY_USER);
            response.setEndTime(Instant.now());
            response.setStatus(SimulationStatus.FAILED);
            return true;
        }
    }

    boolean applySuccessfulResult(SimulationResponse response, SimulationResult result) {
        synchronized (response) {
            if (isTerminal(response)) {
                return false;
            }
            String[] names = result.getSignalNames();
            for (int i = 0; i < names.length; i++) {
                float[] values = result.getSignalData(i);
                if (values != null) {
                    double[] converted = new double[values.length];
                    for (int j = 0; j < values.length; j++) {
                        converted[j] = values[j];
                    }
                    response.addResult(names[i], converted);
                }
            }
            response.addResult("time", result.getTimeArray());
            response.setErrorMessage(null);
            response.setEndTime(Instant.now());
            response.setStatus(SimulationStatus.COMPLETED);
            return true;
        }
    }

    boolean applyFailureResult(SimulationResponse response, String errorMessage) {
        synchronized (response) {
            if (isTerminal(response)) {
                return false;
            }
            response.setErrorMessage(errorMessage);
            response.setEndTime(Instant.now());
            response.setStatus(SimulationStatus.FAILED);
            return true;
        }
    }

    private static boolean isTerminal(SimulationResponse response) {
        return response.getStatus() == SimulationStatus.COMPLETED || response.getStatus() == SimulationStatus.FAILED;
    }

    /**
     * Captures one base snapshot and prepares every child before publishing the Batch.
     * Accepted jobs own their prepared settings and parameters independently of later caller changes.
     *
     * @param request the source selection, parameter variations, concurrency limit, and optional settings.
     * @return a detached Batch summary after atomic capacity admission.
     * @throws CircuitPreparationException for invalid input or a child that fails preparation.
     * @throws ResponseStatusException when retained capacity prevents admission.
     */
    public BatchSimulationResponse submitBatch(BatchSimulationRequest request) {
        int requestedLimit = request.getMaxConcurrent() == null
                ? DEFAULT_BATCH_CONCURRENCY : request.getMaxConcurrent();
        if (requestedLimit < 0 || requestedLimit > 100) throw invalidBatch("Batch concurrency must be between zero and 100");
        CircuitFileService.ResolvedSource source = circuitFiles.resolveSource(request.getCircuitId(), request.getCircuitFile());
        BatchSimulationRequest.SimulationSettings settings = request.getSimulationSettings();
        Double duration = settings == null ? null : settings.getSimulationTime();
        Double dt = settings == null ? null : settings.getTimeStep();
        if (!source.stored()) {
            if (duration == null) duration = 0.02;
            if (dt == null) dt = 1e-6;
        }
        List<Map<String, Double>> parameterSets = expandParameterSets(request);
        List<PreparedCircuit> prepared = new ArrayList<>();
        for (Map<String, Double> parameters : parameterSets) {
            SimulationRequest child = new SimulationRequest(request.getCircuitFile(), duration, dt);
            child.setCircuitId(request.getCircuitId());
            child.setParameters(parameters);
            child.setSolverType(settings == null ? null : settings.getSolverType());
            prepared.add(prepare(child, source));
        }
        BatchJob batch = new BatchJob(requestedLimit);
        synchronized (admissionMonitor) {
            requireCapacity(prepared.size(), true);
            List<String> ids = new ArrayList<>();
            for (PreparedCircuit circuit : prepared) {
                SimulationJob job = createJob(circuit, batch);
                batch.pending.add(job);
                ids.add(job.response.getSimulationId());
            }
            batch.response = new BatchSimulationResponse(UUID.randomUUID().toString(), List.copyOf(ids), requestedLimit);
            batches.put(batch.response.getBatchId(), batch);
        }
        dispatchBatch(batch);
        return batchSnapshot(batch);
    }

    private void dispatchBatch(BatchJob batch) {
        boolean retry;
        do {
            List<SimulationJob> admitted = new ArrayList<>();
            synchronized (batch) {
                while (!batch.cancelled && batch.active < batch.limit && !batch.pending.isEmpty()) {
                    SimulationJob job = batch.pending.remove();
                    if (!job.isCancelled()) {
                        batch.active++;
                        admitted.add(job);
                    }
                }
            }
            retry = false;
            for (SimulationJob job : admitted) {
                if (!dispatch(job)) {
                    synchronized (batch) {
                        batch.active--;
                    }
                    retry = true;
                }
            }
        } while (retry);
    }

    private void releaseBatchSlot(SimulationJob job) {
        if (job.batch != null) {
            synchronized (job.batch) {
                job.batch.active--;
            }
            dispatchBatch(job.batch);
        }
    }

    public BatchJobStatus getBatchStatus(String batchId) {
        BatchJob batch = batches.get(batchId);
        if (batch == null) {
            return null;
        }
        BatchJobStatus status = new BatchJobStatus();
        status.setBatchId(batchId);
        status.setTotal(batch.response.getTotalSimulations());
        status.setSubmittedAt(batch.response.getSubmittedAt());
        int completed = 0;
        int failed = 0;
        int running = 0;
        int pending = 0;
        List<String> failedIds = new ArrayList<>();
        Map<String, String> childStatuses = new HashMap<>();
        for (String id : batch.response.getSimulationIds()) {
            SimulationJob child = jobs.get(id);
            if (child == null) {
                continue;
            }
            SimulationStatus childStatus;
            synchronized (child) {
                childStatus = child.response.getStatus();
            }
            childStatuses.put(id, childStatus.toString());
            switch (childStatus) {
                case COMPLETED -> completed++;
                case FAILED -> { failed++; failedIds.add(id); }
                case RUNNING -> running++;
                case PENDING -> pending++;
            }
        }
        status.setCompleted(completed);
        status.setFailed(failed);
        status.setRunning(running);
        status.setPending(pending);
        status.setOverallProgress(status.getTotal() == 0 ? 0.0 : (completed + failed) * 100.0 / status.getTotal());
        status.setDone(completed + failed == status.getTotal());
        status.setSimulationStatuses(childStatuses);
        status.setFailedIds(failedIds.isEmpty() ? null : failedIds);
        return status;
    }

    public BatchSimulationResponse getBatch(String batchId) {
        BatchJob batch = batches.get(batchId);
        return batch == null ? null : batchSnapshot(batch);
    }

    private BatchSimulationResponse batchSnapshot(BatchJob batch) {
        BatchSimulationResponse source = batch.response;
        BatchSimulationResponse result = new BatchSimulationResponse();
        result.setBatchId(source.getBatchId());
        result.setTotalSimulations(source.getTotalSimulations());
        result.setSimulationIds(new ArrayList<>(source.getSimulationIds()));
        result.setSubmittedAt(source.getSubmittedAt());
        result.setMaxConcurrent(source.getMaxConcurrent());
        result.setMessage(source.getMessage());
        return result;
    }

    public boolean cancelBatch(String batchId) {
        BatchJob batch = batches.get(batchId);
        if (batch == null) {
            return false;
        }
        synchronized (batch) {
            batch.cancelled = true;
        }
        batch.response.getSimulationIds().forEach(this::cancelSimulation);
        return true;
    }

    private static final class SimulationJob {
        private final String owner;
        private final PreparedCircuit circuit;
        private final ExecutionOptions options = ExecutionOptions.defaults();
        private final BatchJob batch;
        private final SimulationResponse response;
        private final List<SseEmitter> emitters = new ArrayList<>();
        private HeadlessSimulationEngine engine;
        private boolean terminalPublished;
        private final ProgressMailbox mailbox;

        private SimulationJob(PreparedCircuit circuit, BatchJob batch, Executor deliveryExecutor, String owner) {
            this.owner = owner;
            mailbox = new ProgressMailbox(deliveryExecutor);
            this.circuit = circuit;
            this.batch = batch;
            response = new SimulationResponse(UUID.randomUUID().toString());
            response.setStartTime(Instant.now());
        }

        private synchronized boolean isCancelled() {
            return response.getStatus() == SimulationStatus.FAILED
                    && CANCELLED_BY_USER.equals(response.getErrorMessage());
        }
    }

    private static final class BatchJob {
        private final int limit;
        private final Queue<SimulationJob> pending = new ArrayDeque<>();
        private BatchSimulationResponse response;
        private int active;
        private boolean cancelled;

        private BatchJob(int requestedLimit) {
            limit = requestedLimit == 0 ? DEFAULT_BATCH_CONCURRENCY : Math.max(1, requestedLimit);
        }
    }
    private List<java.util.Map<String, Double>> expandParameterSets(BatchSimulationRequest req) {
        // Explicit list takes priority
        if (req.getParameterSets() != null && !req.getParameterSets().isEmpty()) {
            if (req.getParameterSets().size() > 100) throw invalidBatch("A Batch supports at most 100 explicit parameter sets");
            List<Map<String, Double>> owned = new ArrayList<>();
            for (Map<String, Double> row : req.getParameterSets()) {
                if (row == null) throw invalidBatch("Each Batch parameter set must be a map");
                owned.add(java.util.Collections.unmodifiableMap(new HashMap<>(row)));
            }
            return List.copyOf(owned);
        }
        // Linear sweep
        if (req.getLinearSweep() != null) {
            BatchSimulationRequest.LinearSweep sweep = req.getLinearSweep();
            List<java.util.Map<String, Double>> sets = new ArrayList<>();
            int n = sweep.getPoints();
            validateSweep(sweep.getParameterPath(), sweep.getStartValue(), sweep.getEndValue(), n, false);
            for (int i = 0; i < n; i++) {
                double val = sweep.getStartValue() + i * (sweep.getEndValue() - sweep.getStartValue()) / (n - 1);
                java.util.Map<String, Double> params = new java.util.HashMap<>();
                if (sweep.getFixedParameters() != null) {
                    params.putAll(sweep.getFixedParameters());
                }
                params.put(sweep.getParameterPath(), val);
                sets.add(params);
            }
            return sets;
        }
        // Log sweep
        if (req.getLogSweep() != null) {
            BatchSimulationRequest.LogSweep sweep = req.getLogSweep();
            List<java.util.Map<String, Double>> sets = new ArrayList<>();
            int n = sweep.getPoints();
            validateSweep(sweep.getParameterPath(), sweep.getStartValue(), sweep.getEndValue(), n, true);
            double logStart = Math.log10(sweep.getStartValue());
            double logEnd = Math.log10(sweep.getEndValue());
            for (int i = 0; i < n; i++) {
                double logVal = logStart + i * (logEnd - logStart) / (n - 1);
                double val = Math.pow(10, logVal);
                java.util.Map<String, Double> params = new java.util.HashMap<>();
                if (sweep.getFixedParameters() != null) {
                    params.putAll(sweep.getFixedParameters());
                }
                params.put(sweep.getParameterPath(), val);
                sets.add(params);
            }
            return sets;
        }
        throw invalidBatch("A Batch requires parameter sets or a sweep");
    }

    private static void validateSweep(String path, double start, double end, int points, boolean logarithmic) {
        if (path == null || path.isBlank() || points < 2 || points > 1000
                || !Double.isFinite(start) || !Double.isFinite(end) || (logarithmic && (start <= 0 || end <= 0))) {
            throw invalidBatch("A sweep requires a parameter path, finite limits, and two to 1000 points");
        }
    }

    private static CircuitPreparationException invalidBatch(String message) {
        return CircuitPreparationException.failure(HttpStatus.BAD_REQUEST, "INVALID_BATCH",
                Diagnostic.Category.CONFIGURATION, message, null);
    }

}

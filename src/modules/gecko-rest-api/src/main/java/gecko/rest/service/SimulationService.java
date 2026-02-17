package gecko.rest.service;

import gecko.core.allg.SolverType;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationProgress;
import gecko.core.simulation.SimulationResult;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for simulation operations.
 * Handles business logic for circuit simulations using HeadlessSimulationEngine.
 *
 * This service integrates with the gecko-simulation-core module to run
 * actual circuit simulations without any GUI dependencies.
 */
@Service
public class SimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    static final String CANCELLED_BY_USER = "Cancelled by user";

    private final Map<String, SimulationResponse> simulationStore = new ConcurrentHashMap<>();
    private final Map<String, HeadlessSimulationEngine> runningEngines = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> progressEmitters = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    /**
     * Register an SSE emitter to receive progress events for a simulation.
     *
     * @param simulationId the simulation ID
     * @param timeoutMs the timeout in milliseconds for the SSE connection
     * @return an SseEmitter for streaming progress, or null if simulation not found
     */
    public SseEmitter registerProgressStream(String simulationId, long timeoutMs) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response == null) {
            return null;
        }

        SseEmitter emitter = new SseEmitter(timeoutMs);
        progressEmitters.computeIfAbsent(simulationId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Clean up when client disconnects or times out
        emitter.onCompletion(() -> removeEmitter(simulationId, emitter));
        emitter.onTimeout(() -> removeEmitter(simulationId, emitter));
        emitter.onError(e -> removeEmitter(simulationId, emitter));

        // If already completed/failed, send final event immediately
        SimulationResponse.SimulationStatus status = response.getStatus();
        if (status == SimulationResponse.SimulationStatus.COMPLETED ||
            status == SimulationResponse.SimulationStatus.FAILED) {
            try {
                emitter.send(SseEmitter.event()
                    .name("status")
                    .data("{\"status\":\"" + status + "\",\"simulationId\":\"" + simulationId + "\"}"));
                emitter.complete();
            } catch (Exception ignored) {
                // Client already gone
            }
        }

        return emitter;
    }

    /**
     * Removes an emitter from the list for a simulation.
     */
    private void removeEmitter(String simulationId, SseEmitter emitter) {
        List<SseEmitter> emitters = progressEmitters.get(simulationId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                progressEmitters.remove(simulationId);
            }
        }
    }

    /**
     * Broadcast a progress update to all registered SSE emitters for a simulation.
     */
    public void broadcastProgress(String simulationId, double progress, double currentTime, double endTime) {
        List<SseEmitter> emitters = progressEmitters.get(simulationId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String data = String.format(
            "{\"progress\":%.3f,\"currentTime\":%.6f,\"endTime\":%.6f,\"simulationId\":\"%s\"}",
            progress, currentTime, endTime, simulationId
        );

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        dead.forEach(e -> removeEmitter(simulationId, e));
    }

    /**
     * Complete all SSE emitters when a simulation finishes.
     */
    public void completeProgressStreams(String simulationId, boolean success) {
        List<SseEmitter> emitters = progressEmitters.remove(simulationId);
        if (emitters == null) {
            return;
        }
        String event = success ? "complete" : "error";
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data("{\"simulationId\":\"" + simulationId + "\"}"));
                emitter.complete();
            } catch (Exception ignored) {
                // Client already gone
            }
        }
    }

    /**
     * Submit a new simulation job.
     * The simulation runs asynchronously and results are stored when complete.
     *
     * @param request Simulation request parameters
     * @return Created simulation response with ID for tracking
     */
    public SimulationResponse submitSimulation(SimulationRequest request) {
        String simulationId = UUID.randomUUID().toString();
        SimulationResponse response = new SimulationResponse(simulationId);
        response.setStatus(SimulationResponse.SimulationStatus.PENDING);
        response.setStartTime(Instant.now());

        simulationStore.put(simulationId, response);

        // Start simulation asynchronously
        executorService.submit(() -> runSimulation(simulationId, request));

        logger.info("Submitted simulation {} for circuit: {}", simulationId, request.getCircuitFile());
        return response;
    }

    /**
     * Runs the simulation using HeadlessSimulationEngine.
     */
    private void runSimulation(String simulationId, SimulationRequest request) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response == null) {
            return;
        }

        if (!markRunning(response)) {
            logger.info("Skipping simulation {} because status is {}", simulationId, response.getStatus());
            return;
        }

        try {
            SimulationConfig config = buildSimulationConfig(request);

            // Create and run the simulation engine
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            runningEngines.put(simulationId, engine);

            // Set up progress listener with SSE broadcasting
            engine.setProgressListener((currentTime, endTime, currentStep) -> {
                double progress = endTime > 0 ? currentTime / endTime : 0;
                broadcastProgress(simulationId, progress, currentTime, endTime);
                logger.debug("Simulation {} progress: {:.1f}%", simulationId, (progress * 100));
            });

            logger.info("Starting simulation {} with dt={}, duration={}",
                    simulationId, request.getTimeStep(), request.getSimulationTime());

            // Run the simulation
            SimulationResult result = engine.runSimulation(config);

            // Process results
            if (result.isSuccess()) {
                if (applySuccessfulResult(response, result)) {
                    completeProgressStreams(simulationId, true);
                    logger.info("Simulation {} completed: {} steps in {} ms",
                            simulationId, result.getTotalTimeSteps(), result.getExecutionTimeMs());
                } else {
                    completeProgressStreams(simulationId, false);
                    logger.info("Simulation {} completed after cancellation request; keeping cancelled status",
                            simulationId);
                }
            } else {
                if (applyFailureResult(response, result.getErrorMessage())) {
                    completeProgressStreams(simulationId, false);
                    logger.error("Simulation {} failed: {}", simulationId, result.getErrorMessage());
                }
            }

        } catch (Exception e) {
            if (applyFailureResult(response, "Simulation error: " + e.getMessage())) {
                completeProgressStreams(simulationId, false);
                logger.error("Simulation {} threw exception", simulationId, e);
            }
        } finally {
            runningEngines.remove(simulationId);
        }
    }

    /**
     * Get simulation by ID.
     * If simulation is running, includes detailed progress information.
     *
     * @param simulationId Simulation identifier
     * @return Simulation response or null if not found
     */
    public SimulationResponse getSimulation(String simulationId) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null && response.getStatus() == SimulationResponse.SimulationStatus.RUNNING) {
            // Populate progress details if simulation is running
            HeadlessSimulationEngine engine = runningEngines.get(simulationId);
            if (engine != null) {
                SimulationProgress detailed = engine.getDetailedProgress();
                if (detailed != null) {
                    response.setProgressDetails(new SimulationResponse.ProgressDetails(
                        detailed.getPreCalcProgress(),
                        detailed.getMainSimProgress(),
                        detailed.getCurrentTime(),
                        detailed.getEndTime(),
                        detailed.getCurrentStep(),
                        detailed.getTotalSteps(),
                        detailed.getEstimatedRemainingMs()
                    ));
                }
            }
        }
        return response;
    }

    /**
     * Get all simulations.
     *
     * @return Map of all simulations by ID
     */
    public Map<String, SimulationResponse> getAllSimulations() {
        return new ConcurrentHashMap<>(simulationStore);
    }

    /**
     * Cancel a pending or running simulation.
     *
     * @param simulationId Simulation identifier
     * @return Updated simulation response
     */
    public SimulationResponse cancelSimulation(String simulationId) {
        // Try to cancel the running engine
        HeadlessSimulationEngine engine = runningEngines.get(simulationId);
        if (engine != null) {
            engine.cancel();
            logger.info("Cancellation requested for simulation {}", simulationId);
        }

        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null) {
            markCancelled(response);
        }
        return response;
    }

    /**
     * Get simulation progress.
     *
     * @param simulationId Simulation identifier
     * @return Progress as a percentage (0-100), or -1 if simulation is unknown
     */
    public double getSimulationProgress(String simulationId) {
        HeadlessSimulationEngine engine = runningEngines.get(simulationId);
        if (engine != null) {
            return engine.getProgress() * 100.0;
        }
        SimulationResponse response = simulationStore.get(simulationId);
        if (response == null) {
            return -1;
        }
        if (response.getStatus() == SimulationResponse.SimulationStatus.COMPLETED) {
            return 100.0;
        }
        return 0.0;
    }

    /**
     * Get specific signal data from a completed simulation.
     *
     * @param simulationId Simulation identifier
     * @param signalName Name of the signal
     * @return Signal data array or null if not found
     */
    public double[] getSignalData(String simulationId, String signalName) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null) {
            return response.getResult(signalName);
        }
        return null;
    }

    /**
     * Update simulation status.
     *
     * @param simulationId Simulation identifier
     * @param status New status
     */
    public void updateStatus(String simulationId, SimulationResponse.SimulationStatus status) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null) {
            synchronized (response) {
                response.setStatus(status);
                if (status == SimulationResponse.SimulationStatus.COMPLETED ||
                        status == SimulationResponse.SimulationStatus.FAILED) {
                    response.setEndTime(Instant.now());
                }
            }
        }
    }

    /**
     * Add results to a completed simulation.
     *
     * @param simulationId Simulation identifier
     * @param signalName Name of the signal
     * @param signalData Array of signal values
     */
    public void addResults(String simulationId, String signalName, double[] signalData) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null) {
            response.addResult(signalName, signalData);
        }
    }

    /**
     * Clear all simulation data (for testing/cleanup).
     */
    public void clearAll() {
        // Cancel all running simulations
        for (HeadlessSimulationEngine engine : runningEngines.values()) {
            engine.cancel();
        }
        runningEngines.clear();
        simulationStore.clear();
        progressEmitters.clear();
    }

    /**
     * Get count of simulations by status.
     *
     * @return Map of status to count
     */
    public Map<SimulationResponse.SimulationStatus, Long> getStatistics() {
        Map<SimulationResponse.SimulationStatus, Long> stats = new ConcurrentHashMap<>();
        for (SimulationResponse.SimulationStatus status : SimulationResponse.SimulationStatus.values()) {
            stats.put(status, simulationStore.values().stream()
                    .filter(r -> r.getStatus() == status)
                    .count());
        }
        return stats;
    }

    /**
     * Check if a simulation is currently running.
     *
     * @param simulationId Simulation identifier
     * @return true if simulation is running
     */
    public boolean isRunning(String simulationId) {
        return runningEngines.containsKey(simulationId);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    SimulationConfig buildSimulationConfig(SimulationRequest request) {
        SimulationConfig.Builder builder = SimulationConfig.builder()
                .circuitFile(request.getCircuitFile())
                .stepWidth(request.getTimeStep())
                .simulationDuration(request.getSimulationTime())
                .solverType(parseSolverType(request.getSolverType()));

        if (request.getParameters() != null) {
            builder.withParameters(request.getParameters());
        }

        return builder.build();
    }

    /**
     * Parse solver type string to SolverType enum.
     * Supports: "backward-euler", "be", "trapezoidal", "trz", "gear-shichman", "gear", "gs"
     * Defaults to SOLVER_BE if string is null or unrecognized.
     *
     * @param solverTypeStr solver type string (case-insensitive)
     * @return parsed SolverType
     */
    SolverType parseSolverType(String solverTypeStr) {
        if (solverTypeStr == null) {
            return SolverType.SOLVER_BE;
        }
        return switch (solverTypeStr.toLowerCase().trim()) {
            case "trapezoidal", "trz" -> SolverType.SOLVER_TRZ;
            case "gear-shichman", "gear", "gs" -> SolverType.SOLVER_GS;
            default -> SolverType.SOLVER_BE;
        };
    }

    boolean markRunning(SimulationResponse response) {
        synchronized (response) {
            if (response.getStatus() != SimulationResponse.SimulationStatus.PENDING) {
                return false;
            }
            response.setStatus(SimulationResponse.SimulationStatus.RUNNING);
            return true;
        }
    }

    boolean markCancelled(SimulationResponse response) {
        synchronized (response) {
            if (response.getStatus() == SimulationResponse.SimulationStatus.FAILED
                    && isBlank(response.getErrorMessage())) {
                response.setErrorMessage(CANCELLED_BY_USER);
                response.setEndTime(Instant.now());
                return true;
            }
            if (response.getStatus() != SimulationResponse.SimulationStatus.PENDING
                    && response.getStatus() != SimulationResponse.SimulationStatus.RUNNING) {
                return false;
            }
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage(CANCELLED_BY_USER);
            response.setEndTime(Instant.now());
            return true;
        }
    }

    boolean applySuccessfulResult(SimulationResponse response, SimulationResult result) {
        synchronized (response) {
            if (isCancelled(response)) {
                return false;
            }

            // Copy result data to response before exposing COMPLETED.
            String[] signalNames = result.getSignalNames();
            for (int i = 0; i < signalNames.length; i++) {
                float[] floatData = result.getSignalData(i);
                if (floatData == null) {
                    continue;
                }
                double[] doubleData = new double[floatData.length];
                for (int j = 0; j < floatData.length; j++) {
                    doubleData[j] = floatData[j];
                }
                response.addResult(signalNames[i], doubleData);
            }
            response.addResult("time", result.getTimeArray());
            response.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
            response.setErrorMessage(null);
            response.setEndTime(Instant.now());
            return true;
        }
    }

    boolean applyFailureResult(SimulationResponse response, String errorMessage) {
        synchronized (response) {
            if (isCancelled(response)) {
                return false;
            }
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage(errorMessage);
            response.setEndTime(Instant.now());
            return true;
        }
    }

    private static boolean isCancelled(SimulationResponse response) {
        return response.getStatus() == SimulationResponse.SimulationStatus.FAILED
                && CANCELLED_BY_USER.equals(response.getErrorMessage());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

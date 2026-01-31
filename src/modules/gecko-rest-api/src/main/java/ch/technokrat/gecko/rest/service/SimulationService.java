package ch.technokrat.gecko.rest.service;

import ch.technokrat.gecko.core.allg.SolverType;
import ch.technokrat.gecko.core.simulation.HeadlessSimulationEngine;
import ch.technokrat.gecko.core.simulation.SimulationConfig;
import ch.technokrat.gecko.core.simulation.SimulationResult;
import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, SimulationResponse> simulationStore = new ConcurrentHashMap<>();
    private final Map<String, HeadlessSimulationEngine> runningEngines = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

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

        response.setStatus(SimulationResponse.SimulationStatus.RUNNING);

        try {
            // Create simulation configuration
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(request.getCircuitFile())
                    .stepWidth(request.getTimeStep())
                    .simulationDuration(request.getSimulationTime())
                    .solverType(SolverType.SOLVER_BE) // Default to Backward Euler
                    .build();

            // Apply parameter overrides if provided
            if (request.getParameters() != null) {
                for (Map.Entry<String, Double> entry : request.getParameters().entrySet()) {
                    config = SimulationConfig.builder()
                            .circuitFile(request.getCircuitFile())
                            .stepWidth(request.getTimeStep())
                            .simulationDuration(request.getSimulationTime())
                            .solverType(SolverType.SOLVER_BE)
                            .withParameter(entry.getKey(), entry.getValue())
                            .build();
                }
            }

            // Create and run the simulation engine
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            runningEngines.put(simulationId, engine);

            // Set up progress listener
            engine.setProgressListener((currentTime, endTime, currentStep) -> {
                // Could add WebSocket or SSE progress updates here
                logger.debug("Simulation {} progress: {:.1f}%", simulationId,
                        (currentTime / endTime * 100));
            });

            logger.info("Starting simulation {} with dt={}, duration={}",
                    simulationId, request.getTimeStep(), request.getSimulationTime());

            // Run the simulation
            SimulationResult result = engine.runSimulation(config);

            // Process results
            if (result.isSuccess()) {
                response.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
                response.setEndTime(Instant.now());

                // Copy result data to response
                String[] signalNames = result.getSignalNames();
                for (int i = 0; i < signalNames.length; i++) {
                    float[] floatData = result.getSignalData(i);
                    if (floatData != null) {
                        double[] doubleData = new double[floatData.length];
                        for (int j = 0; j < floatData.length; j++) {
                            doubleData[j] = floatData[j];
                        }
                        response.addResult(signalNames[i], doubleData);
                    }
                }

                // Add time array
                response.addResult("time", result.getTimeArray());

                logger.info("Simulation {} completed: {} steps in {} ms",
                        simulationId, result.getTotalTimeSteps(), result.getExecutionTimeMs());
            } else {
                response.setStatus(SimulationResponse.SimulationStatus.FAILED);
                response.setErrorMessage(result.getErrorMessage());
                response.setEndTime(Instant.now());

                logger.error("Simulation {} failed: {}", simulationId, result.getErrorMessage());
            }

        } catch (Exception e) {
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage("Simulation error: " + e.getMessage());
            response.setEndTime(Instant.now());

            logger.error("Simulation {} threw exception", simulationId, e);
        } finally {
            runningEngines.remove(simulationId);
        }
    }

    /**
     * Get simulation by ID.
     *
     * @param simulationId Simulation identifier
     * @return Simulation response or null if not found
     */
    public SimulationResponse getSimulation(String simulationId) {
        return simulationStore.get(simulationId);
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
     * Cancel a running simulation.
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
        if (response != null && response.getStatus() == SimulationResponse.SimulationStatus.RUNNING) {
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage("Cancelled by user");
            response.setEndTime(Instant.now());
        }
        return response;
    }

    /**
     * Get simulation progress for a running simulation.
     *
     * @param simulationId Simulation identifier
     * @return Progress as a percentage (0-100), or -1 if not running
     */
    public double getSimulationProgress(String simulationId) {
        HeadlessSimulationEngine engine = runningEngines.get(simulationId);
        if (engine != null) {
            return engine.getProgress() * 100.0;
        }
        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null && response.getStatus() == SimulationResponse.SimulationStatus.COMPLETED) {
            return 100.0;
        }
        return -1;
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
        if (response != null && response.getResults() != null) {
            return response.getResults().get(signalName);
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
            response.setStatus(status);
            if (status == SimulationResponse.SimulationStatus.COMPLETED ||
                    status == SimulationResponse.SimulationStatus.FAILED) {
                response.setEndTime(Instant.now());
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
}

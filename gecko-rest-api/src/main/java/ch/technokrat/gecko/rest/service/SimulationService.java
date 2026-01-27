package ch.technokrat.gecko.rest.service;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for simulation operations.
 * Handles business logic for circuit simulations.
 * This MVP version uses in-memory storage; production would use database + message queue.
 */
@Service
public class SimulationService {

    private final Map<String, SimulationResponse> simulationStore = new ConcurrentHashMap<>();

    /**
     * Submit a new simulation job.
     *
     * @param request Simulation request parameters
     * @return Created simulation response
     */
    public SimulationResponse submitSimulation(SimulationRequest request) {
        String simulationId = UUID.randomUUID().toString();
        SimulationResponse response = new SimulationResponse(simulationId);
        response.setStatus(SimulationResponse.SimulationStatus.PENDING);
        response.setStartTime(Instant.now());

        simulationStore.put(simulationId, response);
        return response;
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
     * Cancel a simulation.
     *
     * @param simulationId Simulation identifier
     * @return Updated simulation response
     */
    public SimulationResponse cancelSimulation(String simulationId) {
        SimulationResponse response = simulationStore.get(simulationId);
        if (response != null) {
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage("Cancelled by user");
            response.setEndTime(Instant.now());
        }
        return response;
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
}

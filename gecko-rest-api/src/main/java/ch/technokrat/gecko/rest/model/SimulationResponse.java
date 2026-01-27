package ch.technokrat.gecko.rest.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Response DTO for circuit simulation results.
 * Contains simulation status, timestamps, and output signal data.
 */
public class SimulationResponse {

    public enum SimulationStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    private String simulationId;
    private SimulationStatus status;
    private Instant startTime;
    private Instant endTime;
    private Map<String, double[]> results;
    private String errorMessage;

    // Constructors
    public SimulationResponse() {
        this.results = new HashMap<>();
        this.status = SimulationStatus.PENDING;
    }

    public SimulationResponse(String simulationId) {
        this();
        this.simulationId = simulationId;
    }

    // Getters and setters
    public String getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public SimulationStatus getStatus() {
        return status;
    }

    public void setStatus(SimulationStatus status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Map<String, double[]> getResults() {
        return results;
    }

    public void setResults(Map<String, double[]> results) {
        this.results = results;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addResult(String signalName, double[] signalData) {
        this.results.put(signalName, signalData);
    }

    public long getExecutionTimeMs() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return -1;
    }

    @Override
    public String toString() {
        return "SimulationResponse{" +
                "simulationId='" + simulationId + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", resultsCount=" + (results != null ? results.size() : 0) +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

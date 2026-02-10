package ch.technokrat.gecko.rest.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Response DTO for circuit simulation results.
 * Contains simulation status, timestamps, and output signal data.
 */
public class SimulationResponse {

    public enum SimulationStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    private String simulationId;
    private volatile SimulationStatus status;
    private volatile Instant startTime;
    private volatile Instant endTime;
    private final Map<String, double[]> results;
    private volatile String errorMessage;

    // Constructors
    public SimulationResponse() {
        this.results = new ConcurrentHashMap<>();
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
        return Collections.unmodifiableMap(copyResults(results));
    }

    public void setResults(Map<String, double[]> results) {
        this.results.clear();
        if (results == null) {
            return;
        }
        results.forEach(this::addResult);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addResult(String signalName, double[] signalData) {
        if (signalName == null) {
            return;
        }
        this.results.put(signalName, signalData == null ? new double[0] : Arrays.copyOf(signalData, signalData.length));
    }

    public double[] getResult(String signalName) {
        double[] data = this.results.get(signalName);
        if (data == null) {
            return null;
        }
        return Arrays.copyOf(data, data.length);
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

    private static Map<String, double[]> copyResults(Map<String, double[]> source) {
        Map<String, double[]> copy = new HashMap<>();
        source.forEach((key, value) -> copy.put(key,
                value == null ? new double[0] : Arrays.copyOf(value, value.length)));
        return copy;
    }
}

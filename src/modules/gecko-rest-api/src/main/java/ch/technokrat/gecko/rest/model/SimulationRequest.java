package ch.technokrat.gecko.rest.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

/**
 * Request DTO for circuit simulation submissions.
 * Contains circuit file reference and simulation parameters.
 */
public class SimulationRequest {

    @NotBlank(message = "Circuit file path cannot be blank")
    private String circuitFile;

    @NotNull(message = "Simulation time cannot be null")
    @Positive(message = "Simulation time must be positive")
    private Double simulationTime;

    @NotNull(message = "Time step cannot be null")
    @Positive(message = "Time step must be positive")
    private Double timeStep;

    private Map<String, Double> parameters;

    // Constructors
    public SimulationRequest() {
    }

    public SimulationRequest(String circuitFile, Double simulationTime, Double timeStep) {
        this.circuitFile = circuitFile;
        this.simulationTime = simulationTime;
        this.timeStep = timeStep;
    }

    // Getters and setters
    public String getCircuitFile() {
        return circuitFile;
    }

    public void setCircuitFile(String circuitFile) {
        this.circuitFile = circuitFile;
    }

    public Double getSimulationTime() {
        return simulationTime;
    }

    public void setSimulationTime(Double simulationTime) {
        this.simulationTime = simulationTime;
    }

    public Double getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(Double timeStep) {
        this.timeStep = timeStep;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Double> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "SimulationRequest{" +
                "circuitFile='" + circuitFile + '\'' +
                ", simulationTime=" + simulationTime +
                ", timeStep=" + timeStep +
                ", parameters=" + parameters +
                '}';
    }
}

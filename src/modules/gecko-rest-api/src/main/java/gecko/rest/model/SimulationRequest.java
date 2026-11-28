package gecko.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import java.util.Map;

/**
 * Request DTO for circuit simulation submissions.
 * Contains a circuit source and optional simulation settings.
 * Stored sources supply omitted settings independently.
 * Path submissions require a simulation time and a time step.
 * A null optional setting has the same meaning as an omitted setting.
 */
public class SimulationRequest {

    private String circuitFile;
    private String circuitId;

    @Positive(message = "Simulation time must be positive")
    private Double simulationTime;

    @Positive(message = "Time step must be positive")
    private Double timeStep;

    private Map<String, Double> parameters;

    private String solverType;  // Optional. Inherits the captured solver.

    @Valid
    @Schema(description = "Optional periodic steady-state search. The simulation first runs period by period from "
            + "the DC operating point until the full state repeats, records nothing during the search, then "
            + "records simulationTime seconds from that state. Off when omitted. The period must be a whole number "
            + "of time steps, and a CONTROL carrier that clamps per step has a simulated period of a whole number "
            + "of steps, so a 35 kHz carrier at a 1 microsecond step has a period of 30 microseconds, not 28.57. "
            + "Search steps count toward the step limit. A pre-calculation time cannot combine with it.")
    private PeriodicSteadyState periodicSteadyState;

    /** Settings of the periodic steady-state search. */
    @Schema(name = "PeriodicSteadyStateRequest", description = "Optional periodic steady-state search. The simulation "
            + "first runs period by period from the DC operating point until the full state repeats, and records "
            + "nothing during the search. It then records simulationTime seconds from that state. Off when omitted. "
            + "The period must be a whole number of time steps. A CONTROL carrier that clamps per step has a simulated "
            + "period of a whole number of steps, so a 35 kHz carrier at a 1 microsecond step has a period of "
            + "30 microseconds, not 28.57. Search steps count toward the step limit. A pre-calculation time cannot "
            + "combine with the search. Batch requests do not accept this field.")
    public static class PeriodicSteadyState {
        @NotNull(message = "Period is required")
        @Positive(message = "Period must be positive")
        @Schema(description = "Simulated period T in seconds. A whole number of time steps, at least 2, and a whole "
                + "multiple of the CONTROL sample period if one is set.", example = "3e-5")
        private Double period;

        @Positive(message = "Tolerance must be positive")
        @DecimalMax(value = "0.1", message = "Tolerance must be at most 0.1")
        @Schema(description = "Relative state change at which the search stops, above 0 and at most 0.1. "
                + "Defaults to 1e-4.", example = "1e-4")
        private Double tolerance;

        @Min(value = 3, message = "Period limit must be at least 3")
        @Max(value = 1_000_000, message = "Period limit must be at most 1000000")
        @Schema(description = "Most periods the search may run, from 3 to 1000000. Defaults to 1000. "
                + "The job fails with steadyState.converged false when the limit is reached.", example = "1000")
        private Integer maxPeriods;

        public Double getPeriod() { return period; }
        public void setPeriod(Double period) { this.period = period; }
        public Double getTolerance() { return tolerance; }
        public void setTolerance(Double tolerance) { this.tolerance = tolerance; }
        public Integer getMaxPeriods() { return maxPeriods; }
        public void setMaxPeriods(Integer maxPeriods) { this.maxPeriods = maxPeriods; }
    }

    // Constructors
    public SimulationRequest() {
    }

    public SimulationRequest(String circuitFile, Double simulationTime, Double timeStep) {
        this.circuitFile = circuitFile;
        this.simulationTime = simulationTime;
        this.timeStep = timeStep;
    }

    // Getters and setters
    /**
     * Returns the explicit registry source selection.
     *
     * @return the exact registry ID, or null when the legacy source field is selected.
     */
    public String getCircuitId() { return circuitId; }
    /**
     * Selects an explicit registry ID for validation during admission.
     * Select this field or circuitFile, but not both.
     *
     * @param circuitId the exact live registry ID, or null to omit this selection.
     */
    public void setCircuitId(String circuitId) { this.circuitId = circuitId; }

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

    public String getSolverType() {
        return solverType;
    }

    public void setSolverType(String solverType) {
        this.solverType = solverType;
    }

    public PeriodicSteadyState getPeriodicSteadyState() {
        return periodicSteadyState;
    }

    public void setPeriodicSteadyState(PeriodicSteadyState periodicSteadyState) {
        this.periodicSteadyState = periodicSteadyState;
    }

    @Override
    public String toString() {
        return "SimulationRequest{" +
                "circuitFile='" + circuitFile + '\'' +
                ", simulationTime=" + simulationTime +
                ", timeStep=" + timeStep +
                ", solverType='" + solverType + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}

package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Partial update of circuit simulation parameters.
 * All fields are optional - only provided fields are updated.
 */
@Schema(description = "Partial update of circuit simulation parameters")
public class CircuitParameterUpdate {

    @Schema(description = "Simulation duration in seconds", example = "0.05")
    private Double simulationDuration;

    @Schema(description = "Time step in seconds", example = "5e-7")
    private Double timeStep;

    @Schema(
        description = "Solver type: backward-euler, trapezoidal, gear-shichman",
        example = "trapezoidal"
    )
    private String solverType;

    public CircuitParameterUpdate() {
    }

    public CircuitParameterUpdate(Double simulationDuration, Double timeStep, String solverType) {
        this.simulationDuration = simulationDuration;
        this.timeStep = timeStep;
        this.solverType = solverType;
    }

    public Double getSimulationDuration() {
        return simulationDuration;
    }

    public void setSimulationDuration(Double simulationDuration) {
        this.simulationDuration = simulationDuration;
    }

    public Double getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(Double timeStep) {
        this.timeStep = timeStep;
    }

    public String getSolverType() {
        return solverType;
    }

    public void setSolverType(String solverType) {
        this.solverType = solverType;
    }
}

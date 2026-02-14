package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Detailed circuit information including structure and metadata.
 */
@Schema(description = "Detailed circuit information")
public record CircuitInfo(
    @Schema(description = "Circuit identifier")
    String circuitId,

    @Schema(description = "Original filename")
    String filename,

    @Schema(description = "Circuit format version", example = "1.0")
    String version,

    @Schema(description = "List of circuit components")
    List<ComponentInfo> components,

    @Schema(description = "List of connections/wires")
    List<ConnectionInfo> connections,

    @Schema(description = "Simulation parameters if available")
    SimulationParameters simulationParameters
) {
    /**
     * Simulation parameters extracted from circuit file.
     */
    @Schema(description = "Simulation parameters")
    public record SimulationParameters(
        @Schema(description = "Simulation end time [s]", example = "0.1")
        Double endTime,

        @Schema(description = "Time step [s]", example = "1e-6")
        Double timeStep,

        @Schema(description = "Solver type", example = "SOLVER_BE")
        String solverType
    ) {}
}

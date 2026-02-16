package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detailed circuit information including structure and metadata.
 */
@Schema(description = "Detailed circuit information")
public record CircuitInfo(
    @Schema(description = "Circuit identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    String circuitId,

    @Schema(description = "Original filename", example = "buck_converter.ipes")
    String filename,

    @Schema(description = "Circuit file format version", example = "124")
    Integer fileVersion,

    @Schema(description = "Simulation parameters")
    SimulationParameters simulationParameters,

    @Schema(description = "Component counts by domain")
    ComponentCounts componentCounts,

    @Schema(description = "Display settings from circuit file")
    DisplaySettings displaySettings,

    @Schema(description = "File metadata")
    Metadata metadata
) {
    /**
     * Simulation parameters extracted from circuit file.
     */
    @Schema(description = "Simulation parameters")
    public record SimulationParameters(
        @Schema(description = "Simulation end time [s]", example = "0.02")
        Double endTime,

        @Schema(description = "Time step [s]", example = "1e-6")
        Double timeStep,

        @Schema(description = "Solver type", example = "backward-euler", allowableValues = {"backward-euler", "trapezoidal", "gear-shichman"})
        String solverType,

        @Schema(description = "Pre-calculation time [s]", example = "0.0")
        Double preSimulationTime,

        @Schema(description = "Pre-calculation step width [s]", example = "1e-7")
        Double preSimulationTimeStep
    ) {}

    /**
     * Component counts by simulation domain.
     */
    @Schema(description = "Component counts by domain")
    public record ComponentCounts(
        @Schema(description = "Number of circuit (LK) components", example = "15")
        int circuit,

        @Schema(description = "Number of control components", example = "20")
        int control,

        @Schema(description = "Number of thermal components", example = "5")
        int thermal,

        @Schema(description = "Number of connections/wires", example = "42")
        int connections
    ) {}

    /**
     * Display settings preserved from circuit file.
     */
    @Schema(description = "Display settings")
    public record DisplaySettings(
        @Schema(description = "Window width in pixels", example = "1400")
        Integer windowWidth,

        @Schema(description = "Window height in pixels", example = "900")
        Integer windowHeight,

        @Schema(description = "Font size", example = "12")
        Integer fontSize
    ) {}

    /**
     * File metadata.
     */
    @Schema(description = "File metadata")
    public record Metadata(
        @Schema(description = "Creation date", example = "2026-02-15")
        String creationDate,

        @Schema(description = "Unique file identifier", example = "12345")
        Integer uniqueFileId
    ) {}
}

package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Information about a circuit component.
 */
@Schema(description = "Circuit component information")
public record ComponentInfo(
    @Schema(description = "Component type", example = "MOSFET")
    String type,

    @Schema(description = "Component ID", example = "Q1")
    String id,

    @Schema(description = "Component label/name", example = "Main Switch")
    String label,

    @Schema(description = "X position in schematic", example = "100")
    Integer xPosition,

    @Schema(description = "Y position in schematic", example = "200")
    Integer yPosition
) {}

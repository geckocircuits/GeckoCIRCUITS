package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Information about a connection/wire between components.
 */
@Schema(description = "Connection between circuit components")
public record ConnectionInfo(
    @Schema(description = "Source component terminal", example = "Q1.drain")
    String from,

    @Schema(description = "Destination component terminal", example = "L1.in")
    String to,

    @Schema(description = "Wire label/name", example = "switch_node")
    String label
) {}

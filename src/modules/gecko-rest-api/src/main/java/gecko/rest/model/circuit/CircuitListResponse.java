package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response containing a list of loaded circuits.
 */
@Schema(description = "List of loaded circuits")
public record CircuitListResponse(
    @Schema(description = "List of circuit summaries")
    List<CircuitSummary> circuits,

    @Schema(description = "Total number of loaded circuits", example = "5")
    int total
) {
    /**
     * Summary information about a loaded circuit.
     */
    @Schema(description = "Circuit summary")
    public record CircuitSummary(
        @Schema(description = "Circuit identifier")
        String circuitId,

        @Schema(description = "Original filename")
        String filename,

        @Schema(description = "Total component count")
        int componentCount,

        @Schema(description = "Time when circuit was loaded (ISO 8601)", example = "2026-02-16T10:30:00Z")
        String loadedAt
    ) {}
}

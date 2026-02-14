package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response after loading a circuit file.
 * Contains circuit ID and basic metadata.
 */
@Schema(description = "Response after loading a circuit file")
public record CircuitLoadResponse(
    @Schema(description = "Unique circuit identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    String circuitId,

    @Schema(description = "Load status", example = "loaded")
    String status,

    @Schema(description = "Original filename", example = "buck_converter.ipes")
    String filename,

    @Schema(description = "Number of components in circuit", example = "15")
    int componentCount,

    @Schema(description = "Error message if status is 'failed'")
    String errorMessage
) {
    /**
     * Create a successful load response.
     */
    public static CircuitLoadResponse success(String circuitId, String filename, int componentCount) {
        return new CircuitLoadResponse(circuitId, "loaded", filename, componentCount, null);
    }

    /**
     * Create a failed load response.
     */
    public static CircuitLoadResponse failure(String filename, String errorMessage) {
        return new CircuitLoadResponse(null, "failed", filename, 0, errorMessage);
    }
}

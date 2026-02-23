package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response for circuit validation.
 */
@Schema(description = "Circuit validation result")
public record ValidationResponse(
    @Schema(description = "Whether the circuit is valid", example = "true")
    boolean valid,

    @Schema(description = "Validation warnings (non-critical issues)")
    List<String> warnings,

    @Schema(description = "Validation errors (critical issues)")
    List<String> errors
) {
    /**
     * Create a successful validation response.
     */
    public static ValidationResponse success() {
        return new ValidationResponse(true, List.of(), List.of());
    }

    /**
     * Create a successful validation with warnings.
     */
    public static ValidationResponse successWithWarnings(List<String> warnings) {
        return new ValidationResponse(true, warnings, List.of());
    }

    /**
     * Create a failed validation response.
     */
    public static ValidationResponse failure(List<String> errors) {
        return new ValidationResponse(false, List.of(), errors);
    }

    /**
     * Create a failed validation with warnings.
     */
    public static ValidationResponse failure(List<String> warnings, List<String> errors) {
        return new ValidationResponse(false, warnings, errors);
    }
}

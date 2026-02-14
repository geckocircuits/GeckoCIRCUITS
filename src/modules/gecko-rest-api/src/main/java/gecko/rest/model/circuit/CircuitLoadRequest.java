package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for loading a circuit file.
 * Supports base64 encoded content for programmatic access.
 */
@Schema(description = "Request for loading a circuit file from base64 content")
public record CircuitLoadRequest(
    @Schema(description = "Base64 encoded .ipes file content", example = "H4sIAAAAAAAA...")
    @NotBlank
    String content,

    @Schema(description = "Original filename", example = "buck_converter.ipes")
    @NotBlank
    String filename
) {}

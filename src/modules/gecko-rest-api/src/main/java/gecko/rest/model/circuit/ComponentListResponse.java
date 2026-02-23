package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response containing a list of circuit components.
 */
@Schema(description = "Response containing circuit components")
public record ComponentListResponse(
    @Schema(description = "Circuit identifier")
    String circuitId,

    @Schema(description = "List of all components in the circuit")
    List<ComponentInfo> components
) {}

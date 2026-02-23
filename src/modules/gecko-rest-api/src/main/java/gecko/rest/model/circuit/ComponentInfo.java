package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Information about a circuit component.
 */
@Schema(description = "Circuit component information")
public record ComponentInfo(
    @Schema(description = "Component type identifier", example = "1")
    int type,

    @Schema(description = "Component name/label", example = "R1")
    String name,

    @Schema(description = "Simulation domain", example = "circuit", allowableValues = {"circuit", "control", "thermal"})
    String domain,

    @Schema(description = "X-Y position in schematic", example = "[100, 200]")
    int[] position,

    @Schema(description = "Component orientation (0, 90, 180, 270)", example = "0")
    int orientation,

    @Schema(description = "Component parameters (e.g., resistance, capacitance)", example = "{\"resistance\": 10.0, \"temperature\": 25.0}")
    Map<String, Object> parameters
) {}

package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.List;
import gecko.core.preparation.ConfigurationField;

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
    Map<String, Object> parameters,
    List<ConfigurationField> configurationFields
) {
    /**
     * Preserves construction without a writable configuration descriptor list.
     * Raw inspection parameters do not establish which fields accept numeric overrides.
     *
     * @param type the component type identifier.
     * @param name the complete component name.
     * @param domain the inspection domain.
     * @param position the schematic coordinates.
     * @param orientation the schematic orientation.
     * @param parameters the raw inspection parameters.
     */
    public ComponentInfo(int type, String name, String domain, int[] position, int orientation, Map<String, Object> parameters) {
        this(type, name, domain, position, orientation, parameters, List.of());
    }
}

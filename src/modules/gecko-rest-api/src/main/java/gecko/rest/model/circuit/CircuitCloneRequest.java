package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Request to clone a circuit with optional parameter overrides.
 */
@Schema(description = "Request to clone a circuit with optional parameter overrides")
public class CircuitCloneRequest {

    @Schema(
        description = "Parameter overrides in dot-notation format (ComponentName.parameterKey -> value)",
        example = "{\"R1.resistance\": 50.0}"
    )
    private Map<String, Double> overrides;

    public CircuitCloneRequest() {
    }

    public CircuitCloneRequest(Map<String, Double> overrides) {
        this.overrides = overrides;
    }

    public Map<String, Double> getOverrides() {
        return overrides;
    }

    public void setOverrides(Map<String, Double> overrides) {
        this.overrides = overrides;
    }
}

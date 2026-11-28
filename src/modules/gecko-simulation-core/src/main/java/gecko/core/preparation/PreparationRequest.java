package gecko.core.preparation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Owns the caller's requested settings and numeric assignments. */
public record PreparationRequest(SolverOverrides settings, Map<String, Double> parameters) {
    /**
     * Copies assignments into an immutable map. Invalid entries remain available for atomic validation during preparation.
     *
     * @param settings the optional setting overrides
     * @param parameters the complete numeric assignment set
     * @throws NullPointerException if settings or parameters is null
     */
    public PreparationRequest {
        settings = Objects.requireNonNull(settings, "Settings are required");
        parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "Parameters are required")));
    }
    /**
     * Selects captured settings and no parameter assignments.
     *
     * @return a request that inherits the captured configuration
     */
    public static PreparationRequest defaults() { return new PreparationRequest(SolverOverrides.none(), Map.of()); }
}

package gecko.core.preparation;

import gecko.core.allg.SolverType;

/** Optional settings. Null fields inherit the captured values. */
public record SolverOverrides(SolverType solver, Double timeStep, Double duration, SolverFeatures features) {
    /** Null features mean every opt-in feature is off. */
    public SolverOverrides {
        features = features == null ? SolverFeatures.none() : features;
    }

    /** Overrides only the circuit-file settings and leaves opt-in features off. */
    public SolverOverrides(SolverType solver, Double timeStep, Double duration) {
        this(solver, timeStep, duration, null);
    }

    /**
     * Leaves every setting absent so preparation inherits each captured value.
     *
     * @return overrides with no supplied setting
     */
    public static SolverOverrides none() { return new SolverOverrides(null, null, null); }
}

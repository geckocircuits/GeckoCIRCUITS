package gecko.core.preparation;

import gecko.core.allg.SolverSettingsCore;

/**
 * Opt-in solver features that circuit files do not describe. Each feature is off by default.
 * Preparation retains this object, so every session opened from a prepared circuit inherits it.
 * Add new opt-in features as components here and apply them in {@link #applyTo}.
 *
 * @param controlSamplePeriod CONTROL sample period Ts in seconds, finite and not negative.
 *        Zero runs CONTROL at every electrical step. A positive value runs CONTROL at the first
 *        electrical step with t &gt;= k*Ts and holds its outputs between samples.
 *        The controller sees the nominal Ts as its step, so delays and integrators count controller ticks.
 *        An electrical step longer than Ts runs CONTROL at every step with that longer step.
 * @param zeroCrossing locates diode, thyristor and IGBT conduction changes inside a step by splitting the
 *        electrical interval. CONTROL and recordings stay on the fixed grid. Gear-Shichman rejects it.
 */
public record SolverFeatures(double controlSamplePeriod, boolean zeroCrossing) {
    private static final SolverFeatures NONE = new SolverFeatures(0, false);

    /** @param controlSamplePeriod CONTROL sample period with every other feature off */
    public SolverFeatures(double controlSamplePeriod) { this(controlSamplePeriod, false); }

    /** @return every feature off */
    public static SolverFeatures none() { return NONE; }

    /** @return true when every value can execute */
    boolean valid() { return Double.isFinite(controlSamplePeriod) && controlSamplePeriod >= 0; }

    /** Writes the feature values into execution settings. */
    SolverSettingsCore applyTo(SolverSettingsCore settings) {
        return settings.withControlSamplePeriod(controlSamplePeriod).withZeroCrossing(zeroCrossing);
    }
}

package gecko.core.preparation;

import gecko.core.allg.PeriodicSteadyStateSettings;
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
 * @param dcOperatingPoint seeds the first electrical step from a solved DC operating point instead of the
 *        configured initial states.
 * @param periodicSteadyState searches for the periodic steady state before the main phase. Null is off.
 *        The search turns on the DC operating point start for its own session only.
 */
public record SolverFeatures(double controlSamplePeriod, boolean zeroCrossing, boolean dcOperatingPoint,
        PeriodicSteadyStateSettings periodicSteadyState) {
    private static final SolverFeatures NONE = new SolverFeatures(0, false, false, null);

    /** @param controlSamplePeriod CONTROL sample period with every other feature off */
    public SolverFeatures(double controlSamplePeriod) { this(controlSamplePeriod, false, false, null); }

    /** CONTROL sample period, event location and DC start, with the steady-state search off. */
    public SolverFeatures(double controlSamplePeriod, boolean zeroCrossing, boolean dcOperatingPoint) {
        this(controlSamplePeriod, zeroCrossing, dcOperatingPoint, null);
    }

    /** CONTROL sample period and event location, with DC start off. */
    public SolverFeatures(double controlSamplePeriod, boolean zeroCrossing) { this(controlSamplePeriod, zeroCrossing, false, null); }

    /** @return every feature off */
    public static SolverFeatures none() { return NONE; }

    /** @return true when every value can execute */
    boolean valid() { return Double.isFinite(controlSamplePeriod) && controlSamplePeriod >= 0; }

    /** Writes the feature values into execution settings. */
    SolverSettingsCore applyTo(SolverSettingsCore settings) {
        return settings.withControlSamplePeriod(controlSamplePeriod).withZeroCrossing(zeroCrossing)
                .withDcOperatingPoint(dcOperatingPoint).withPeriodicSteadyState(periodicSteadyState);
    }
}

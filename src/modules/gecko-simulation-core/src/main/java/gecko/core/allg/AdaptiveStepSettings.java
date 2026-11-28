package gecko.core.allg;

/**
 * Settings of the adaptive electrical step. Null in the settings that carry it means the feature is off.
 * The step adapts only between the fixed CONTROL and recording grid points. It never exceeds the grid step.
 *
 * @param tolerance the relative local-error tolerance, from 0 (exclusive) to 0.1
 * @param minStep the smallest electrical sub-step in seconds, or NaN for the main grid step divided by 1000
 * @param maxStep the largest electrical sub-step in seconds, or NaN for the main grid step
 */
public record AdaptiveStepSettings(double tolerance, double minStep, double maxStep) {
    /** Default tolerance. */
    public static final double DEFAULT_TOLERANCE = 1e-3;
    /** Largest tolerance. */
    public static final double MAX_TOLERANCE = 0.1;
    /** Divisor of the grid step that gives the default smallest sub-step. */
    public static final double DEFAULT_MIN_DIVISOR = 1000;

    /** Creates settings with the default tolerance and the default clamps. */
    public AdaptiveStepSettings() { this(DEFAULT_TOLERANCE, Double.NaN, Double.NaN); }

    /** Creates settings with the default clamps. */
    public AdaptiveStepSettings(double tolerance) { this(tolerance, Double.NaN, Double.NaN); }

    /** @return the smallest sub-step for a grid step */
    public double resolvedMinStep(double gridStep) { return Double.isNaN(minStep) ? gridStep / DEFAULT_MIN_DIVISOR : minStep; }

    /** @return the largest sub-step for a grid step */
    public double resolvedMaxStep(double gridStep) { return Double.isNaN(maxStep) ? gridStep : maxStep; }
}

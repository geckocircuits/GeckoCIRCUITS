package gecko.core.simulation;

/**
 * Constant-memory statistics of the adaptive electrical step.
 *
 * @param accepted committed sub-steps
 * @param rejected trials that the local-error estimate rejected and retried with a shorter step.
 *     Discarded event and refinement trials are not counted
 * @param forced committed sub-steps whose error estimate exceeded the tolerance at the smallest step or after the retry bound
 * @param minStep shortest committed sub-step, or NaN when none committed
 * @param maxStep longest committed sub-step, or NaN when none committed
 * @param located diode-family events located inside a grid step
 * @param unlocated diode-family state changes that were not located
 */
public record AdaptiveStepReport(long accepted, long rejected, long forced, double minStep, double maxStep,
        long located, long unlocated) {
    /** @return the report of a run that does not adapt */
    public static AdaptiveStepReport empty() { return new AdaptiveStepReport(0, 0, 0, Double.NaN, Double.NaN, 0, 0); }
}

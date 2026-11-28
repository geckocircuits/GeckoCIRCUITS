package gecko.core.simulation;

/**
 * Result of a periodic steady-state search.
 *
 * @param converged true when the full periodic state repeated within the tolerance
 * @param periods the number of periods that the search ran
 * @param residual the largest scaled state change over the last period, infinite after a discrete mismatch
 * @param estimate the contraction-corrected remaining error, infinite when the change does not shrink
 * @param worstState a description of the state with the largest change, or empty before the first comparison
 * @param startTime the time of the last committed search step in seconds, where the main phase continues
 */
public record SteadyStateReport(boolean converged, int periods, double residual, double estimate,
        String worstState, double startTime) { }

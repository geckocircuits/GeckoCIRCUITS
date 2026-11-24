package gecko.core.allg;

/**
 * Settings of the periodic steady-state search. Null in the settings that carry it means the search is off.
 *
 * @param period the simulated period T in seconds, a whole number of steps
 * @param tolerance the relative state change that ends the search, from 0 (exclusive) to 0.1
 * @param maxPeriods the most periods the search may run, from 3 to 1,000,000
 */
public record PeriodicSteadyStateSettings(double period, double tolerance, int maxPeriods) {
    /** Default tolerance. */
    public static final double DEFAULT_TOLERANCE = 1e-4;
    /** Default period limit. */
    public static final int DEFAULT_MAX_PERIODS = 1000;
    /** Smallest period limit. */
    public static final int MIN_PERIODS = 3;
    /** Largest period limit. */
    public static final int MAX_PERIODS = 1_000_000;
    /** Largest tolerance. */
    public static final double MAX_TOLERANCE = 0.1;

    /** Creates settings with the default tolerance and period limit. */
    public PeriodicSteadyStateSettings(double period) { this(period, DEFAULT_TOLERANCE, DEFAULT_MAX_PERIODS); }
}

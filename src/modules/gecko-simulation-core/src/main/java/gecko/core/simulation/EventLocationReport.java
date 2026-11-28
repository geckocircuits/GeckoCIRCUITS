package gecko.core.simulation;

/**
 * Counts of diode-family switching events that zero-crossing location handled.
 *
 * @param located events whose time is inside a step
 * @param unlocated transitions that were committed at the step end because the per-step event limit
 *        or the one-event-per-device rule prevented location
 * @param firstUnlocatedTime step end time of the first unlocated transition, or NaN when there is none
 * @param retained events kept in the bounded log
 * @param dropped located events that did not fit in the log
 */
public record EventLocationReport(long located, long unlocated, double firstUnlocatedTime, int retained, long dropped) {
    /** @return the report of a run that locates nothing */
    public static EventLocationReport empty() { return new EventLocationReport(0, 0, Double.NaN, 0, 0); }
}

package gecko.core.control;

/**
 * A calculator with private state beyond its output signal. The periodic steady-state search compares
 * the exported values from one period to the next. The method only reads, and it runs after a committed step.
 * It must emit the same number of values in the same order on every call.
 */
public interface PeriodicStateSource {
    /** @param sink receives every private state value that the next step reads */
    void exportState(StateSink sink);
}

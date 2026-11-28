package gecko.core.preparation;

/** Logging choices cannot change the prepared configuration. */
public record ExecutionOptions(boolean loggingEnabled, int loggingInterval) {
    /**
     * Validates logging choices without changing prepared settings.
     *
     * @param loggingEnabled whether to retain logged samples
     * @param loggingInterval the positive number of time steps between samples
     * @throws IllegalArgumentException if the interval is less than one
     */
    public ExecutionOptions {
        if (loggingInterval < 1) throw new IllegalArgumentException("Logging interval must be positive");
    }
    /**
     * Selects data logging at every time step.
     *
     * @return the default logging options
     */
    public static ExecutionOptions defaults() { return new ExecutionOptions(true, 1); }
}

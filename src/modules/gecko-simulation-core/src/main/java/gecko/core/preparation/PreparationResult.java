package gecko.core.preparation;

/** Rejected requests do not publish an effective circuit. */
public sealed interface PreparationResult {
    /**
     * Returns all known preparation diagnostics, including findings from independent checks.
     *
     * @return the immutable readiness report
     */
    ReadinessReport report();
    /** Invalid requested assignments or explicit settings publish no effective circuit. */
    record Rejected(ReadinessReport report) implements PreparationResult {}
    /** Accepted changes remain inspectable when invalid or unsupported input prevents execution. */
    record Inspectable(CircuitSnapshot circuit, ReadinessReport report) implements PreparationResult {}
    /** Preflight permits execution, with no guarantee of numerical convergence. */
    record Ready(CircuitSnapshot circuit, PreparedCircuit execution, ReadinessReport report) implements PreparationResult {}
}

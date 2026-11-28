package gecko.core.preparation;

import java.util.List;

/** Readiness does not predict numerical convergence. */
public record ReadinessReport(Outcome outcome, List<Diagnostic> diagnostics) {
    public enum Outcome { READY, INVALID, UNSUPPORTED }
    /**
     * Copies diagnostics so later caller changes cannot change the report.
     *
     * @param outcome the conservative preflight outcome
     * @param diagnostics the findings from configuration and readiness checks
     */
    public ReadinessReport { diagnostics = List.copyOf(diagnostics); }
    /**
     * Indicates that preflight permits execution. This result does not guarantee numerical convergence.
     *
     * @return true only for a READY outcome
     */
    public boolean readyForHeadless() { return outcome == Outcome.READY; }
}

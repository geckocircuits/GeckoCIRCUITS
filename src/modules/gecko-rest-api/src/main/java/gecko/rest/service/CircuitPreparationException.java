package gecko.rest.service;

import gecko.core.preparation.Diagnostic;
import gecko.core.preparation.ReadinessReport;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Carries preparation findings through the HTTP boundary. */
public final class CircuitPreparationException extends ResponseStatusException {
    private final ReadinessReport report;

    /**
     * Associates an HTTP error status with immutable preparation findings.
     *
     * @param status the response status for the rejected operation.
     * @param report the readiness outcome and typed diagnostics.
     */
    public CircuitPreparationException(HttpStatus status, ReadinessReport report) {
        super(status, report.diagnostics().isEmpty() ? "Circuit preparation failed" : report.diagnostics().get(0).message());
        this.report = report;
    }

    /**
     * Returns the immutable readiness report for the rejected operation.
     *
     * @return the outcome and typed diagnostics.
     */
    public ReadinessReport report() { return report; }

    static CircuitPreparationException failure(HttpStatus status, String code, Diagnostic.Category category,
            String message, String parameterPath) {
        return new CircuitPreparationException(status, new ReadinessReport(ReadinessReport.Outcome.INVALID,
                List.of(new Diagnostic(code, category, Diagnostic.Severity.ERROR, message,
                        null, null, parameterPath, null))));
    }
}

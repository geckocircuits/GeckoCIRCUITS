package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import gecko.core.preparation.Diagnostic;
import gecko.core.preparation.ReadinessReport;

/**
 * Response for circuit validation.
 */
@Schema(description = "Circuit validation result")
public record ValidationResponse(
    @Schema(description = "Whether the circuit is valid", example = "true")
    boolean valid,

    @Schema(description = "Validation warnings (non-critical issues)")
    List<String> warnings,

    @Schema(description = "Validation errors (critical issues)")
    List<String> errors,
    boolean readyForHeadless,
    ReadinessReport.Outcome outcome,
    List<Diagnostic> diagnostics
) {
    /**
     * Preserves construction through the legacy three-field response shape.
     * The valid flag supplies the readiness summary, and typed diagnostics remain empty.
     *
     * @param valid whether the legacy result permits Headless execution.
     * @param warnings the legacy warning messages.
     * @param errors the legacy error messages.
     */
    public ValidationResponse(boolean valid, List<String> warnings, List<String> errors) {
        this(valid, warnings, errors, valid, valid ? ReadinessReport.Outcome.READY : ReadinessReport.Outcome.INVALID, List.of());
    }

    /**
     * Builds the legacy summary and typed readiness fields from one preparation report.
     * This conversion does not execute a simulation.
     *
     * @param report the immutable preparation outcome and diagnostics.
     * @return a response whose valid flag conservatively matches Headless readiness.
     */
    public static ValidationResponse fromReport(ReadinessReport report) {
        return new ValidationResponse(report.readyForHeadless(),
                report.diagnostics().stream().filter(item -> item.severity() == Diagnostic.Severity.WARNING)
                        .map(Diagnostic::message).toList(),
                report.diagnostics().stream().filter(item -> item.severity() == Diagnostic.Severity.ERROR)
                        .map(Diagnostic::message).toList(),
                report.readyForHeadless(), report.outcome(), report.diagnostics());
    }

    /**
     * Create a successful validation response.
     */
    public static ValidationResponse success() {
        return new ValidationResponse(true, List.of(), List.of());
    }

    /**
     * Create a successful validation with warnings.
     */
    public static ValidationResponse successWithWarnings(List<String> warnings) {
        return new ValidationResponse(true, warnings, List.of());
    }

    /**
     * Create a failed validation response.
     */
    public static ValidationResponse failure(List<String> errors) {
        return new ValidationResponse(false, List.of(), errors);
    }

    /**
     * Create a failed validation with warnings.
     */
    public static ValidationResponse failure(List<String> warnings, List<String> errors) {
        return new ValidationResponse(false, warnings, errors);
    }
}

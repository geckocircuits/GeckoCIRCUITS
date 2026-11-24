package gecko.core.preparation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.io.ImportIssue;
import gecko.core.simulation.SimulationTiming;
import java.util.ArrayList;
import java.util.List;

/** Captures circuit ownership and resolves an execution description. */
public final class CircuitPreparation {
    /**
     * Lists supported writable numeric fields without changing the snapshot.
     * CONTROL SIGNAL phaseDegrees and phase use degrees. Their param3 alias uses radians.
     *
     * @param source the captured circuit to inspect
     * @return an immutable field list with immutable alias lists
     */
    public List<ConfigurationField> configurationFields(CircuitSnapshot source) {
        return CircuitConfiguration.components(source.toModel()).stream()
                .flatMap(component -> CircuitConfiguration.fields(component).stream().map(field ->
                        new ConfigurationField(component.domain(), component.data().getName(), field.name(),
                                "param" + field.slot(), field.aliases())))
                .toList();
    }

    /**
     * Copies model data, settings, connections, and parser evidence into an owned snapshot.
     * Later caller changes cannot change the captured input. This method does not run preflight.
     *
     * @param source the model to capture
     * @return an immutable snapshot with detached inspection views
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if a parameter contains an unsupported mutable value
     */
    public CircuitSnapshot capture(CircuitModel source) { return new CircuitSnapshot(source); }
    /**
     * Normalizes captured configuration, applies assignments atomically, and checks readiness without time steps.
     * A Rejected result publishes no changed circuit. An Inspectable result retains accepted changes but cannot execute.
     * A Ready result contains an immutable execution description. Readiness does not guarantee numerical convergence.
     * The base snapshot remains unchanged for every outcome.
     *
     * @param base the captured input whose omitted settings are inherited
     * @param request the owned settings and numeric assignments
     * @return the effective circuit and readiness report, or a rejection report
     */
    public PreparationResult prepare(CircuitSnapshot base, PreparationRequest request) {
        var model = base.toModel();
        for (var finding : CircuitConfiguration.normalize(model)) {
            var issue = new ImportIssue(finding.code(), finding.domain(), finding.component(), finding.message(),
                    finding.sourceLocation(), false, finding.parameterPath());
            if (!model.getImportIssues().contains(issue)) model.getImportIssues().add(issue);
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        var overrideDiagnostics = CircuitConfiguration.apply(model, request.parameters());
        diagnostics.addAll(overrideDiagnostics);
        var settings = request.settings();
        if (settings.solver() != null) model.setSolverType(settings.solver());
        if (settings.timeStep() != null) {
            if (positive(settings.timeStep())) model.setTimeStep(settings.timeStep());
            else diagnostics.add(invalidSetting("timeStep", "Time step must be finite and positive"));
        }
        if (settings.duration() != null) {
            if (positive(settings.duration())) model.setSimulationDuration(settings.duration());
            else diagnostics.add(invalidSetting("duration", "Simulation duration must be finite and positive"));
        }
        if (!settings.features().valid()) {
            diagnostics.add(invalidSetting("controlSamplePeriod", "CONTROL sample period must be finite and non-negative"));
        }
        if (settings.features().zeroCrossing() && model.getSolverType() == SolverType.SOLVER_GS) {
            diagnostics.add(invalidSetting("zeroCrossing", SimulationTiming.GS_ZERO_CROSSING_MESSAGE));
        }
        model.getImportIssues().removeIf(issue -> issue.code().equals("INVALID_SETTING")
                && switch (issue.parameterPath() == null ? "" : issue.parameterPath()) {
                    case "solver" -> settings.solver() != null;
                    case "timeStep" -> settings.timeStep() != null && positive(settings.timeStep());
                    case "duration" -> settings.duration() != null && positive(settings.duration());
                    default -> false;
                });
        boolean rejected = !overrideDiagnostics.isEmpty() || diagnostics.stream().anyMatch(item -> item.code().equals("INVALID_SETTING"));
        CircuitConfiguration.derive(model);
        var snapshot = capture(model);
        CircuitModel execution;
        try {
            execution = snapshot.executionModel();
        } catch (IllegalArgumentException error) {
            CircuitReadiness.appendImportDiagnostics(model, diagnostics);
            diagnostics.add(new Diagnostic("INVALID_TOPOLOGY", Diagnostic.Category.TOPOLOGY, Diagnostic.Severity.ERROR,
                    error.getMessage(), null, null, null, model.getFilePath()));
            CircuitReadiness.appendTimingDiagnostics(model, diagnostics);
            var report = new ReadinessReport(ReadinessReport.Outcome.INVALID, diagnostics);
            return rejected ? new PreparationResult.Rejected(report) : new PreparationResult.Inspectable(snapshot, report);
        }
        diagnostics.addAll(CircuitConfiguration.validate(execution));
        diagnostics.addAll(CircuitReadiness.check(execution, diagnostics));
        boolean invalid = diagnostics.stream().anyMatch(item -> item.severity() == Diagnostic.Severity.ERROR
                && item.category() != Diagnostic.Category.CAPABILITY);
        var outcome = invalid ? ReadinessReport.Outcome.INVALID
                : diagnostics.stream().anyMatch(item -> item.category() == Diagnostic.Category.CAPABILITY)
                    ? ReadinessReport.Outcome.UNSUPPORTED : ReadinessReport.Outcome.READY;
        var report = new ReadinessReport(outcome, diagnostics);
        if (rejected) return new PreparationResult.Rejected(report);
        if (!report.readyForHeadless()) return new PreparationResult.Inspectable(snapshot, report);
        return new PreparationResult.Ready(snapshot, new PreparedCircuit(snapshot, settings.features()), report);
    }
    private static boolean positive(double value) { return Double.isFinite(value) && value > 0; }
    private static Diagnostic invalidSetting(String path, String message) {
        return new Diagnostic("INVALID_SETTING", Diagnostic.Category.CONFIGURATION, Diagnostic.Severity.ERROR,
                message, null, null, path, null);
    }
}

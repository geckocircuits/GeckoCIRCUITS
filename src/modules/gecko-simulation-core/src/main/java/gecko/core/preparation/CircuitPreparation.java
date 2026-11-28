package gecko.core.preparation;

import gecko.core.allg.SolverType;
import gecko.core.ac.AcExecutionPlan;
import gecko.core.io.CircuitModel;
import gecko.core.io.HierarchyIndex;
import gecko.core.io.HierarchyMetadataCodec;
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
        var model = source.toModel();
        var hierarchy = model.getHierarchy() == null || model.getAcSettings() != null ? null : new HierarchyIndex(model);
        return CircuitConfiguration.components(model).stream()
                .flatMap(component -> CircuitConfiguration.fields(component).stream().map(field ->
                        new ConfigurationField(component.domain(),
                                CircuitConfiguration.executionName(hierarchy, component), field.name(),
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
        if (model.getAcSettings() != null) {
            if (model.isTransientUic()) {
                return new PreparationResult.Rejected(new ReadinessReport(ReadinessReport.Outcome.INVALID,
                        List.of(invalidSetting("transientUic", "UIC applies only to transient analysis."))));
            }
            return prepareAc(base, model, request);
        }
        boolean hierarchy = model.getHierarchy() != null;
        if ((hierarchy && !supportsTransientHierarchy(model)) || model.getCircuitComponents().stream().anyMatch(component ->
                component.getAcComponent() != null && component.getAcComponent().law() instanceof gecko.core.ac.AcComponent.Inductance)) {
            return new PreparationResult.Rejected(new ReadinessReport(ReadinessReport.Outcome.UNSUPPORTED,
                    List.of(new Diagnostic("UNSUPPORTED_TRANSIENT_LAW", Diagnostic.Category.CAPABILITY, Diagnostic.Severity.ERROR,
                            hierarchy ? "Transient hierarchy requires ordinary R/C, constant electrical sources and root voltage observation."
                                    : "Select AC analysis for the imported series resistance law.",
                            "LK", null, "analysis", model.getFilePath()))));
        }
        for (var finding : CircuitConfiguration.normalize(model)) {
            var issue = new ImportIssue(finding.code(), finding.domain(), finding.component(), finding.message(),
                    finding.sourceLocation(), false, finding.parameterPath());
            if (!model.getImportIssues().contains(issue)) model.getImportIssues().add(issue);
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<Diagnostic> overrideDiagnostics;
        try {
            overrideDiagnostics = CircuitConfiguration.apply(model, request.parameters());
        } catch (IllegalArgumentException error) {
            if (!hierarchy) throw error;
            return new PreparationResult.Rejected(new ReadinessReport(ReadinessReport.Outcome.INVALID,
                    List.of(new Diagnostic("INVALID_TOPOLOGY", Diagnostic.Category.TOPOLOGY, Diagnostic.Severity.ERROR,
                            error.getMessage(), null, null, null, model.getFilePath()))));
        }
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
        if (!SimulationTiming.validOutputStart(model.getOutputStartTime(), model.getSimulationDuration())) {
            diagnostics.add(invalidSetting("outputStartTime", "Output start time must be finite and between zero and the stop time"));
        }
        if (!settings.features().valid()) {
            diagnostics.add(invalidSetting("controlSamplePeriod", "CONTROL sample period must be finite and non-negative"));
        }
        if (settings.features().zeroCrossing() && model.getSolverType() == SolverType.SOLVER_GS) {
            diagnostics.add(invalidSetting("zeroCrossing", SimulationTiming.GS_ZERO_CROSSING_MESSAGE));
        }
        for (var issue : SimulationTiming.periodicSteadyStateIssues(settings.features().periodicSteadyState(),
                model.getTimeStep(), model.getSimulationDuration(), model.getPreSimulationTime(),
                settings.features().controlSamplePeriod(), settings.features().controlSubsystemPeriods())) {
            diagnostics.add(invalidSetting(issue.parameterPath(), issue.message()));
        }
        for (var issue : SimulationTiming.adaptiveStepIssues(settings.features().adaptiveStep(), model.getSolverType(),
                model.getTimeStep(), model.getPreSimulationTime(), model.getPreSimulationTimeStep())) {
            diagnostics.add(invalidSetting(issue.parameterPath(), issue.message()));
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
        if (model.getHierarchy() != null && !supportsTransientHierarchy(model)) {
            diagnostics.add(new Diagnostic("UNSUPPORTED_TRANSIENT_LAW", Diagnostic.Category.CAPABILITY,
                    Diagnostic.Severity.ERROR, "The configured hierarchy uses a source or element outside the transient subset.",
                    "LK", null, "analysis", model.getFilePath()));
            return new PreparationResult.Rejected(new ReadinessReport(ReadinessReport.Outcome.UNSUPPORTED, diagnostics));
        }
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
        for (var component : execution.getCircuitComponents()) {
            if (component.getAcComponent() == null) continue;
            var law = component.getAcComponent().law();
            boolean exponentialDiode = law instanceof gecko.core.ac.AcComponent.ExponentialDiode diode && diode.transientLaw();
            if (!exponentialDiode && !(law instanceof gecko.core.ac.AcComponent.Mosfet)
                    && !(law instanceof gecko.core.ac.AcComponent.Behavioural)) continue;
            if (execution.getHierarchy() != null || execution.isTransientUic() || settings.features().dcOperatingPoint()
                    || settings.features().periodicSteadyState() != null || execution.getSolverType() == SolverType.SOLVER_GS) {
                diagnostics.add(invalidSetting("solver", "Use backward Euler or trapezoidal integration on a flat circuit "
                        + "without DC start, periodic steady state, or UIC for transient semiconductor models."));
                rejected = true;
                break;
            }
            try {
                if (law instanceof gecko.core.ac.AcComponent.ExponentialDiode diode) diode.validate(execution.rootParameterMap());
                if (law instanceof gecko.core.ac.AcComponent.Mosfet transistor) transistor.evaluate(execution.rootParameterMap());
                if (law instanceof gecko.core.ac.AcComponent.Behavioural source) source.compile(execution.rootParameterMap());
            } catch (IllegalArgumentException error) {
                diagnostics.add(invalidSetting(component.getName(), error.getMessage()));
                rejected = true;
            }
        }
        if (execution.isTransientUic()) {
            String problem;
            try {
                problem = uicProblem(execution, settings.features());
            } catch (IllegalArgumentException error) {
                problem = error.getMessage();
            }
            if (problem != null) {
                diagnostics.add(invalidSetting("transientUic", problem));
                rejected = true;
            }
        }
        for (var issue : SimulationTiming.controlSubsystemIssues(execution, model.getTimeStep(),
                settings.features().controlSamplePeriod(), settings.features().controlSubsystemPeriods())) {
            diagnostics.add(invalidSetting(issue.parameterPath(), issue.message()));
            rejected = true;
        }
        if (settings.features().periodicSteadyState() != null && !rejected) {
            for (var issue : SimulationTiming.periodicSteadyStateCircuitIssues(execution, model.getTimeStep(),
                    settings.features().controlSamplePeriod(), settings.features().controlSubsystemPeriods(),
                    settings.features().periodicSteadyState())) {
                diagnostics.add(invalidSetting(issue.parameterPath(), issue.message()));
                rejected = true;
            }
        }
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

    private static boolean supportsTransientHierarchy(CircuitModel model) {
        if (!model.getThermalComponents().isEmpty() || !model.getResources().isEmpty()
                || !model.getResourceFailures().isEmpty() || !model.getStructuralComponents().isEmpty()
                || model.isTransientUic()) return false;
        if (model.getSpecialComponents().isEmpty() || model.getSpecialComponents().stream().anyMatch(component ->
                component.getType() != 27 || component.getSavedBlock() == null
                        || !component.getSavedBlock().sections().isEmpty()
                        || !component.getSavedBlock().tokens().containsKey(HierarchyMetadataCodec.OCCURRENCE)
                        || component.getSavedBlock().enabledState() != 1)) return false;
        return model.getCircuitComponents().stream().allMatch(component -> {
            if (!enabled(component) || component.getAcComponent() != null) return false;
            if (component.getType() == 1 || component.getType() == 3) return true;
            if (component.getType() == 29) {
                var saved = component.getSavedBlock();
                return HierarchyIndex.parent(saved) != 0
                        && saved.tokens().containsKey(HierarchyMetadataCodec.BOUNDARY_LOCATION);
            }
            double[] values = component.getNumericParameters();
            return (component.getType() == 4 || component.getType() == 5) && values.length > 0
                    && (values[0] == 0 || values[0] == 401);
        }) && model.getControlComponents().stream().allMatch(component ->
                (component.getType() == 1 || component.getType() == 5) && enabled(component)
                        && HierarchyIndex.parent(component.getSavedBlock()) == 0);
    }

    private static boolean enabled(CircuitModel.ComponentData component) {
        return component.getSavedBlock() == null || component.getSavedBlock().enabledState() == 1;
    }

    private PreparationResult prepareAc(CircuitSnapshot snapshot, CircuitModel model, PreparationRequest request) {
        if (!request.equals(PreparationRequest.defaults())) {
            return new PreparationResult.Rejected(new ReadinessReport(ReadinessReport.Outcome.INVALID,
                    List.of(invalidSetting("analysis", "AC does not accept transient settings or numeric overrides."))));
        }
        var diagnostics = new ArrayList<Diagnostic>();
        AcExecutionPlan plan = null;
        try {
            if (model.getHierarchy() == null) model = snapshot.executionModel();
            plan = new AcExecutionPlan(model);
        } catch (IllegalArgumentException error) {
            diagnostics.add(new Diagnostic("INVALID_AC", Diagnostic.Category.CONFIGURATION, Diagnostic.Severity.ERROR,
                    error.getMessage(), "LK", null, "analysis", model.getFilePath()));
        }
        var parserTimingIssues = model.getSavedTransientSettingIssues();
        var conversionIssues = model.getConversionIssues();
        model.getImportIssues().removeIf(issue -> parserTimingIssues.contains(issue) && !conversionIssues.contains(issue));
        CircuitReadiness.appendImportDiagnostics(model, diagnostics);
        for (var component : model.getThermalComponents()) {
            diagnostics.add(new Diagnostic("UNSUPPORTED_DOMAIN", Diagnostic.Category.CAPABILITY, Diagnostic.Severity.ERROR,
                    "AC analysis does not support THERMAL elements.", "THERMAL", component.getName(), null,
                    component.getSourceLocation()));
        }
        if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == Diagnostic.Severity.ERROR)) {
            return new PreparationResult.Inspectable(snapshot, new ReadinessReport(ReadinessReport.Outcome.INVALID, diagnostics));
        }
        return new PreparationResult.Ready(snapshot, new PreparedCircuit(snapshot, plan),
                new ReadinessReport(ReadinessReport.Outcome.READY, diagnostics));
    }
    private static String uicProblem(CircuitModel model, SolverFeatures features) {
        if (features.dcOperatingPoint() || features.periodicSteadyState() != null
                || features.adaptiveStep() != null || features.zeroCrossing()
                || model.getPreSimulationTime() > 0 || model.getSolverType() != SolverType.SOLVER_BE) {
            return "UIC requires fixed backward Euler and cannot be combined with DC or other startup features.";
        }
        if (!model.getThermalComponents().isEmpty() || !model.getSpecialComponents().isEmpty()
                || model.getControlComponents().stream().anyMatch(component -> component.getType() != 1 && component.getType() != 5)) {
            return "UIC supports only a grounded series RC circuit with voltage observation.";
        }
        var netlist = gecko.core.circuit.netlist.NetlistBuilder.buildFromCircuitModel(model);
        int voltage = -1, resistor = -1, capacitor = -1;
        if (netlist != null && netlist.getElementCount() == 3 && netlist.getNodeMax() == 2) {
            for (int i = 0; i < netlist.getElementCount(); i++) {
                switch (netlist.getType(i)) {
                    case LK_U -> voltage = i;
                    case LK_R -> resistor = i;
                    case LK_C -> capacitor = i;
                    default -> { }
                }
            }
        }
        if (voltage < 0 || resistor < 0 || capacitor < 0) {
            return "UIC requires one constant voltage source, one resistor, and one capacitor.";
        }
        var resistorValues = netlist.getParameter(resistor);
        var capacitorValues = netlist.getParameter(capacitor);
        if (resistorValues.length == 0 || capacitorValues.length < 2
                || !positive(resistorValues[0]) || !positive(capacitorValues[0])
                || !Double.isFinite(capacitorValues[1])) {
            return "UIC requires finite capacitor initial voltage and positive finite resistance and capacitance.";
        }
        var source = netlist.getParameter(voltage);
        int input = netlist.getNodeX(voltage);
        int output = netlist.getNodeX(capacitor);
        if (source.length < 2 || !Double.isFinite(source[1])
                || source[0] != gecko.core.circuit.SourceType.QUELLE_DC
                && source[0] != gecko.core.circuit.SourceType.QUELLE_DC_NEW
                || input == 0 || output == 0 || input == output
                || netlist.getNodeY(voltage) != 0 || netlist.getNodeY(capacitor) != 0
                || netlist.getNodeX(resistor) != input || netlist.getNodeY(resistor) != output) {
            return "UIC requires the signed source and capacitor terminals of a grounded series RC circuit.";
        }
        return null;
    }

    private static boolean positive(double value) { return Double.isFinite(value) && value > 0; }
    private static Diagnostic invalidSetting(String path, String message) {
        return new Diagnostic("INVALID_SETTING", Diagnostic.Category.CONFIGURATION, Diagnostic.Severity.ERROR,
                message, null, null, path, null);
    }
}

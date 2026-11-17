package gecko.core.preparation;

import gecko.core.circuit.ControlTypCore;
import gecko.core.simulation.ControlBlockDescriptor;
import gecko.core.simulation.SimulationTiming;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.matrix.StamperRegistry;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.circuit.netlist.TopologyValidationException;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.ImportIssue;
import gecko.core.simulation.ControlCalculatorFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Preflight constructs descriptions and never advances an electrical time step. */
final class CircuitReadiness {
    static List<Diagnostic> check(CircuitModel model, List<Diagnostic> configurationDiagnostics) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        appendImportDiagnostics(model, diagnostics);
        for (var component : model.getThermalComponents()) {
            if (diagnostics.stream().noneMatch(d -> d.code().equals("UNSUPPORTED_DOMAIN")
                    && "THERMAL".equals(d.domain()) && component.getName().equals(d.component()))) {
                diagnostics.add(new Diagnostic("UNSUPPORTED_DOMAIN", Diagnostic.Category.CAPABILITY, Diagnostic.Severity.ERROR,
                        "Saved thermal elements cannot run in the headless engine.", "THERMAL", component.getName(),
                        null, component.getSourceLocation()));
            }
        }
        checkComponentModes(model, diagnostics);
        CircuitNetlist topology = checkTopology(model, diagnostics);
        checkControl(model, topology, diagnostics);
        if (configurationDiagnostics.stream().noneMatch(item -> "CONTROL".equals(item.domain()) && item.severity() == Diagnostic.Severity.ERROR)
                && diagnostics.stream().noneMatch(item -> "CONTROL".equals(item.domain()) && item.severity() == Diagnostic.Severity.ERROR)) {
            try {
                new ControlCalculatorFactory().buildCircuit(model, model.getTimeStep(), topology);
            } catch (ControlCalculatorFactory.DependencyCycleException cycle) {
                diagnostics.add(new Diagnostic("CONTROL_DEPENDENCY_CYCLE", Diagnostic.Category.REFERENCES,
                        Diagnostic.Severity.ERROR, cycle.getMessage(), "CONTROL", null, null, null));
            } catch (ControlCalculatorFactory.InvalidSignalWidthException width) {
                diagnostics.add(new Diagnostic("INVALID_CONTROL_WIDTH", Diagnostic.Category.REFERENCES,
                        Diagnostic.Severity.ERROR, width.getMessage(), "CONTROL", width.component(), null, null));
            }
        }
        appendTimingDiagnostics(model, diagnostics);
        return diagnostics;
    }

    static void appendImportDiagnostics(CircuitModel model, List<Diagnostic> diagnostics) {
        for (var issue : model.getImportIssues()) {
            appendUnique(diagnostics, new Diagnostic(issue.code(), issue.unsupported() ? Diagnostic.Category.CAPABILITY
                    : configurationIssue(issue) ? Diagnostic.Category.CONFIGURATION : Diagnostic.Category.PARSING,
                    Diagnostic.Severity.ERROR, issue.message(), issue.domain(), issue.component(),
                    issue.parameterPath(), issue.sourceLocation()));
        }
    }

    static void appendTimingDiagnostics(CircuitModel model, List<Diagnostic> diagnostics) {
        var settings = new SolverSettingsCore(model.getSolverType(), model.getTimeStep(), model.getSimulationDuration())
                .withPreCalculation(model.getPreSimulationTime(), model.getPreSimulationTimeStep());
        for (var issue : SimulationTiming.settingsIssues(settings)) {
            appendUnique(diagnostics, setting(issue.parameterPath(), issue.message()));
        }
    }

    private static void appendUnique(List<Diagnostic> diagnostics, Diagnostic diagnostic) {
        if (!diagnostics.contains(diagnostic)) diagnostics.add(diagnostic);
    }

    private static boolean configurationIssue(ImportIssue issue) {
        return issue.code().equals("INVALID_SETTING") || CircuitConfiguration.isNormalizationIssue(issue);
    }
    private static void checkComponentModes(CircuitModel model, List<Diagnostic> diagnostics) {
        for (var component : model.getCircuitComponents()) {
            int type = component.getType();
            if (type == 8 && savedValue(component, "reverserecoverydelay", 9) != 0) {
                unsupportedField(component, "reverserecoverydelay", diagnostics);
            }
            if (type == 10 && savedValue(component, "satCurEnabled", 10) > 0.5) {
                unsupportedField(component, "satCurEnabled", diagnostics);
            }
            if (Set.of(6, 7, 8, 10, 28).contains(type) && savedValue(component, "numberParalleled", 12) > 1) {
                unsupportedField(component, "numberParalleled", diagnostics);
            }
            for (String token : List.of("nonlinear", "isNonlinear", "useNonlinearChar")) {
                if (savedValue(component, token, -1) > 0.5) unsupportedField(component, token, diagnostics);
            }
        }
    }

    private static double savedValue(CircuitModel.ComponentData component, String field, int slot) {
        Object named = component.getImportTokens().containsKey(field) ? component.getImportTokens().get(field)
                : component.getParameters().get(field);
        if (named instanceof Boolean flag) return flag ? 1 : 0;
        if (named instanceof Number number) return number.doubleValue();
        if (named instanceof String text) {
            if (text.equalsIgnoreCase("true")) return 1;
            if (text.equalsIgnoreCase("false")) return 0;
            try { return Double.parseDouble(text); }
            catch (NumberFormatException error) { return Double.NaN; }
        }
        double[] values = component.getNumericParameters();
        return slot >= 0 && slot < values.length ? values[slot] : 0;
    }

    private static void unsupportedField(CircuitModel.ComponentData component, String field, List<Diagnostic> diagnostics) {
        diagnostics.add(new Diagnostic("UNSUPPORTED_PARAMETER", Diagnostic.Category.CAPABILITY, Diagnostic.Severity.ERROR,
                "The configured field is unsupported by the headless engine.", "LK", component.getName(),
                component.getName() + "." + field, component.getSourceLocation()));
    }
    private static CircuitNetlist checkTopology(CircuitModel model, List<Diagnostic> diagnostics) {
        var registry = StamperRegistry.createDefault();
        var topology = new CircuitSnapshot(model).toModel();
        topology.getCircuitComponents().clear();
        List<CircuitModel.ComponentData> unknown = new ArrayList<>();
        for (var component : model.getCircuitComponents()) {
            if (component.getType() == 9 || (CircuitTypCore.isValidTypeNumber(component.getType())
                    && registry.hasStamper(CircuitTypCore.fromTypeNumber(component.getType())))) {
                topology.addCircuitComponent(component);
            } else {
                unknown.add(component);
                diagnostics.add(new Diagnostic("UNSUPPORTED_COMPONENT", Diagnostic.Category.CAPABILITY, Diagnostic.Severity.ERROR,
                        "The headless engine does not support this component type.", "LK", component.getName(), null,
                        component.getSourceLocation()));
            }
        }
        if (model.getImportIssues().stream().anyMatch(issue -> "LK".equals(issue.domain()) && !configurationIssue(issue))) return null;
        if (!unknown.isEmpty()) {
            if (model.getConnections().stream().anyMatch(wire -> "LK".equals(wire.getType()))) return null;
            Set<String> affected = new HashSet<>();
            unknown.forEach(component -> affected.addAll(nonGroundLabels(component)));
            boolean changed;
            do {
                changed = false;
                for (var component : topology.getCircuitComponents()) {
                    Set<String> labels = nonGroundLabels(component);
                    if (labels.stream().anyMatch(affected::contains)) changed |= affected.addAll(labels);
                }
            } while (changed);
            topology.getCircuitComponents().removeIf(component -> nonGroundLabels(component).stream().anyMatch(affected::contains)
                    || (component.getType() == 9 && !affected.isEmpty()));
        }
        try {
            CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(topology);
            return unknown.isEmpty() ? netlist : null;
        } catch (TopologyValidationException error) {
            diagnostics.add(new Diagnostic(error.code(), Diagnostic.Category.TOPOLOGY, Diagnostic.Severity.ERROR,
                    error.getMessage(), "LK", error.component(), null, model.getFilePath()));
        } catch (IllegalArgumentException error) {
            diagnostics.add(new Diagnostic("INVALID_TOPOLOGY", Diagnostic.Category.TOPOLOGY, Diagnostic.Severity.ERROR,
                    error.getMessage(), "LK", null, null, model.getFilePath()));
        }
        return null;
    }

    private static Set<String> nonGroundLabels(CircuitModel.ComponentData component) {
        Set<String> labels = new HashSet<>();
        for (String[] terminal : List.of(component.getTerminalXLabels(), component.getTerminalYLabels())) {
            for (String label : terminal) {
                if (label != null && !label.isBlank() && !label.trim().equals("0") && !label.trim().equalsIgnoreCase("gnd")) {
                    labels.add(label.trim());
                }
            }
        }
        return labels;
    }

    private static Integer resolveElectricalTerminalNode(CircuitModel model, CircuitModel.ComponentData measurement,
            String label, CircuitNetlist topology) {
        long sheet = measurement.getSavedBlock() == null ? 0 : measurement.getSavedBlock().parentSheetIdentifier();
        for (var component : model.getCircuitComponents()) {
            long componentSheet = component.getSavedBlock() == null ? 0 : component.getSavedBlock().parentSheetIdentifier();
            if (componentSheet != sheet) continue;
            int element = topology == null ? -1 : topology.getElementIndexByName(component.getName());
            if (topology != null && element < 0) continue;
            for (String terminalLabel : component.getTerminalXLabels()) {
                if (java.util.Objects.equals(terminalLabel, label)) {
                    return topology == null ? 0 : topology.getNodeX(element);
                }
            }
            for (String terminalLabel : component.getTerminalYLabels()) {
                if (java.util.Objects.equals(terminalLabel, label)) {
                    return topology == null ? 0 : topology.getNodeY(element);
                }
            }
        }
        return null;
    }

    private static void checkControl(CircuitModel model, CircuitNetlist topology, List<Diagnostic> diagnostics) {
        CircuitTopology controlTopology = CircuitTopology.resolve(model);
        for (var component : model.getControlComponents()) {
            int type = component.getType();
            var descriptor = ControlBlockDescriptor.find(type);
            if (!ControlTypCore.isKnown(type) && descriptor == null) {
                diagnostics.add(control("UNSUPPORTED_CONTROL_COMPONENT", Diagnostic.Category.CAPABILITY, component,
                        "The headless engine does not support this CONTROL type."));
            } else {
                int required = type == 4 ? 6 : type == 3 || type == 7 ? 1 : 0;
                if (component.isParsedInput() && component.getImportedParameterCount() < required) {
                    diagnostics.add(control("INVALID_CONTROL_LAYOUT", Diagnostic.Category.PARSING, component,
                            "The saved CONTROL layout has too few numeric parameters."));
                }
                if (type == 4 && component.getNumericParameters().length > 0) {
                    double subtype = component.getNumericParameters()[0];
                    if (Double.isFinite(subtype) && subtype == Math.rint(subtype)
                            && subtype != 400 && subtype != 401 && subtype != 404 && subtype != 405 && subtype != 406
                            && subtype != 0 && subtype != 1 && subtype != 2 && subtype != 3 && subtype != 4) {
                        diagnostics.add(control("UNSUPPORTED_CONTROL_SUBTYPE", Diagnostic.Category.CAPABILITY, component,
                                "The headless engine does not support this SIGNAL subtype."));
                    }
                }
                int outputs = descriptor != null ? descriptor.outputs(component) : type == 5 || type == 6 ? 0 : 1;
                int inputs = descriptor != null ? descriptor.inputs(component) : type == 6 || type == 7 ? 1 : 0;
                if (hasUsedPortAfter(component.getTerminalYLabels(), outputs)
                        || (type != 5 && hasUsedPortAfter(component.getTerminalXLabels(), inputs))) {
                    diagnostics.add(control("INVALID_CONTROL_PORT", Diagnostic.Category.REFERENCES, component,
                            "The CONTROL block uses a port that its calculator does not provide."));
                }
            }
        }
        Set<String> gateTargets = new HashSet<>();
        for (var component : model.getControlComponents()) {
            int type = component.getType();
            var descriptor = ControlBlockDescriptor.find(type);
            if (!ControlTypCore.isKnown(type) && descriptor == null) continue;
            String[] inputs = component.getTerminalXLabels();
            int required = descriptor != null ? descriptor.inputs(component)
                    : type == 6 || type == 7 ? 1 : type == 5 ? inputs.length : 0;
            int activeInputs = required;
            if (type == 22 && component.getParameters().get("activeInputWidth") instanceof Number width) {
                activeInputs = Math.min(required, Math.max(0, width.intValue()));
            }
            for (int port = 0; port < required; port++) {
                if (type == 22 && port >= activeInputs) continue;
                String label = port < inputs.length ? inputs[port] : "";
                var group = controlTopology.group(component, CircuitTopology.Side.INPUT, port);
                int producerCount = group == null ? 0 : group.producerTerminals().size();
                if (type == 5 && (label == null || label.isBlank()) && producerCount == 0) continue;
                if (producerCount == 0) {
                    diagnostics.add(new Diagnostic("MISSING_CONTROL_INPUT", Diagnostic.Category.REFERENCES,
                            Diagnostic.Severity.WARNING, "An unconnected CONTROL input uses zero.", "CONTROL",
                            component.getName(), component.getName() + ".input[" + port + "]", component.getSourceLocation()));
                } else if (producerCount > 1 && !sharedMeasurementSignal(group, model, controlTopology)) {
                    diagnostics.add(control("AMBIGUOUS_CONTROL_SIGNAL", Diagnostic.Category.REFERENCES, component,
                            "The CONTROL input group identifies more than one signal producer."));
                }
            }
            if (type == 1) {
                String[] references = component.getParameterStrings();
                for (int port = 0; port < 2; port++) {
                    String reference = port < references.length ? references[port] : "";
                    Integer resolvedNode = reference == null || reference.isBlank() ? null
                            : resolveElectricalTerminalNode(model, component, reference, topology);
                    if (resolvedNode == null || topology != null
                            && (resolvedNode < 0 || resolvedNode > topology.getNodeMax())) {
                        diagnostics.add(control("INVALID_PROBE_REFERENCE", Diagnostic.Category.REFERENCES, component,
                                "A voltage probe label must identify a valid electrical terminal in its saved sheet."));
                    }
                }
            } else if (type == 2 || type == 6) {
                checkElectricalReference(model, component, type == 6, diagnostics);
                String[] references = component.getParameterStrings();
                if (type == 6 && references.length > 0 && references[0] != null && !references[0].isBlank()
                        && !gateTargets.add(references[0])) {
                    diagnostics.add(control("AMBIGUOUS_GATE_REFERENCE", Diagnostic.Category.REFERENCES, component,
                            "More than one gate controls the same electrical component."));
                }
            }
        }
    }

    private static boolean sharedMeasurementSignal(CircuitTopology.Group group, CircuitModel model,
            CircuitTopology topology) {
        return group.producerTerminals().stream().allMatch(terminal -> model.getControlComponents().stream()
                .filter(candidate -> topology.identity(candidate).equals(terminal.component()))
                .anyMatch(candidate -> candidate.getType() == 1 || candidate.getType() == 2));
    }

    private static boolean hasUsedPortAfter(String[] labels, int count) {
        for (int port = count; port < labels.length; port++) {
            if (labels[port] != null && !labels[port].isBlank()) return true;
        }
        return false;
    }

    private static void checkElectricalReference(CircuitModel model, CircuitModel.ComponentData component,
                                                  boolean gate, List<Diagnostic> diagnostics) {
        String[] references = component.getParameterStrings();
        String reference = references.length > 0 ? references[0] : "";
        var matches = model.getCircuitComponents().stream().filter(candidate -> candidate.getName().equals(reference)).toList();
        if (reference == null || reference.isBlank() || matches.isEmpty()) {
            diagnostics.add(control("MISSING_REFERENCE", Diagnostic.Category.REFERENCES, component,
                    "The CONTROL block requires an existing electrical component reference."));
        } else if (matches.size() > 1) {
            diagnostics.add(control("AMBIGUOUS_REFERENCE", Diagnostic.Category.REFERENCES, component,
                    "The electrical component reference identifies more than one component."));
        } else if (gate && !Set.of(7, 8, 10, 28).contains(matches.getFirst().getType())
                && CircuitTypCore.isValidTypeNumber(matches.getFirst().getType())
                && StamperRegistry.createDefault().hasStamper(CircuitTypCore.fromTypeNumber(matches.getFirst().getType()))) {
            diagnostics.add(control("INVALID_GATE_REFERENCE", Diagnostic.Category.REFERENCES, component,
                    "A gate reference must identify a supported switch."));
        } else if (!gate && matches.getFirst().getType() == 9) {
            diagnostics.add(control("INVALID_PROBE_REFERENCE", Diagnostic.Category.REFERENCES, component,
                    "A current probe cannot reference magnetic coupling metadata."));
        }
    }

    private static Diagnostic control(String code, Diagnostic.Category category, CircuitModel.ComponentData component,
                                      String message) {
        return new Diagnostic(code, category, Diagnostic.Severity.ERROR, message, "CONTROL", component.getName(),
                null, component.getSourceLocation());
    }
    private static Diagnostic setting(String path, String message) {
        return new Diagnostic("INVALID_SETTING", Diagnostic.Category.CONFIGURATION, Diagnostic.Severity.ERROR,
                message, null, null, path, null);
    }
}

package gecko.core.io.importer;

import gecko.core.ac.AcComponent;
import gecko.core.ac.AcInputOrigin;
import gecko.core.ac.AcDocumentSettings;
import gecko.core.ac.AcSweep;
import gecko.core.ac.ParameterDefinition;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.CircuitModel.ConnectionData;
import gecko.core.io.CircuitResource;
import gecko.core.io.ImportIssue;
import gecko.core.io.SpicePorts;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;
import java.util.regex.Pattern;

/** Imports the documented SPICE element subset and retains unsupported source evidence. */
public final class SpiceImporter {
    record Line(int number, String text) { }
    private static final Pattern ASSIGNMENT = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)\\s*=\\s*(\\{[^}]*}|[^\\s,=]+)");

    public CircuitModel convert(String source, String sourceName) {
        return convert(sourceName, source.hashCode(), lines(source), new LinkedHashMap<>(), Map.of(),
                AcInputOrigin.PORTABLE_CIR, null).model();
    }

    record SheetResult(CircuitModel model, List<ParameterDefinition> parameters, Map<String, String> scope) { }
    record ChildContext(boolean ac, Function<List<ParameterDefinition>, Map<String, String>> bindParameters) { }

    SheetResult convert(String sourceName, int sourceId, List<Line> statements, Map<String, String> nodes,
                        Map<String, ComponentData> placements, AcInputOrigin origin, ChildContext child) {
        var model = new CircuitModel();
        model.setFilePath(sourceName);
        model.setFileVersion(202);
        model.setUniqueFileId(sourceId);
        model.setSimulationDuration(0.01);
        model.setTimeStep(1e-6);
        var parameters = new HashMap<String, String>();
        var orderedParameters = new ArrayList<ParameterDefinition>();
        var definitions = new HashMap<String, String>();
        List<Line> lines = rootLines(statements, model);
        boolean acRequested = lines.stream().anyMatch(line -> keyword(line.text()).equals(".ac"));
        for (var line : lines) {
            String directive = keyword(line.text());
            if (directive.equals(".param")) {
                if (!assignments(line.text().substring(6), parameters)) {
                    issue(model, line, "MALFORMED_PARAMETER", "", "The parameter assignment is invalid.");
                }
                var assignment = ASSIGNMENT.matcher(line.text().substring(6));
                while (assignment.find()) {
                    orderedParameters.add(new ParameterDefinition(assignment.group(1), assignment.group(2)));
                }
            }
            if (directive.equals(".model")) {
                if (origin.tested()) issue(model, line, "UNSUPPORTED_ASC_PROFILE", directive,
                        "The selected ASC profile requires linear components without models.");
                String[] fields = line.text().split("\\s+", 3);
                if (fields.length == 3) definitions.put(fields[1].toLowerCase(Locale.ROOT), fields[2]);
                else issue(model, line, "MALFORMED_MODEL", "", "The model definition is incomplete.");
            }
        }
        for (var line : lines) {
            if (keyword(line.text()).equals(".ac")) {
                try {
                    String[] args = arguments(line.text().substring(3));
                    if (model.getAcSettings() != null || args.length != 4 || !args[0].equalsIgnoreCase("dec")) {
                        throw new IllegalArgumentException("Require one .ac dec analysis with density, start, and stop.");
                    }
                    model.setAcSettings(new AcDocumentSettings(
                            new AcSweep(Integer.parseInt(args[1]), value(args[2], parameters), value(args[3], parameters)),
                            orderedParameters, line.text()));
                } catch (IllegalArgumentException error) {
                    issue(model, line, "INVALID_AC", ".ac", error.getMessage());
                }
                continue;
            }
            if (!keyword(line.text()).equals(".tran")) continue;
            if (acRequested) issue(model, line, "MULTIPLE_ANALYSES", ".tran", "Select one analysis.");
            try {
                String[] args = arguments(line.text().substring(5));
                if (args.length < 2 || args.length > 5) {
                    throw new IllegalArgumentException("The transient directive requires a step and duration.");
                }
                double step = value(args[0], parameters), end = value(args[1], parameters);
                if (args.length > 2 && !args[2].equalsIgnoreCase("uic") && value(args[2], parameters) != 0) {
                    issue(model, line, "UNSUPPORTED_TRANSIENT_OPTION", ".tran", "The transient output start time is unsupported.");
                }
                if (args.length > 3 && !args[3].equalsIgnoreCase("uic")) step = value(args[3], parameters);
                if (args.length > 4 && !args[4].equalsIgnoreCase("uic")) {
                    issue(model, line, "UNSUPPORTED_TRANSIENT_OPTION", ".tran", "The trailing transient option is unsupported: " + args[4]);
                }
                if (Arrays.stream(args).anyMatch(argument -> argument.equalsIgnoreCase("uic"))) {
                    issue(model, line, "UNSUPPORTED_TRANSIENT_OPTION", ".tran", "The UIC transient option is unsupported.");
                }
                if (step == 0) step = end / 10000;
                if (step <= 0 || end <= step) throw new IllegalArgumentException("Require 0 < time step < duration.");
                model.setTimeStep(step);
                model.setSimulationDuration(end);
            } catch (IllegalArgumentException | IndexOutOfBoundsException error) {
                issue(model, line, "INVALID_TRANSIENT", "", error.getMessage());
            }
        }
        boolean ac = child == null ? model.getAcSettings() != null : child.ac();
        if (child != null && !ac) {
            parameters.clear();
            parameters.putAll(child.bindParameters().apply(List.copyOf(orderedParameters)));
        } else if (child == null && !ac) {
            // Scalar lookup uses the last definition of each name in a non-AC document.
            var effective = new LinkedHashMap<String, ParameterDefinition>();
            for (var definition : orderedParameters) {
                effective.put(definition.name().toLowerCase(Locale.ROOT), definition);
            }
            model.setRootParameterDefinitions(List.copyOf(effective.values()));
        }
        nodes.putIfAbsent("0", "0");
        var names = new HashSet<String>();
        for (var line : lines) {
            String text = line.text();
            String directive = keyword(text);
            if (List.of(".param", ".model", ".tran", ".ac").contains(directive)) continue;
            if (ac && !origin.tested() && text.strip().equalsIgnoreCase(".options logparams")) continue;
            if (text.startsWith(".")) {
                issue(model, line, "UNSUPPORTED_DIRECTIVE", directive, "The analysis or directive is unsupported.");
                continue;
            }
            String[] fields = text.split("\\s+", 4);
            ComponentData placement = placements.get(fields[0]);
            if (placement == null && line.number() == 1 && !firstElement(fields, parameters, definitions)) continue;
            int controlCount = model.getControlComponents().size();
            var resourceIds = new HashSet<>(model.getResources().keySet());
            try {
                String name = fields[0];
                if (name.contains("/") || !names.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Require a unique component name without slashes.");
                SpicePorts descriptor = placement == null ? SpicePorts.find(Character.toUpperCase(name.charAt(0)))
                        : SpicePorts.find("LK", placement.getType());
                if (descriptor == null || descriptor == SpicePorts.E && !ac) {
                    issue(model, line, "UNSUPPORTED_ELEMENT", name, "Unsupported SPICE element: " + name);
                    continue;
                }
                char kind = descriptor.designator();
                if (origin.tested() && "RLCVIE".indexOf(kind) < 0) {
                    throw new IllegalArgumentException("The selected ASC profile requires a supported linear component.");
                }
                String positive = node(nodes, fields[1]), negative = node(nodes, fields[2]);
                int type = descriptor.type();
                int index = model.getCircuitComponents().size();
                ComponentData data = placement == null
                        ? component(type, name, 8 + index % 6 * 10, 6 + index / 6 * 14, positive, negative) : placement;
                if (placement != null) initializeTerminals(data, positive, negative);
                data.getImportTokens().put("uniqueObjectIdentifier", Long.toString(10000L + index));
                List<String> aliases = List.of(positive, negative);
                AcComponent.Law law = null;
                var scalarBindings = new HashMap<Integer, String>();
                switch (kind) {
                    case 'R', 'L', 'C' -> {
                        String[] args = arguments(fields[3]);
                        double v = value(args[0], parameters);
                        if (v <= 0) throw new IllegalArgumentException("Require a positive passive component value.");
                        double[] values = new double[type == 1 ? 3 : type == 2 ? 5 : 11];
                        values[0] = v;
                        if (!ac) bindIfScoped(scalarBindings, 0, args[0]);
                        if (ac && kind == 'L') {
                            law = acInductance(fields[3], args, origin);
                        } else if (args.length > 1) {
                            if (args.length > 2 || kind == 'R' || !args[1].toLowerCase(Locale.ROOT).startsWith("ic=")) {
                                throw new IllegalArgumentException("Unsupported passive component attributes.");
                            }
                            values[1] = value(args[1].substring(3), parameters);
                            if (!ac) bindIfScoped(scalarBindings, 1, args[1].substring(3));
                        }
                        data.setNumericParameters(values);
                        if (ac && (kind == 'R' || kind == 'C')) law = new AcComponent.Passive(args[0]);
                    }
                    case 'V', 'I' -> {
                        if (ac) {
                            var voltage = acVoltage(fields[3]);
                            source(data, voltage.dc(), model, parameters);
                            if (kind == 'I' && !voltage.waveform().isEmpty()) {
                                throw new IllegalArgumentException("Unsupported transient AC current source waveform.");
                            }
                            law = kind == 'V' ? voltage : new AcComponent.Current(voltage.dc(),
                                    voltage.magnitude(), voltage.phaseDegrees());
                        } else {
                            String spec = fields[3].strip().toUpperCase(Locale.ROOT);
                            if (child != null && (spec.startsWith("PULSE") || spec.startsWith("SIN"))) {
                                issue(model, line, "UNSUPPORTED_HIERARCHY_CONTROL", name,
                                        "Child waveform CONTROL components and resources are unsupported.");
                                continue;
                            }
                            source(data, fields[3], model, parameters);
                            if (data.getNumericParameters()[0] == 401) bindIfScoped(scalarBindings, 1, dcValue(fields[3]));
                        }
                    }
                    case 'E' -> {
                        String[] tail = fields[3].split("\\s+", 3);
                        if (tail.length != 3) throw new IllegalArgumentException("Require two sensing nodes and a gain.");
                        String sensePositive = node(nodes, tail[0]), senseNegative = node(nodes, tail[1]);
                        aliases = List.of(positive, negative, sensePositive, senseNegative);
                        assignPorts(data, descriptor, aliases);
                        String expression = tail[2].strip();
                        boolean laplace = expression.matches("(?is)^laplace\\s*=.*");
                        if (laplace) expression = expression.substring(expression.indexOf('=') + 1).strip();
                        law = new AcComponent.Controlled(expression, laplace);
                        data.setNumericParameters(new double[]{0});
                    }
                    case 'D' -> {
                        data.setNumericParameters(new double[]{1e12, 0, 1e-6, 1e12, 0, 0, 0, 0, 0, 0, 0, 0, 1});
                        String definition = definitions.get(fields[3].trim().toLowerCase(Locale.ROOT));
                        if (definition == null || !modelType(definition).equals("d")) {
                            issue(model, line, "DIODE_MODEL_APPROXIMATION", name, "The diode uses the native ideal piecewise-linear model.");
                        } else {
                            var args = new HashMap<String, String>();
                            if (!assignments(modelArguments(definition), args)) throw new IllegalArgumentException("The diode model parameters are invalid.");
                            if (args.containsKey("ron")) data.getNumericParameters()[2] = value(args.get("ron"), parameters);
                            if (args.containsKey("roff")) data.getNumericParameters()[3] = value(args.get("roff"), parameters);
                            if (args.containsKey("vfwd")) data.getNumericParameters()[1] = value(args.get("vfwd"), parameters);
                            if (args.isEmpty() || args.keySet().stream().anyMatch(k -> !List.of("ron", "roff", "vfwd").contains(k))) {
                                issue(model, line, "DIODE_MODEL_APPROXIMATION", name, "The diode model uses unsupported exponential behavior.");
                            }
                        }
                    }
                    case 'S' -> voltageSwitch(data, fields[3], model, parameters, definitions, nodes);
                    default -> throw new IllegalArgumentException("Unsupported element.");
                }
                if (!scalarBindings.isEmpty()) data.setScalarBindings(scalarBindings);
                data.setSourceLocation(model.getFilePath() + ":" + line.number());
                if (law != null) {
                    data.setAcComponent(new AcComponent(name, descriptor.ports().stream()
                            .map(SpicePorts.PortSpec::order).toList(), aliases, false, law));
                    if (placement != null) {
                        assignPorts(data, descriptor, aliases.stream().map(alias -> "").toList());
                    }
                }
                model.addCircuitComponent(data);
                if (placement == null) {
                    if (ac && law != null) acTerminalWires(model, data, descriptor, aliases);
                    else terminalWires(model, data);
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException error) {
                model.getControlComponents().subList(controlCount, model.getControlComponents().size()).clear();
                model.getResources().keySet().retainAll(resourceIds);
                issue(model, line, "MALFORMED_ELEMENT", fields[0], error.getMessage());
            }
        }
        if (!ac && child == null) addScope(model, nodes);
        int circuitRows = (model.getCircuitComponents().size() + 5) / 6;
        for (int index = 0; index < model.getControlComponents().size(); index++) {
            model.getControlComponents().get(index).getPosition()[1] = 5 + circuitRows * 14 + index / 6 * 12;
        }
        return new SheetResult(model, List.copyOf(orderedParameters), Map.copyOf(parameters));
    }

    private static AcComponent.Inductance acInductance(String value, String[] args, AcInputOrigin origin) {
        if (origin.tested() && args.length == 1) {
            return new AcComponent.Inductance(args[0], new AcComponent.ImportedDefault(), origin);
        }
        String attributes = value.strip().substring(args[0].length()).strip();
        var match = Pattern.compile("(?is)^rser\\s*=\\s*(.+)$").matcher(attributes);
        if (!match.matches() || arguments(match.group(1)).length != 1 || match.group(1).contains("=")) {
            throw new IllegalArgumentException("Require one explicit Rser expression and no other inductor attributes.");
        }
        return new AcComponent.Inductance(args[0], new AcComponent.ExplicitResistance(match.group(1)), origin);
    }

    private static void assignPorts(ComponentData component, SpicePorts descriptor, List<String> labels) {
        String[] inputs = new String[(int) descriptor.nativePorts().stream()
                .filter(port -> port.side() == gecko.core.io.CircuitTopology.Side.INPUT).count()];
        String[] outputs = new String[descriptor.nativePorts().size() - inputs.length];
        for (var port : descriptor.nativePorts()) {
            var target = port.side() == gecko.core.io.CircuitTopology.Side.INPUT ? inputs : outputs;
            target[port.port()] = labels.get(port.order() - 1);
        }
        component.setTerminalXLabels(inputs);
        component.setTerminalYLabels(outputs);
    }

    private static void acTerminalWires(CircuitModel model, ComponentData component, SpicePorts descriptor, List<String> labels) {
        for (var port : descriptor.nativePorts()) {
            var offset = port.nativeOffset(component.getOrientation(), false);
            int x = component.getPosition()[0] + offset.x(), y = component.getPosition()[1] + offset.y();
            int dx = Integer.signum(offset.x()), dy = dx == 0 ? Integer.signum(offset.y()) : 0;
            model.addConnection(new ConnectionData("LK", new int[][]{{x, y}, {x + dx, y + dy}}, labels.get(port.order() - 1)));
        }
    }

    private static AcComponent.Voltage acVoltage(String spec) {
        String[] fields = arguments(spec);
        String dc = null, waveform = "", magnitude = null, phase = "0";
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (field.equals("\"\"")) continue;
            if (field.equalsIgnoreCase("AC")) {
                if (magnitude != null) throw new IllegalArgumentException("Use one AC source specification.");
                if (++i >= fields.length) throw new IllegalArgumentException("Require an AC magnitude.");
                magnitude = fields[i];
                if (i + 1 < fields.length && !fields[i + 1].equalsIgnoreCase("DC")
                        && !waveform(fields[i + 1])) phase = fields[++i];
            } else if (field.equalsIgnoreCase("DC")) {
                if (dc != null) throw new IllegalArgumentException("Use one DC source specification.");
                if (++i >= fields.length) throw new IllegalArgumentException("Require a DC value.");
                dc = fields[i];
            } else if (waveform(field)) {
                if (!waveform.isEmpty()) throw new IllegalArgumentException("Use one transient source waveform.");
                if (!field.contains("(") && i + 1 < fields.length && fields[i + 1].startsWith("(")) field += fields[++i];
                waveform = field;
            } else {
                if (dc != null) throw new IllegalArgumentException("Use one DC source value.");
                dc = field;
            }
        }
        return new AcComponent.Voltage(dc == null ? "0" : dc, waveform, magnitude == null ? "0" : magnitude, phase);
    }

    static List<ParameterDefinition> parameterAssignments(String text) {
        var assignments = new ArrayList<ParameterDefinition>();
        var matcher = ASSIGNMENT.matcher(text);
        int offset = 0;
        while (matcher.find()) {
            if (!text.substring(offset, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Require scalar parameter assignments.");
            }
            assignments.add(new ParameterDefinition(matcher.group(1), matcher.group(2)));
            offset = matcher.end();
        }
        if (!text.substring(offset).isBlank()) throw new IllegalArgumentException("Require scalar parameter assignments.");
        return List.copyOf(assignments);
    }

    private static boolean waveform(String token) {
        return token.matches("(?is)^(pulse|sine?)(\\(.*)?");
    }

    static void addScope(CircuitModel model, Map<String, String> nodes) {
        var used = new HashSet<String>();
        for (var data : model.getCircuitComponents()) {
            used.addAll(List.of(data.getTerminalXLabels()));
            used.addAll(List.of(data.getTerminalYLabels()));
        }
        var signals = new ArrayList<String>();
        for (var entry : nodes.entrySet()) {
            if (entry.getValue().equals("0") || !used.contains(entry.getValue())) continue;
            var probe = control(model, 1, "VOLT." + entry.getValue(), new double[]{0}, new String[0]);
            probe.setParameterStrings(new String[]{entry.getValue(), "0", "0"});
            probe.setTerminalYLabels(new String[]{"V_" + entry.getValue()});
            signals.add(probe.getTerminalYLabels()[0]);
        }
        for (int start = 0; start < signals.size(); start += 8) {
            var ports = signals.subList(start, Math.min(start + 8, signals.size()));
            String name = start == 0 ? "SCOPE.IMPORT" : "SCOPE.IMPORT." + (1 + start / 8);
            var scope = control(model, 5, name, new double[0], ports.toArray(String[]::new));
            scope.setTerminalYLabels(new String[0]);
            scope.getImportTokens().put("tn", Integer.toString(ports.size()));
        }
    }

    static ComponentData component(int type, String name, int x, int y, String positive, String negative) {
        var data = new ComponentData(type, name, x, y, 503);
        initializeTerminals(data, positive, negative);
        return data;
    }

    private static void initializeTerminals(ComponentData data, String positive, String negative) {
        data.setTerminalXLabels(new String[]{positive});
        data.setTerminalYLabels(new String[]{negative});
        data.setParameterStrings(new String[]{"", "", "0"});
    }

    static void terminalWires(CircuitModel model, ComponentData data) {
        int x = data.getPosition()[0], y = data.getPosition()[1];
        model.addConnection(new ConnectionData("LK", new int[][]{{x, y - 2}, {x - 2, y - 2}}, data.getTerminalXLabels()[0]));
        model.addConnection(new ConnectionData("LK", new int[][]{{x, y + 2}, {x + 2, y + 2}}, data.getTerminalYLabels()[0]));
    }

    static void source(ComponentData data, String spec, CircuitModel model, Map<String, String> parameters) {
        String upper = spec.toUpperCase(Locale.ROOT).trim();
        double[] saved = new double[21];
        saved[0] = 401;
        if (upper.startsWith("PULSE")) {
            double[] args = Arrays.stream(arguments(unwrap(spec.substring(5)))).mapToDouble(s -> value(s, parameters)).toArray();
            if (args.length < 2 || args.length > 7) throw new IllegalArgumentException("PULSE requires two to seven arguments.");
            double low = args[0], high = args[1], delay = args.length > 2 ? args[2] : 0;
            double rise = args.length > 3 && args[3] != 0 ? args[3] : model.getTimeStep();
            double fall = args.length > 4 && args[4] != 0 ? args[4] : model.getTimeStep();
            double width = args.length > 5 ? args[5] : model.getSimulationDuration();
            double period = args.length > 6 ? args[6] : model.getSimulationDuration() + delay + rise + width + fall;
            if (delay < 0 || rise <= 0 || fall <= 0 || width < 0 || period <= 0 || rise + width + fall > period)
                throw new IllegalArgumentException("The PULSE timing is invalid.");
            var samples = new TreeMap<Double, Double>();
            samples.put(0.0, low);
            double cycles = Math.ceil(model.getSimulationDuration() / period) + 1;
            if (cycles > 1000000) throw new IllegalArgumentException("The PULSE table exceeds one million cycles.");
            for (int i = 0; i < cycles; i++) {
                double start = delay + i * period;
                samples.put(start, low); samples.put(start + rise, high);
                samples.put(start + rise + width, high); samples.put(start + rise + width + fall, low);
            }
            importedWaveform(model, data, samples);
            saved[0] = 400;
        } else if (upper.startsWith("SIN")) {
            int keywordLength = upper.startsWith("SINE") ? 4 : 3;
            double[] args = Arrays.stream(arguments(unwrap(spec.substring(keywordLength)))).mapToDouble(s -> value(s, parameters)).toArray();
            if (args.length < 3 || args.length > 6) throw new IllegalArgumentException("SIN requires three to six arguments.");
            double delay = args.length > 3 ? args[3] : 0, damping = args.length > 4 ? args[4] : 0;
            double phase = args.length > 5 ? args[5] * Math.PI / 180 : 0;
            if (args[2] < 0 || delay < 0 || damping < 0) throw new IllegalArgumentException("The SIN timing is invalid.");
            importedWaveform(model, data, sampleWaveform(model, time -> {
                double elapsed = Math.max(0, time - delay);
                return args[0] + args[1] * Math.exp(-damping * elapsed) * Math.sin(2 * Math.PI * args[2] * elapsed + phase);
            }));
            saved[0] = 400;
        } else {
            saved[1] = value(dcValue(spec), parameters);
        }
        sourceSettings(data, saved);
    }

    private static String dcValue(String spec) {
        return keyword(spec).equals("dc") ? spec.substring(2).trim() : spec.trim();
    }

    private static void bindIfScoped(Map<Integer, String> bindings, int slot, String expression) {
        try {
            SpiceValue.evaluate(expression, Map.of());
        } catch (IllegalArgumentException error) {
            // The expression has already evaluated in the occurrence scope.
            bindings.put(slot, expression);
        }
    }

    static void controlledSource(ComponentData data, String signal) {
        double[] saved = new double[21];
        saved[0] = 400;
        sourceSettings(data, saved);
        data.setParameterStrings(new String[]{signal, "", "0"});
    }

    private static void sourceSettings(ComponentData data, double[] saved) {
        data.setNumericParameters(saved);
        data.getImportTokens().put("sourceType", Integer.toString((int) saved[0]));
        data.getImportTokens().put("amplitude", "0.0");
        data.getImportTokens().put("phaseDegrees", "0.0");
    }

    static TreeMap<Double, Double> sampleWaveform(CircuitModel model, DoubleUnaryOperator valueAt) {
        var samples = new TreeMap<Double, Double>();
        double steps = Math.ceil(model.getSimulationDuration() / model.getTimeStep()) + 2;
        if (steps > 2000000) throw new IllegalArgumentException("The waveform table exceeds two million samples.");
        for (int i = 0; i < steps; i++) {
            double time = i * model.getTimeStep();
            samples.put(time, valueAt.applyAsDouble(time));
        }
        return samples;
    }

    private static void importedWaveform(CircuitModel model, ComponentData target, TreeMap<Double, Double> samples) {
        var signal = new ComponentData(4, "SIGNAL." + target.getName(),
                8 + model.getControlComponents().size() % 6 * 10, 0, 503);
        addWaveform(model, signal, samples);
        target.setParameterStrings(new String[]{signal.getTerminalYLabels()[0], "", "0"});
    }

    static void addWaveform(CircuitModel model, ComponentData signal, TreeMap<Double, Double> samples) {
        if (samples.entrySet().stream().anyMatch(point -> !Double.isFinite(point.getKey()) || !Double.isFinite(point.getValue()))) {
            throw new IllegalArgumentException("The source waveform contains a nonfinite time or value.");
        }
        var text = new StringBuilder();
        samples.forEach((time, value) -> text.append(time).append(' ').append(value).append('\n'));
        addControl(model, signal, new double[]{406, 0, 0, 0, 0, 0.5, 0, 0}, new String[0]);
        long id = 200000L + model.getControlComponents().size();
        long resource = 300000L + model.getResources().size();
        String filename = "waveform-" + resource + ".txt";
        signal.getImportTokens().put("uniqueObjectIdentifier", Long.toString(id));
        signal.getImportTokens().put("externalDataFileHashValue", Long.toString(resource));
        signal.getImportTokens().put("datnamXY", filename);
        signal.getImportTokens().put("version170", "1");
        model.getResources().put(resource, new CircuitResource(resource, filename, filename,
                text.toString().getBytes(StandardCharsets.UTF_8), List.of(id)));
    }

    private static void voltageSwitch(ComponentData data, String tail, CircuitModel model,
            Map<String, String> parameters, Map<String, String> definitions, Map<String, String> nodes) {
        String[] args = arguments(tail);
        if (args.length != 3) throw new IllegalArgumentException("A voltage-controlled switch requires two control nodes and one model.");
        String definition = definitions.get(args[2].toLowerCase(Locale.ROOT));
        if (definition == null || !modelType(definition).equals("sw")) throw new IllegalArgumentException("The switch model is missing.");
        var values = new HashMap<String, String>();
        if (!assignments(modelArguments(definition), values)) throw new IllegalArgumentException("The switch model parameters are invalid.");
        if (values.keySet().stream().anyMatch(k -> !List.of("ron", "roff", "vt", "vh").contains(k))) throw new IllegalArgumentException("The switch model contains unsupported parameters.");
        double ron = value(values.getOrDefault("ron", "1"), parameters), roff = value(values.getOrDefault("roff", "1e12"), parameters);
        double threshold = value(values.getOrDefault("vt", "0"), parameters), hysteresis = value(values.getOrDefault("vh", "0"), parameters);
        if (ron <= 0 || roff <= 0 || hysteresis < 0) throw new IllegalArgumentException("The switch parameters are invalid.");
        data.setNumericParameters(new double[]{roff, ron, roff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1});
        var probe = control(model, 1, "VOLT." + data.getName(), new double[]{0}, new String[0]);
        probe.setParameterStrings(new String[]{node(nodes, args[0]), node(nodes, args[1]), "0"});
        var constant = control(model, 3, "THRESHOLD." + data.getName(), new double[]{threshold}, new String[0]);
        var difference = control(model, 13, "ERROR." + data.getName(), new double[0],
                new String[]{probe.getTerminalYLabels()[0], constant.getTerminalYLabels()[0]});
        ComponentData comparator;
        if (hysteresis > 0) comparator = control(model, 11, "COMPARE." + data.getName(), new double[]{hysteresis, 0}, difference.getTerminalYLabels());
        else {
            var zero = control(model, 3, "ZERO." + data.getName(), new double[]{0}, new String[0]);
            comparator = control(model, 46, "COMPARE." + data.getName(), new double[0],
                    new String[]{difference.getTerminalYLabels()[0], zero.getTerminalYLabels()[0]});
        }
        var gate = control(model, 6, "GATE." + data.getName(), new double[]{1}, comparator.getTerminalYLabels());
        gate.setTerminalYLabels(new String[0]);
        gate.setParameterStrings(new String[]{data.getName(), "", "0"});
        data.setParameterStrings(new String[]{gate.getName(), "", "0"});
    }

    static ComponentData control(CircuitModel model, int type, String name, double[] parameters, String[] inputs) {
        int index = model.getControlComponents().size();
        var data = new ComponentData(type, name, 8 + index % 6 * 10, 0, 503);
        addControl(model, data, parameters, inputs);
        return data;
    }

    static void addControl(CircuitModel model, ComponentData data, double[] parameters, String[] inputs) {
        int index = model.getControlComponents().size();
        data.setNumericParameters(parameters);
        data.setTerminalXLabels(inputs);
        data.setTerminalYLabels(new String[]{"control_" + index});
        data.setParameterStrings(new String[]{"", "", "0"});
        model.addControlComponent(data);
    }

    private static boolean firstElement(String[] fields, Map<String, String> parameters, Map<String, String> definitions) {
        if (fields[0].matches("(?i)[a-z][\\w.$:-]*[0-9][\\w.$:-]*")) return true;
        if (fields.length < 4) return false;
        char kind = Character.toUpperCase(fields[0].charAt(0));
        String tail = fields[3].toLowerCase(Locale.ROOT);
        return switch (kind) {
            case 'R', 'L', 'C', 'V', 'I' -> tail.matches("(?s)^(?:[+\\-0-9.{(]|dc\\s|pulse\\s*\\(|sine?\\s*\\().*")
                    || parameters.containsKey(tail);
            case 'D' -> definitions.containsKey(tail);
            case 'S' -> {
                String[] args = tail.trim().split("\\s+");
                yield args.length == 3 && definitions.containsKey(args[2]);
            }
            default -> false;
        };
    }

    private static List<Line> rootLines(List<Line> lines, CircuitModel model) {
        var root = new ArrayList<Line>();
        int depth = 0;
        for (var line : lines) {
            String token = keyword(line.text());
            if (token.equals(".end")) break;
            if (token.equals(".subckt")) {
                if (depth == 0) issue(model, line, "UNSUPPORTED_DIRECTIVE", token, "Subcircuit definitions are unsupported.");
                depth++;
            } else if (token.equals(".ends") && depth > 0) depth--;
            else if (depth == 0) root.add(line);
        }
        return root;
    }

    private static String keyword(String text) {
        return text.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    private static String modelArguments(String definition) {
        return unwrap(definition.replaceFirst("^[^\\s(]+", ""));
    }

    private static String modelType(String definition) {
        return definition.split("[\\s(]", 2)[0].toLowerCase(Locale.ROOT);
    }

    private static String unwrap(String text) {
        String trimmed = text.strip();
        if (!trimmed.startsWith("(")) return trimmed;
        if (!trimmed.endsWith(")")) throw new IllegalArgumentException("Close the argument list.");
        return trimmed.substring(1, trimmed.length() - 1);
    }

    private static List<Line> lines(String source) {
        var result = new ArrayList<Line>();
        int number = 0;
        for (String raw : source.split("\\R")) {
            number++;
            String line = raw.split(";", 2)[0].strip();
            if (number == 1 && line.startsWith("\uFEFF")) line = line.substring(1).strip();
            if (line.isEmpty() || line.startsWith("*")) continue;
            if (line.startsWith("+") && !result.isEmpty()) {
                var previous = result.removeLast();
                result.add(new Line(previous.number(), previous.text() + " " + line.substring(1)));
            } else result.add(new Line(number, line));
        }
        return result;
    }

    static String[] arguments(String text) {
        var result = new ArrayList<String>();
        int depth = 0, start = 0;
        for (int i = 0; i <= text.length(); i++) {
            char c = i == text.length() ? ' ' : text.charAt(i);
            if (c == '{' || c == '(') depth++;
            if (c == '}' || c == ')') depth--;
            if (depth < 0) throw new IllegalArgumentException("The argument list contains an unmatched closing bracket.");
            if ((Character.isWhitespace(c) || c == ',') && depth == 0) {
                if (start < i) result.add(text.substring(start, i));
                start = i + 1;
            }
        }
        if (depth != 0) throw new IllegalArgumentException("Close each parameter expression.");
        return result.toArray(String[]::new);
    }

    private static double value(String text, Map<String, String> parameters) { return SpiceValue.evaluate(text, parameters); }
    private static boolean assignments(String text, Map<String, String> values) {
        var matcher = ASSIGNMENT.matcher(text);
        int end = 0;
        boolean valid = true;
        while (matcher.find()) {
            valid &= text.substring(end, matcher.start()).matches("[\\s,]*");
            values.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2));
            end = matcher.end();
        }
        return valid && text.substring(end).matches("[\\s,]*");
    }
    private static String node(Map<String, String> nodes, String name) {
        return nodes.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> "N" + nodes.size());
    }
    private static void issue(CircuitModel model, Line line, String code, String name, String message) {
        model.addConversionIssue(new ImportIssue(code, "SPICE", name, message == null ? "The input is incomplete." : message,
                model.getFilePath() + ":" + line.number(), true));
    }
}

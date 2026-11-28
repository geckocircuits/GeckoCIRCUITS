package gecko.core.io.importer;

import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.CircuitModel.ConnectionData;
import gecko.core.io.CircuitTopology;
import gecko.core.io.ImportIssue;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.io.importer.PlecsText.Block;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Converts a scalar PLECS Standalone schematic. Electrical nets use numbered
 * ports and the connection tree. Component positions use the saved coordinates.
 */
public final class PlecsImporter {
    private static final double MIN_ON_RESISTANCE = 1e-6;
    private static final double OFF_RESISTANCE = 1e12;
    private static final Pattern POSITION = Pattern.compile("\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*]");
    private static final Pattern NUMERIC_SUFFIX = Pattern.compile(
            "(?<![\\p{Alnum}_.])(?:\\d++(?:\\.\\d*+)?|\\.\\d++)(?:[eE][+-]?\\d++)?+[a-zA-Zµ_]");

    public CircuitModel convert(String source, String sourceName) {
        var conversion = new Conversion(source.hashCode(), sourceName);
        try {
            conversion.convert(PlecsText.parse(source));
        } catch (PlecsText.SyntaxError error) {
            conversion.issue("INVALID_PLECS_FORMAT", null, error.line(), error.getMessage(), null);
        }
        return conversion.model;
    }

    private enum Kind {
        RESISTOR(1, 2, 0, 0, "R"),
        INDUCTOR(2, 2, 0, 0, "L", "i_init"),
        CAPACITOR(3, 2, 0, 0, "C", "v_init"),
        DCVOLTAGESOURCE(4, 2, 0, 0, "V"),
        DCCURRENTSOURCE(5, 2, 0, 0, "I"),
        ACVOLTAGESOURCE(4, 2, 0, 0, "V", "w", "phi"),
        ACCURRENTSOURCE(5, 2, 0, 0, "I", "w", "phi"),
        VOLTAGESOURCE(4, 2, 3, 0, "DiscretizationBehavior", "StateSpaceInlining"),
        CURRENTSOURCE(5, 2, 3, 0, "DiscretizationBehavior", "StateSpaceInlining"),
        DIODE(6, 2, 0, 0, "Vf", "Ron", "thermal", "T_init"),
        SWITCH(7, 2, 3, 0, "s_init"),
        GROUND(0, 1, 0, 0),
        PULSEGENERATOR(4, 0, 0, 1, "Hi", "Lo", "f", "DutyCycle", "Delay", "DataType"),
        SINEGENERATOR(4, 0, 0, 1, "Amplitude", "Bias", "Frequency", "Phase", "Units"),
        CONSTANT(3, 0, 0, 1, "Value", "DataType"),
        VOLTMETER(1, 2, 0, 3),
        COMPARATOR(26, 0, 1, 3);

        final int type;
        final int electricalPorts;
        final int signalInput;
        final int signalOutput;
        final List<String> parameters;

        Kind(int type, int electricalPorts, int signalInput, int signalOutput, String... parameters) {
            this.type = type;
            this.electricalPorts = electricalPorts;
            this.signalInput = signalInput;
            this.signalOutput = signalOutput;
            this.parameters = List.of(parameters);
        }

        boolean isElectricalComponent() { return electricalPorts == 2 && signalOutput == 0; }

        boolean acceptsSignalInput(int terminal) {
            return terminal == signalInput || this == COMPARATOR && terminal == 2;
        }
    }

    private record Port(String name, int terminal) { }

    private enum State {
        OPEN("0"), ACTIVE("1"), SHORTED("2");

        final String enabledShorted;

        State(String enabledShorted) { this.enabledShorted = enabledShorted; }
    }

    private record Part(Kind kind, Block block, ComponentData data, double[] values, State state) { }

    private record Point(int x, int y) { }

    private record LayoutTerminal(Part part, String label, Point start, List<Point> ends) { }

    private static final class Conversion {
        private final CircuitModel model = new CircuitModel();
        private final Map<String, Part> parts = new LinkedHashMap<>();
        private final Map<Port, Port> electrical = new HashMap<>();
        private final Map<Port, Port> signals = new HashMap<>();
        private final Map<Port, String> labels = new LinkedHashMap<>();
        private final Map<Port, String> outputs = new HashMap<>();
        private final List<Part> electricalParts = new ArrayList<>();
        private int generatedRow = 8;
        private int generatedCount;
        private ComponentData zero;
        private boolean fixedStep;
        private boolean discreteModel;

        Conversion(int id, String sourceName) {
            model.setFilePath(sourceName == null || sourceName.isBlank() ? "<PLECS>" : sourceName);
            model.setFileVersion(202);
            model.setUniqueFileId(id);
            model.setSimulationDuration(0.01);
            model.setTimeStep(1e-6);
        }

        void convert(Block root) {
            var schematics = root.children("Schematic");
            if (schematics.size() != 1) {
                issue("INVALID_PLECS_FORMAT", null, root.line(), "Specify one root Schematic block.", null);
                return;
            }
            settings(root);
            Block schematic = schematics.getFirst();
            var seen = new HashSet<String>();
            for (Block block : schematic.children("Component")) {
                String name = block.get("Name");
                if (name == null || name.isBlank() || !seen.add(name)) {
                    issue("INVALID_PLECS_COMPONENT", name, block.line(), "Use a unique component name.", "Name");
                    continue;
                }
                readPart(block, name);
            }
            waveformStep();
            for (Block block : schematic.children("Connection")) readConnection(block);
            for (var entry : parts.entrySet()) {
                if (entry.getValue().state() == State.SHORTED) {
                    join(new Port(entry.getKey(), 1), new Port(entry.getKey(), 2));
                }
            }
            referenceNode();
            addSignalProducers();
            addComparators();
            addElectricalComponents();
            routeTerminalWires();
            validateElectricalPartition();

            var scopeNodes = new LinkedHashMap<String, String>();
            for (Part part : electricalParts) {
                if (part.state() != State.ACTIVE) continue;
                String first = part.data().getTerminalXLabels()[0], second = part.data().getTerminalYLabels()[0];
                scopeNodes.put(first, first);
                scopeNodes.put(second, second);
            }
            int before = model.getControlComponents().size();
            SpiceImporter.addScope(model, scopeNodes);
            for (int i = before; i < model.getControlComponents().size(); i++) {
                positionGenerated(model.getControlComponents().get(i));
            }
        }

        private void settings(Block root) {
            try {
                double start = number(root, "StartTime", 0);
                double duration = root.get("TimeSpan") == null ? number(root, "StopTime", 0.01) : number(root, "TimeSpan", 0);
                if (root.get("TimeSpan") != null && root.get("StopTime") != null && number(root, "StopTime", 0) != start + duration)
                    throw new IllegalArgumentException("Use the same simulation end time in StopTime and TimeSpan.");
                if (start != 0 || duration <= 0) throw new IllegalArgumentException("Specify a zero start time and a positive duration.");
                String solver = root.get("Solver");
                fixedStep = "discrete".equalsIgnoreCase(solver);
                discreteModel = fixedStep || "DiscStateSpace".equals(root.get("CircuitModel"));
                double step = number(root, fixedStep ? "FixedStep" : "MaxStep", -1);
                if (step == 0 || step < 0 && step != -1)
                    throw new IllegalArgumentException("Specify a positive time step or the automatic value.");
                if (!fixedStep || step == -1) step = step > 0 ? Math.min(duration / 10000, step) : duration / 10000;
                if (!(step > 0 && step < duration)) throw new IllegalArgumentException("Specify a time step below the duration.");
                model.setSimulationDuration(duration);
                model.setTimeStep(step);
            } catch (IllegalArgumentException error) {
                issue("INVALID_PLECS_TIMING", null, root.line(), error.getMessage(), null);
            }
            for (String key : List.of("InitializationCommands", "SystemState")) {
                if (!Objects.toString(root.get(key), "").isBlank()) {
                    issue("UNSUPPORTED_PLECS_SETTING", null, root.line(), "The PLECS setting requires evaluation.", key);
                }
            }
            try {
                if (number(root, "TurnOnThreshold", 0) != 0)
                    issue("UNSUPPORTED_PLECS_SETTING", null, root.line(), "The diode turn-on threshold is unsupported.", "TurnOnThreshold");
            } catch (IllegalArgumentException error) {
                issue("INVALID_PLECS_SETTING", null, root.line(), error.getMessage(), "TurnOnThreshold");
            }
        }

        private void waveformStep() {
            double step = model.getTimeStep();
            for (var entry : parts.entrySet()) {
                Part part = entry.getValue();
                if (part.state() != State.ACTIVE) continue;
                double[] values = part.values();
                double limit = switch (part.kind()) {
                    case PULSEGENERATOR -> values[3] == 0 || values[3] == 1 || values[0] == values[1]
                            ? Double.POSITIVE_INFINITY : Math.min(0.01, Math.min(values[3], 1 - values[3]) / 20) / values[2];
                    case ACVOLTAGESOURCE, ACCURRENTSOURCE, SINEGENERATOR -> 2 * Math.PI / 100 / Math.abs(values[1]);
                    default -> Double.POSITIVE_INFINITY;
                };
                if (limit <= 0 || fixedStep && step > limit) {
                    issue("INVALID_PLECS_TIMING", entry.getKey(), part.block().line(),
                            "Use a smaller time step for this waveform.", "FixedStep");
                } else if (!fixedStep) {
                    step = Math.min(step, limit);
                }
            }
            model.setTimeStep(step);
        }

        private void readPart(Block block, String name) {
            String type = Objects.toString(block.get("Type"), "");
            if (type.isBlank()) {
                issue("INVALID_PLECS_COMPONENT", name, block.line(), "Specify a component type.", "Type");
                return;
            }
            Kind kind;
            try {
                kind = Kind.valueOf(type.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                String code = switch (type) {
                    case "SubSystem", "Subsystem", "ConfigurableSubsystem" -> "UNSUPPORTED_PLECS_SUBSYSTEM";
                    case "CScript" -> "UNSUPPORTED_PLECS_C_SCRIPT";
                    default -> "UNSUPPORTED_PLECS_COMPONENT";
                };
                issue(code, name, block.line(), "Unsupported PLECS component type " + type + ".", "Type");
                return;
            }

            try {
                if (!block.children("Schematic").isEmpty())
                    throw new IllegalArgumentException("The component contains a nested schematic.");
                var position = POSITION.matcher(Objects.toString(block.get("Position"), ""));
                if (!position.matches()) throw new IllegalArgumentException("Specify a two-coordinate Position field.");
                int x = Integer.parseInt(position.group(1));
                int y = Integer.parseInt(position.group(2));
                // Leave room for native terminal offsets and generated scope rows.
                if (Math.abs((long) x) > Integer.MAX_VALUE / 16 || Math.abs((long) y) > Integer.MAX_VALUE / 16)
                    throw new IllegalArgumentException("Use coordinates within the worksheet range.");

                var params = new LinkedHashMap<String, String>();
                for (Block parameter : block.children("Parameter")) {
                    String variable = parameter.get("Variable");
                    String value = parameter.get("Value");
                    if (variable == null || value == null || params.putIfAbsent(variable, value) != null)
                        throw new IllegalArgumentException("Specify a unique variable and value for each Parameter block.");
                    if (!kind.parameters.contains(variable)) {
                        issue("UNSUPPORTED_PLECS_PARAMETER", name, parameter.line(),
                                "The PLECS parameter is unsupported.", variable);
                        return;
                    }
                }
                var data = new ComponentData(kind.type, nativeName(name), x, y, orientation(block, kind));
                State state = switch (block.fields().getOrDefault("CommentOut", "")) {
                    case "" -> State.ACTIVE;
                    case "DisabledOpenCircuit" -> State.OPEN;
                    case "DisabledClosedCircuit" -> State.SHORTED;
                    default -> null;
                };
                if (state == null || state != State.ACTIVE && !kind.isElectricalComponent()) {
                    issue("UNSUPPORTED_PLECS_COMMENT_MODE", name, block.line(),
                            "The component comment mode is unsupported.", "CommentOut");
                    return;
                }
                if (state == State.ACTIVE && (kind == Kind.VOLTAGESOURCE || kind == Kind.CURRENTSOURCE)
                        && !checkControlledSource(name, block, params)) return;
                double[] values = values(kind, params);
                if (state != State.ACTIVE) {
                    data.setSavedBlock(SavedCircuitBlock.parse("ElementLK", new String[]{"enabledShorted " + state.enabledShorted}));
                }
                parts.put(name, new Part(kind, block, data, values, state));
                generatedRow = Math.max(generatedRow, y + 10);
            } catch (IllegalArgumentException error) {
                issue("INVALID_PLECS_COMPONENT", name, block.line(), error.getMessage(), null);
            }
        }

        private boolean checkControlledSource(String name, Block block, Map<String, String> params) {
            // PLECS applies the selected hold to a discretized model. It can also inline a measurement.
            // See https://docs.plexim.com/plecs/latest/components-by-category/controlledvoltagesource/.
            boolean hold = checkSourceChoice(name, block, params, "DiscretizationBehavior", 2, 3, !discreteModel);
            boolean inlining = checkSourceChoice(name, block, params, "StateSpaceInlining", 1, 2, true);
            return hold && inlining;
        }

        private boolean checkSourceChoice(String name, Block block, Map<String, String> params, String key,
                int supported, int lastChoice, boolean supportedModel) {
            int choice;
            try {
                choice = Integer.parseInt(params.getOrDefault(key, Integer.toString(supported)).strip());
            } catch (NumberFormatException error) {
                choice = 0;
            }
            if (choice < 1 || choice > lastChoice) {
                issue("INVALID_PLECS_PARAMETER", name, block.line(), "Specify a valid numeric choice for " + key + ".", key);
                return false;
            }
            if (supportedModel && choice == supported) return true;
            issue("UNSUPPORTED_PLECS_PARAMETER", name, block.line(),
                    "The controlled source option is unsupported in this simulation model.", key);
            return false;
        }

        private void readConnection(Block block) {
            String type = block.get("Type");
            boolean wire = "Wire".equals(type);
            if (!wire && !"Signal".equals(type)) {
                issue("UNSUPPORTED_PLECS_CONNECTION", block.get("SrcComponent"), block.line(),
                        "Unsupported PLECS connection type " + type + ".", "Type");
                return;
            }
            Port source = endpoint(block, "Src", wire);
            var destinations = new ArrayList<Port>();
            readDestinations(block, wire, destinations);
            if (wire) {
                if (source != null) destinations.addFirst(source);
                for (int index = 1; index < destinations.size(); index++) {
                    join(destinations.get(index), destinations.getFirst());
                }
            } else {
                for (Port destination : destinations) {
                    if (source != null && signals.putIfAbsent(destination, source) != null) {
                        issue("INVALID_PLECS_CONNECTION", destination.name(), block.line(),
                                "Connect the signal input to one source.", "DstTerminal");
                    }
                }
            }
        }

        private void join(Port first, Port second) {
            electrical.put(root(first), root(second));
        }

        private void readDestinations(Block block, boolean wire, List<Port> destinations) {
            var branches = block.children("Branch");
            if (branches.isEmpty() || block.get("DstComponent") != null || block.get("DstTerminal") != null) {
                Port destination = endpoint(block, "Dst", wire);
                if (destination != null) destinations.add(destination);
            }
            for (Block branch : branches) readDestinations(branch, wire, destinations);
        }

        private Port endpoint(Block block, String prefix, boolean wire) {
            String name = block.get(prefix + "Component");
            String terminal = block.get(prefix + "Terminal");
            if (name == null || terminal == null) {
                issue("INVALID_PLECS_CONNECTION", name, block.line(), "Specify the component and terminal for the connection.", prefix);
                return null;
            }
            Part part = parts.get(name);
            if (part == null) {
                issue("UNRESOLVED_PLECS_CONNECTION", name, block.line(), "The connection refers to an unconverted component.", prefix);
                return null;
            }
            int index;
            try {
                index = Integer.parseInt(terminal);
            } catch (NumberFormatException error) {
                index = 0;
            }
            boolean valid = wire ? index <= part.kind().electricalPorts : prefix.equals("Src")
                    ? index == part.kind().signalOutput : part.kind().acceptsSignalInput(index);
            if (index <= 0 || !valid) {
                issue("INVALID_PLECS_TERMINAL", name, block.line(),
                        "The terminal " + terminal + " is invalid for this connection.", prefix + "Terminal");
                return null;
            }
            return new Port(name, index);
        }

        private Port root(Port port) {
            Port base = port;
            Port parent = electrical.get(base);
            while (parent != null && !parent.equals(base)) {
                base = parent;
                parent = electrical.get(base);
            }
            while (!port.equals(base)) {
                Port next = electrical.put(port, base);
                port = next;
            }
            return base;
        }

        private void referenceNode() {
            for (var entry : parts.entrySet()) {
                if (entry.getValue().kind() == Kind.GROUND) labels.put(root(new Port(entry.getKey(), 1)), "0");
            }
            if (labels.isEmpty()) {
                var circuits = parts.entrySet().stream().filter(entry -> entry.getValue().kind().isElectricalComponent()).toList();
                var reference = circuits.stream().filter(entry -> entry.getValue().kind().type == 4
                        && entry.getValue().state() == State.ACTIVE).findFirst();
                if (!circuits.isEmpty()) labels.put(root(new Port(reference.orElse(circuits.getFirst()).getKey(), 2)), "0");
            }
        }

        private String label(String name, int port) {
            return labels.computeIfAbsent(root(new Port(name, port)), unused -> "N" + labels.size());
        }

        private void addSignalProducers() {
            for (var entry : parts.entrySet()) {
                Part part = entry.getValue();
                ComponentData data = part.data();
                if (part.kind().signalOutput == 0 || part.kind() == Kind.COMPARATOR) continue;
                try {
                    switch (part.kind()) {
                        case PULSEGENERATOR -> SpiceImporter.addWaveform(model, data, pulse(part.values(), model.getSimulationDuration()));
                        case SINEGENERATOR -> {
                            double[] values = part.values();
                            SpiceImporter.addWaveform(model, data, SpiceImporter.sampleWaveform(model,
                                    time -> values[3] + values[0] * Math.sin(values[1] * time + values[2])));
                        }
                        case CONSTANT -> SpiceImporter.addControl(model, data, part.values(), new String[0]);
                        case VOLTMETER -> {
                            SpiceImporter.addControl(model, data, new double[]{0}, new String[0]);
                            data.setParameterStrings(new String[]{label(entry.getKey(), 1), label(entry.getKey(), 2), "0"});
                        }
                        default -> throw new IllegalStateException("Missing signal component conversion.");
                    }
                    outputs.put(new Port(entry.getKey(), part.kind().signalOutput), data.getTerminalYLabels()[0]);
                } catch (IllegalArgumentException error) {
                    issue("INVALID_PLECS_COMPONENT", entry.getKey(), part.block().line(), error.getMessage(), null);
                }
            }
        }

        private void addComparators() {
            for (var entry : parts.entrySet()) {
                Part part = entry.getValue();
                if (part.kind() == Kind.COMPARATOR) {
                    outputs.put(new Port(entry.getKey(), 3), "PLECS_OUT_" + outputs.size());
                }
            }
            for (var entry : parts.entrySet()) {
                String name = entry.getKey();
                Part part = entry.getValue();
                if (part.kind() != Kind.COMPARATOR) continue;
                String plus = incomingSignal(name, 1);
                String minus = incomingSignal(name, 2);
                if (plus == null || minus == null) {
                    issue("INVALID_PLECS_COMPONENT", name, part.block().line(), "Connect both comparator inputs.", null);
                    continue;
                }
                // See https://docs.plexim.com/plecs/latest/components-by-category/comparator/.
                // The Comparator holds its previous output while both inputs are equal.
                var greater = generatedControl(46, "GT." + part.data().getName(), new double[0], plus, minus);
                var unequal = generatedControl(48, "NE." + part.data().getName(), new double[0], plus, minus);
                SpiceImporter.addControl(model, part.data(), new double[0],
                        new String[]{greater.getTerminalYLabels()[0], unequal.getTerminalYLabels()[0]});
                part.data().setTerminalYLabels(new String[]{outputs.get(new Port(name, 3))});
            }
        }

        private String incomingSignal(String name, int terminal) {
            return outputs.get(signals.get(new Port(name, terminal)));
        }

        private void addElectricalComponents() {
            for (var entry : parts.entrySet()) {
                String name = entry.getKey();
                Part part = entry.getValue();
                Kind kind = part.kind();
                if (!kind.isElectricalComponent()) continue;
                ComponentData data = part.data();
                String input = incomingSignal(name, kind.signalInput);
                try {
                    boolean active = part.state() == State.ACTIVE;
                    if (active && kind.signalInput != 0 && input == null && kind != Kind.SWITCH)
                        throw new IllegalArgumentException("Connect the source to a supported signal generator.");

                    data.setTerminalXLabels(new String[]{label(name, 1)});
                    data.setTerminalYLabels(new String[]{label(name, 2)});
                    data.setParameterStrings(new String[]{"", "", "0"});
                    switch (kind) {
                        case DCVOLTAGESOURCE, DCCURRENTSOURCE -> SpiceImporter.source(data, Double.toString(part.values()[0]), model, Map.of());
                        case ACVOLTAGESOURCE, ACCURRENTSOURCE -> {
                            double[] values = part.values();
                            String sine = "SIN(0 " + values[0] + " " + values[1] / (2 * Math.PI) + " 0 0 "
                                    + Math.toDegrees(values[2]) + ")";
                            SpiceImporter.source(data, sine, model, Map.of());
                            positionGenerated(model.getControlComponents().getLast());
                        }
                        case VOLTAGESOURCE, CURRENTSOURCE -> {
                            if (active) SpiceImporter.controlledSource(data, input);
                            else SpiceImporter.source(data, "0", model, Map.of());
                        }
                        case SWITCH -> {
                            if (active) configureSwitch(name, part, input);
                            else data.setNumericParameters(part.values());
                        }
                        default -> data.setNumericParameters(part.values());
                    }
                    model.addCircuitComponent(data);
                    electricalParts.add(part);
                } catch (IllegalArgumentException error) {
                    issue("INVALID_PLECS_COMPONENT", name, part.block().line(), error.getMessage(), null);
                }
            }
        }

        private void routeTerminalWires() {
            var terminals = new ArrayList<LayoutTerminal>();
            var occupied = new HashMap<Point, String>();
            for (Part part : electricalParts) {
                for (boolean input : new boolean[]{true, false}) {
                    LayoutTerminal terminal = layoutTerminal(part, input);
                    terminals.add(terminal);
                    String previous = occupied.putIfAbsent(terminal.start(), terminal.label());
                    if (previous != null && !previous.equals(terminal.label()))
                        layoutIssue(part, "Move the component to separate terminals of different electrical nets.");
                }
            }
            for (LayoutTerminal terminal : terminals) {
                var end = terminal.ends().stream()
                        .filter(point -> occupied.getOrDefault(point, terminal.label()).equals(terminal.label()))
                        .findFirst();
                if (end.isEmpty()) {
                    layoutIssue(terminal.part(), "Move the component to make room for its terminal wire.");
                    continue;
                }
                occupied.put(end.get(), terminal.label());
                model.addConnection(new ConnectionData("LK", new int[][]{
                        {terminal.start().x(), terminal.start().y()}, {end.get().x(), end.get().y()}}, terminal.label()));
            }
        }

        private void validateElectricalPartition() {
            var topology = CircuitTopology.resolve(model);
            var groupsByLabel = new HashMap<String, Integer>();
            var labelsByGroup = new HashMap<Integer, String>();
            for (Part part : electricalParts) {
                if (part.state() == State.OPEN) continue;
                ComponentData data = part.data();
                for (var side : CircuitTopology.Side.values()) {
                    String expected = (side == CircuitTopology.Side.INPUT ? data.getTerminalXLabels() : data.getTerminalYLabels())[0];
                    var terminal = new CircuitTopology.TerminalIdentity(topology.identity(data), side, 0);
                    Integer group = topology.terminalGroups().get(terminal);
                    if (group == null || !groupsByLabel.getOrDefault(expected, group).equals(group)
                            || !labelsByGroup.getOrDefault(group, expected).equals(expected)) {
                        layoutIssue(part, "The exported electrical nets differ from the source connections.");
                        return;
                    }
                    groupsByLabel.put(expected, group);
                    labelsByGroup.put(group, expected);
                }
            }
        }

        private void layoutIssue(Part part, String message) {
            issue("UNSUPPORTED_PLECS_LAYOUT", part.block().get("Name"), part.block().line(), message, "Position");
        }

        private void configureSwitch(String name, Part part, String input) {
            var data = part.data();
            data.setNumericParameters(part.values());
            if (input == null && signals.containsKey(new Port(name, 3)))
                throw new IllegalArgumentException("Connect the switch to a supported signal source.");
            if (input == null) {
                // An undriven PLECS Switch retains its initial conductivity.
                var initial = generatedControl(3, "INITIAL." + data.getName(), new double[]{part.values()[0] == OFF_RESISTANCE ? 0 : 1});
                input = initial.getTerminalYLabels()[0];
            }
            if (zero == null) zero = generatedControl(3, "ZERO", new double[]{0});
            var compare = generatedControl(48, "IN.ON." + data.getName(), new double[0], input, zero.getTerminalYLabels()[0]);
            var gate = generatedControl(6, "GATE." + data.getName(), new double[]{1}, compare.getTerminalYLabels());
            gate.setTerminalYLabels(new String[0]);
            gate.setParameterStrings(new String[]{data.getName(), "", "0"});
            data.setParameterStrings(new String[]{gate.getName(), "", "0"});
        }

        private ComponentData generatedControl(int type, String name, double[] parameters, String... inputs) {
            var control = SpiceImporter.control(model, type, "PLECS." + name, parameters, inputs);
            positionGenerated(control);
            return control;
        }

        private void positionGenerated(ComponentData control) {
            int index = generatedCount++;
            control.getPosition()[0] = 8 + index % 8 * 10;
            control.getPosition()[1] = generatedRow + index / 8 * 12;
        }

        private void issue(String code, String name, int line, String message, String parameter) {
            model.addConversionIssue(new ImportIssue(code, "PLECS", name == null || name.isBlank() ? null : name, message,
                    model.getFilePath() + ":" + line, true, parameter));
        }
    }

    private static LayoutTerminal layoutTerminal(Part part, boolean input) {
        ComponentData data = part.data();
        int direction = input ? 1 : -1;
        int dx = direction * switch (data.getOrientation()) { case 502 -> -1; case 504 -> 1; default -> 0; };
        int dy = direction * switch (data.getOrientation()) { case 501 -> 1; case 502, 504 -> 0; default -> -1; };
        int x = data.getPosition()[0] + 2 * dx, y = data.getPosition()[1] + 2 * dy;
        String label = (input ? data.getTerminalXLabels() : data.getTerminalYLabels())[0];
        return new LayoutTerminal(part, label, new Point(x, y), List.of(
                new Point(x + dy, y - dx), new Point(x - dy, y + dx), new Point(x + dx, y + dy)));
    }

    private static int orientation(Block block, Kind kind) {
        String direction = block.fields().getOrDefault("Direction", "up");
        String flipped = block.fields().getOrDefault("Flipped", "off");
        if (!flipped.equals("on") && !flipped.equals("off")) throw new IllegalArgumentException("Specify Flipped as on or off.");
        boolean reverse = flipped.equals("on");
        // The input terminal of a PLECS current source points opposite the arrow.
        if (kind.type == 5 && kind.isElectricalComponent()) reverse = !reverse;
        return switch (direction) {
            case "up" -> reverse ? 501 : 503;
            case "down" -> reverse ? 503 : 501;
            case "left" -> reverse ? 504 : 502;
            case "right" -> reverse ? 502 : 504;
            default -> throw new IllegalArgumentException("Specify a valid component Direction.");
        };
    }

    private static String nativeName(String name) {
        // Native parameter references use slashes as separators.
        String encoded = name.replace("%", "%25").replace("/", "%2F").replace("\n", "%0A")
                .replace("\r", "%0D").replace("\t", "%09");
        if (!encoded.equals(encoded.strip())) return encoded.replace(" ", "%20");
        return encoded;
    }

    private static double[] values(Kind kind, Map<String, String> params) {
        return switch (kind) {
            case RESISTOR -> new double[]{positive(params, "R"), 0, 0};
            case INDUCTOR -> new double[]{positive(params, "L"), number(params, "i_init", 0), 0, 0, 0};
            case CAPACITOR -> new double[]{positive(params, "C"), number(params, "v_init", 0), 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case DCVOLTAGESOURCE -> new double[]{number(params, "V", 1)};
            case DCCURRENTSOURCE -> new double[]{number(params, "I", 1)};
            case ACVOLTAGESOURCE, ACCURRENTSOURCE -> new double[]{
                    number(params, kind == Kind.ACVOLTAGESOURCE ? "V" : "I", 1),
                    number(params, "w", 2 * Math.PI * 50), number(params, "phi", 0)};
            case DIODE -> {
                double resistance = number(params, "Ron", 0);
                double forward = number(params, "Vf", 0);
                if (resistance < 0) throw new IllegalArgumentException("Specify a nonnegative diode on-resistance.");
                if (!params.getOrDefault("thermal", "").isBlank() || number(params, "T_init", 0) != 0)
                    throw new IllegalArgumentException("The diode thermal model is unsupported.");
                yield new double[]{OFF_RESISTANCE, forward, resistance == 0 ? MIN_ON_RESISTANCE : resistance,
                        OFF_RESISTANCE, 0, 0, 0, 0, 0, 0, 0, 0, 1};
            }
            case SWITCH -> {
                double initial = number(params, "s_init", 0);
                yield new double[]{initial == 0 ? OFF_RESISTANCE : MIN_ON_RESISTANCE, MIN_ON_RESISTANCE,
                        OFF_RESISTANCE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
            }
            case PULSEGENERATOR -> {
                checkDataType(params);
                yield new double[]{number(params, "Lo", 0), number(params, "Hi", 1), positive(params, "f"),
                        number(params, "DutyCycle", 0.5), number(params, "Delay", 0)};
            }
            case CONSTANT -> {
                checkDataType(params);
                yield new double[]{number(params, "Value", 1)};
            }
            case SINEGENERATOR -> sineValues(params);
            default -> new double[0];
        };
    }

    private static double[] sineValues(Map<String, String> params) {
        // See https://docs.plexim.com/plecs/latest/components-by-category/sinewave/.
        String units = params.getOrDefault("Units", "1").strip();
        double frequency = number(params, "Frequency", units.equals("1") ? 2 * Math.PI * 50 : 50);
        double phase = number(params, "Phase", 0);
        switch (units) {
            case "1" -> { }
            case "2" -> {
                frequency *= 2 * Math.PI;
                phase *= 2 * Math.PI;
            }
            case "3" -> {
                frequency *= 2 * Math.PI;
                phase = Math.toRadians(phase);
            }
            default -> throw new IllegalArgumentException("Specify a valid Units choice for the sine generator.");
        }
        return new double[]{number(params, "Amplitude", 1), frequency, phase, number(params, "Bias", 0)};
    }

    private static void checkDataType(Map<String, String> params) {
        if (number(params, "DataType", 10) != 10)
            throw new IllegalArgumentException("Use the default scalar signal data type.");
    }

    private static double positive(Map<String, String> params, String key) {
        double value = number(params, key, 0);
        if (value <= 0) throw new IllegalArgumentException("Specify a positive value for " + key + ".");
        return value;
    }

    private static double number(Block block, String key, double fallback) {
        return number(block.fields(), key, fallback);
    }

    private static double number(Map<String, String> params, String key, double fallback) {
        String value = params.get(key);
        if (value == null) return fallback;
        if (NUMERIC_SUFFIX.matcher(value).find() || value.indexOf('{') >= 0 || value.indexOf('}') >= 0 || value.contains("**"))
            throw new IllegalArgumentException("Specify a scalar SI value for " + key + ".");
        return SpiceValue.evaluate(value, Map.of());
    }

    private static TreeMap<Double, Double> pulse(double[] values, double duration) {
        double low = values[0], high = values[1], period = 1 / values[2], duty = values[3], delay = values[4];
        if (duty < 0 || duty > 1 || !Double.isFinite(period))
            throw new IllegalArgumentException("The pulse timing is invalid.");
        var samples = new TreeMap<Double, Double>();
        if (duty == 0 || duty == 1 || low == high) {
            double value = duty == 0 ? low : high;
            samples.put(0.0, value);
            samples.put(duration, value);
            return samples;
        }
        double cycles = Math.ceil(duration / period) + 1;
        if (cycles > 1000000) throw new IllegalArgumentException("The pulse table exceeds one million cycles.");

        // Put points on each side of the edge. The native table interpolates linearly.
        double phase = delay % period;
        if (phase > 0) phase -= period;
        double width = duty * period;
        samples.put(0.0, phase + width > 0 ? high : low);
        for (int i = 0; i <= cycles; i++) {
            double start = phase + i * period;
            edge(samples, start, low, high);
            edge(samples, start + width, high, low);
        }
        return samples;
    }

    private static void edge(TreeMap<Double, Double> samples, double time, double before, double after) {
        if (time > 0) samples.put(Math.nextDown(time), before);
        if (time >= 0) samples.put(time, after);
    }
}

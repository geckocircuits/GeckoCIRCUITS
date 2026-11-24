package gecko.core.io.importer;

import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.CircuitModel.ConnectionData;
import gecko.core.io.CircuitTopology;
import gecko.core.io.CircuitTopology.Side;
import gecko.core.io.ImportIssue;
import gecko.core.io.ArtifactDigest;
import gecko.core.io.BuildIdentity;
import gecko.core.io.AcProvenance;
import gecko.core.io.SpicePorts;
import gecko.core.ac.AcComponent;
import gecko.core.io.importer.SpiceImporter.Line;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Imports standard LTspice symbols and preserves the source wire geometry. */
public final class LtspiceImporter {
    private record Point(int x, int y) {
        int[] coordinates() { return new int[]{x, y}; }
    }

    private record Wire(Point start, Point end) {
        boolean contains(Point point) {
            if (point.x < Math.min(start.x, end.x) || point.x > Math.max(start.x, end.x)
                    || point.y < Math.min(start.y, end.y) || point.y > Math.max(start.y, end.y)) return false;
            if (start.x == end.x || start.y == end.y) return true;
            long dx = (long) end.x - start.x, dy = (long) end.y - start.y;
            return BigInteger.valueOf(dx).multiply(BigInteger.valueOf((long) point.y - start.y))
                    .equals(BigInteger.valueOf(dy).multiply(BigInteger.valueOf((long) point.x - start.x)));
        }

        List<Point> vertices(Set<Point> points) {
            var order = start.x == end.x ? Comparator.comparingInt(Point::y) : Comparator.comparingInt(Point::x);
            if (order.compare(start, end) > 0) order = order.reversed();
            return points.stream().filter(this::contains).sorted(order).toList();
        }
    }

    private record Flag(Point point, String label) { }

    private enum Orientation {
        R0, R90, R180, R270, M0, M90, M180, M270;

        Point place(Point origin, Point pin) {
            // LTspice rotates clockwise, then mirrors the rotated x coordinate.
            int quarterTurns = ordinal() % 4;
            int x = switch (quarterTurns) {
                case 0 -> pin.x;
                case 1 -> -pin.y;
                case 2 -> -pin.x;
                default -> pin.y;
            };
            int y = switch (quarterTurns) {
                case 0 -> pin.y;
                case 1 -> pin.x;
                case 2 -> -pin.y;
                default -> -pin.x;
            };
            if (ordinal() >= 4) x = -x;
            return new Point(Math.addExact(origin.x, x), Math.addExact(origin.y, y));
        }
    }

    private record Symbol(int line, String kind, Point origin, String rotation, Map<String, String> attributes) {
        String name() { return attributes.getOrDefault("instname", kind); }
    }

    private record PlacedSymbol(Symbol symbol, SpicePorts type, List<Point> pins) {
        Point center(Grid grid) {
            Point first = grid.position(pins.getFirst()), second = grid.position(pins.get(1));
            return new Point(first.x + (second.x - first.x) / 2, first.y + (second.y - first.y) / 2);
        }

        ComponentData component(Grid grid) {
            Point first = pins.getFirst(), second = pins.get(1);
            int orientation = first.x < second.x ? 502 : first.x > second.x ? 504
                    : first.y < second.y ? 503 : 501;
            Point center = center(grid);
            return new ComponentData(type.type(), symbol.name(), center.x, center.y, orientation);
        }

        boolean mirrored() { return symbol.rotation.toUpperCase(Locale.ROOT).startsWith("M"); }
    }

    private record Grid(int minX, int minY, int divisor, int multiplier) {
        static Grid fit(Set<Point> points) {
            int minX = points.stream().mapToInt(Point::x).min().orElse(0);
            int minY = points.stream().mapToInt(Point::y).min().orElse(0);
            // Keep every junction on the grid. Refine it for off-grid inputs.
            int divisor = 8;
            for (Point point : points) {
                while (((long) point.x - minX) % divisor != 0 || ((long) point.y - minY) % divisor != 0) divisor /= 2;
            }
            return new Grid(minX, minY, divisor, 1);
        }

        Grid refine() {
            return divisor > 1 ? new Grid(minX, minY, divisor / 2, multiplier)
                    : new Grid(minX, minY, divisor, multiplier * 2);
        }

        Point position(Point point) {
            return new Point(Math.toIntExact(((long) point.x - minX) / divisor * multiplier + 6),
                    Math.toIntExact(((long) point.y - minY) / divisor * multiplier + 6));
        }
    }

    private record Layout(Grid grid, Map<ComponentData, Point> centers, List<ConnectionData> connections, String conflict) { }

    private static final Set<String> DRAWING_RECORDS = Set.of(
            "VERSION", "SHEET", "WINDOW", "LINE", "RECTANGLE", "CIRCLE", "ARC", "IOPIN", "DATAFLAG");
    private static final Set<String> TRANSIENT_OPTIONS = Set.of(
            "uic", "startup", "steady", "nodiscard", "step", "loadstate", "savestate", "savestatetime");

    /**
     * Imports a schematic file with its saved encoding.
     * @param path the LTspice schematic path
     * @return the converted circuit and its import issues
     * @throws IOException if the input cannot be read or decoded
     */
    public CircuitModel convert(Path path) throws IOException {
        return convert(decode(Files.readAllBytes(path)), path.toString());
    }

    /** Verifies the stock symbol definition of each supported AC component. */
    public CircuitModel convert(Path path, Path symbols) throws IOException {
        var build = BuildIdentity.current();
        byte[] source = Files.readAllBytes(path);
        CircuitModel model = convert(decode(source), path.toString());
        if (model.getAcSettings() == null) throw new IllegalArgumentException("Stock-symbol verification requires an AC schematic.");
        var names = Map.of(SpicePorts.RES, "res.asy", SpicePorts.CAP, "cap.asy",
                SpicePorts.VOLTAGE, "voltage.asy", SpicePorts.E, "e.asy");
        var dependencies = new LinkedHashMap<String, String>();
        for (var component : model.getCircuitComponents()) {
            var descriptor = SpicePorts.find("LK", component.getType());
            String name = names.get(descriptor);
            if (name == null || dependencies.containsKey(name)) continue;
            byte[] symbol = Files.readAllBytes(symbols.resolve(name));
            checkSymbol(decode(symbol), descriptor);
            dependencies.put(name, ArtifactDigest.sha256(symbol));
        }
        model.setAcProvenance(new AcProvenance(ArtifactDigest.sha256(source), build.revision(), build.dirty(),
                build.sourceSnapshotSha256(), build.jarSha256(), dependencies));
        return model;
    }

    private static void checkSymbol(String text, SpicePorts descriptor) {
        var order = new HashMap<Integer, SpicePorts.Point>();
        SpicePorts.Point pin = null;
        for (String line : text.split("\\R")) {
            String[] fields = line.strip().split("\\s+");
            if (fields.length >= 3 && fields[0].equals("PIN")) {
                pin = new SpicePorts.Point(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            } else if (fields.length == 3 && fields[0].equals("PINATTR") && fields[1].equals("SpiceOrder")) {
                int number = Integer.parseInt(fields[2]);
                if (pin == null || order.putIfAbsent(number, pin) != null) {
                    throw new IllegalArgumentException("Invalid stock symbol pin order.");
                }
                pin = null;
            }
        }
        if (order.size() != descriptor.ports().size() || descriptor.ports().stream()
                .anyMatch(port -> !port.sourceLocalPoint().equals(order.get(port.order())))) {
            throw new IllegalArgumentException("The stock symbol pins differ from " + descriptor.name() + ".");
        }
    }

    /**
     * Imports a decoded schematic.
     * @param source the LTspice schematic text
     * @param sourceName the path or name used in diagnostics
     * @return the converted circuit and its import issues
     */
    public CircuitModel convert(String source, String sourceName) {
        if (source.isBlank() || source.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Require a nonempty LTspice schematic.");
        }
        var issues = new ArrayList<ImportIssue>();
        var symbols = new ArrayList<Symbol>();
        var wires = new ArrayList<Wire>();
        var flags = new ArrayList<Flag>();
        var directives = new ArrayList<Line>();
        parse(source, sourceName, symbols, wires, flags, directives, issues);
        List<PlacedSymbol> placed = placeSymbols(symbols, sourceName, issues);
        var markers = new LinkedHashMap<Point, ComponentData>();
        var topologyInput = new CircuitModel();
        var aliases = new LinkedHashMap<String, String>();
        for (Flag flag : flags) {
            markerAt(markers, topologyInput, flag.point);
            String label = flag.label.toLowerCase(Locale.ROOT);
            String alias = label.equals("0") || label.equals("gnd") ? "0" : "flag:" + label;
            aliases.putIfAbsent(alias, flag.label);
            addMarker(topologyInput, flag.point, alias);
        }
        for (PlacedSymbol symbol : placed) {
            for (Point pin : symbol.pins) markerAt(markers, topologyInput, pin);
        }
        for (Wire wire : wires) {
            markerAt(markers, topologyInput, wire.start);
            markerAt(markers, topologyInput, wire.end);
        }
        var wireVertices = new ArrayList<List<Point>>();
        for (Wire wire : wires) {
            List<Point> vertices = wire.vertices(markers.keySet());
            wireVertices.add(vertices);
            topologyInput.addConnection(connection(vertices, ""));
        }
        var topology = CircuitTopology.resolve(topologyInput);
        Map<Point, String> nodeKeys = new HashMap<>();
        Map<String, String> nodeLabels = nodeLabels(markers, topology, aliases, nodeKeys);
        Grid grid = Grid.fit(markers.keySet());
        var statements = new ArrayList<Line>();
        var placements = new HashMap<String, ComponentData>();
        var symbolByName = new HashMap<String, PlacedSymbol>();
        for (PlacedSymbol symbol : placed) {
            String name = symbol.symbol.name();
            placements.put(name, symbol.component(grid));
            symbolByName.put(name, symbol);
            String nodes = String.join(" ", symbol.pins.stream().map(nodeKeys::get).toList());
            statements.add(new Line(symbol.symbol.line, name + " " + nodes + " " + value(symbol.symbol)));
        }
        for (Line directive : directives) {
            statements.add(normalizeDirective(directive, sourceName, issues));
        }
        var model = new SpiceImporter().convert(sourceName, source.hashCode(), statements, nodeLabels, placements);
        issues.forEach(model::addConversionIssue);
        boolean ac = model.getAcSettings() != null;
        Layout layout = planLayout(grid, model, symbolByName, wireVertices, nodeKeys, nodeLabels, ac ? flags : null);
        layout.centers.forEach((component, center) -> {
            component.getPosition()[0] = center.x;
            component.getPosition()[1] = center.y;
        });
        layout.connections.forEach(model::addConnection);
        if (layout.conflict != null) {
            model.addConversionIssue(new ImportIssue("UNSUPPORTED_LAYOUT", "LTSPICE", null, layout.conflict, sourceName, true));
        }
        int bottom = markers.keySet().stream().map(layout.grid::position).mapToInt(Point::y).max().orElse(6);
        for (int i = 0; i < model.getControlComponents().size(); i++) {
            model.getControlComponents().get(i).getPosition()[1] = Math.addExact(bottom, 12 + i / 6 * 12);
        }
        return model;
    }

    private static Layout planLayout(Grid grid, CircuitModel model, Map<String, PlacedSymbol> symbols,
                                     List<List<Point>> wireVertices, Map<Point, String> nodeKeys, Map<String, String> nodeLabels,
                                     List<Flag> flags) {
        Layout original = flags == null ? layout(grid, model, symbols, wireVertices, nodeKeys, nodeLabels)
                : acLayout(grid, model, symbols, wireVertices, nodeKeys, flags);
        Layout candidate = original;
        // At this scale, a native terminal cannot reach a distinct source point
        // or a terminal from a distinct center.
        while (candidate.conflict != null && candidate.grid.multiplier < 8) {
            try {
                Grid finer = candidate.grid.refine();
                candidate = flags == null ? layout(finer, model, symbols, wireVertices, nodeKeys, nodeLabels)
                        : acLayout(finer, model, symbols, wireVertices, nodeKeys, flags);
            } catch (ArithmeticException error) {
                break;
            }
        }
        return candidate.conflict == null ? candidate : original;
    }

    private record Witness(ComponentData marker, String sourceGroup) { }

    private static Layout acLayout(Grid grid, CircuitModel model, Map<String, PlacedSymbol> symbols,
                                   List<List<Point>> wireVertices, Map<Point, String> nodeKeys, List<Flag> flags) {
        var drawing = new CircuitModel();
        var centers = new LinkedHashMap<ComponentData, Point>();
        var witnesses = new ArrayList<Witness>();
        var flagLabels = new LinkedHashMap<Point, String>();
        var flagSpelling = new HashMap<String, String>();
        for (var flag : flags) {
            String label = flag.label.toLowerCase(Locale.ROOT);
            flagLabels.put(grid.position(flag.point), label.equals("gnd") || label.equals("0") ? "0"
                    : flagSpelling.computeIfAbsent(label, key -> flag.label));
        }
        for (var vertices : wireVertices) {
            for (int i = 1; i < vertices.size(); i++) {
                Point a = grid.position(vertices.get(i - 1)), b = grid.position(vertices.get(i));
                acWire(drawing, flagLabels, a, b);
            }
        }
        for (ComponentData data : model.getCircuitComponents()) {
            var symbol = symbols.get(data.getName());
            Point center = symbol.center(grid);
            centers.put(data, center);
            var owned = data.getAcComponent();
            if (owned != null) {
                data.setAcComponent(new AcComponent(owned.instance(), owned.spiceOrders(), owned.aliases(),
                        symbol.mirrored(), owned.law()));
            }
            for (var port : symbol.type.nativePorts()) {
                Point sourcePin = symbol.pins.get(port.order() - 1);
                Point pin = grid.position(sourcePin);
                var offset = port.nativeOffset(data.getOrientation(), symbol.mirrored());
                Point nativePin = new Point(center.x + offset.x(), center.y + offset.y());
                witnesses.add(new Witness(addMarker(drawing, nativePin, ""), nodeKeys.get(sourcePin)));
                if (nativePin.equals(pin)) continue;
                // Extend a sense lead out of the component before changing its row.
                boolean vertical = data.getOrientation() == 501 || data.getOrientation() == 503;
                Point elbow = vertical == (port.order() >= 3)
                        ? new Point(pin.x, nativePin.y) : new Point(nativePin.x, pin.y);
                if (!nativePin.equals(elbow) && !elbow.equals(pin)) {
                    String label = flagLabels.remove(pin);
                    drawing.addConnection(label == null ? connection(List.of(nativePin, elbow, pin), "")
                            : connection(List.of(pin, elbow, nativePin), label));
                } else {
                    acWire(drawing, flagLabels, nativePin, pin);
                }
            }
        }
        // A flag at a bare pin can remain visible on a short local wire.
        flagLabels.forEach((point, label) -> drawing.addConnection(connection(
                List.of(point, new Point(point.x, point.y - 1)), label)));
        var resolved = CircuitTopology.resolve(drawing);
        var forward = new HashMap<String, Integer>();
        var reverse = new HashMap<Integer, String>();
        for (var witness : witnesses) {
            int id = resolved.group(witness.marker, Side.INPUT, 0).id();
            Integer previousId = forward.putIfAbsent(witness.sourceGroup, id);
            String previousGroup = reverse.putIfAbsent(id, witness.sourceGroup);
            if (previousId != null && previousId != id || previousGroup != null && !previousGroup.equals(witness.sourceGroup)) {
                return new Layout(grid, centers, drawing.getConnections(), "The native layout changes the source terminal partition.");
            }
        }
        return new Layout(grid, centers, drawing.getConnections(), null);
    }

    private static void acWire(CircuitModel drawing, Map<Point, String> flags, Point a, Point b) {
        String label = flags.remove(a);
        if (label != null) {
            drawing.addConnection(connection(List.of(a, b), label));
        } else {
            label = flags.remove(b);
            drawing.addConnection(label == null ? connection(List.of(a, b), "") : connection(List.of(b, a), label));
        }
    }

    private static Layout layout(Grid grid, CircuitModel model, Map<String, PlacedSymbol> symbols,
                                 List<List<Point>> wireVertices, Map<Point, String> nodeKeys, Map<String, String> nodeLabels) {
        var drawing = new CircuitModel();
        var centers = new LinkedHashMap<ComponentData, Point>();
        var nets = new LinkedHashMap<String, ComponentData>();
        for (List<Point> vertices : wireVertices) {
            String label = nodeLabels.get(nodeKeys.get(vertices.getFirst()));
            drawing.addConnection(connection(vertices.stream().map(grid::position).toList(), label));
            nets.computeIfAbsent(label, key -> addMarker(drawing, key));
        }
        for (ComponentData data : model.getCircuitComponents()) {
            PlacedSymbol symbol = symbols.get(data.getName());
            Point center = symbol.center(grid);
            centers.put(data, center);
            for (var port : symbol.type.nativePorts()) {
                var labels = port.side() == Side.INPUT ? data.getTerminalXLabels() : data.getTerminalYLabels();
                addLead(drawing, nets, center, grid.position(symbol.pins.get(port.order() - 1)), labels[port.port()]);
            }
        }
        // Output labels encode source nets. A rendered group must contain only one source net.
        var resolved = CircuitTopology.resolve(drawing);
        var labels = new HashMap<Integer, String>();
        for (var net : nets.entrySet()) {
            int group = resolved.group(net.getValue(), Side.INPUT, 0).id();
            String previous = labels.putIfAbsent(group, net.getKey());
            if (previous != null) {
                return new Layout(grid, centers, drawing.getConnections(),
                        "The native layout joins independent nets " + previous + " and " + net.getKey() + ".");
            }
        }
        return new Layout(grid, centers, drawing.getConnections(), null);
    }

    private static void parse(String source, String sourceName, List<Symbol> symbols, List<Wire> wires,
                              List<Flag> flags, List<Line> directives, List<ImportIssue> issues) {
        Symbol active = null;
        int line = 0;
        for (String raw : source.split("\\R")) {
            line++;
            String text = raw.strip();
            if (line == 1 && text.startsWith("\uFEFF")) text = text.substring(1).strip();
            if (text.isEmpty()) continue;
            String[] fields = text.split("\\s+");
            String record = fields[0].toUpperCase(Locale.ROOT);
            if (!record.equals("WINDOW") && !record.equals("SYMATTR")) active = null;
            try {
                switch (record) {
                    case "SYMBOL" -> {
                        requireLength(fields, 5);
                        active = new Symbol(line, fields[1], point(fields, 2), fields[4], new HashMap<>());
                        symbols.add(active);
                    }
                    case "SYMATTR" -> {
                        String[] attribute = text.split("\\s+", 3);
                        requireLength(attribute, 3);
                        if (active == null) throw new IllegalArgumentException("Place the attribute after a symbol.");
                        active.attributes.put(attribute[1].toLowerCase(Locale.ROOT), attribute[2]);
                    }
                    case "WIRE" -> {
                        requireLength(fields, 5);
                        Point start = point(fields, 1), end = point(fields, 3);
                        if (start.equals(end)) throw new IllegalArgumentException("Use two different wire endpoints.");
                        wires.add(new Wire(start, end));
                    }
                    case "FLAG" -> {
                        requireLength(fields, 4);
                        flags.add(new Flag(point(fields, 1), fields[3]));
                    }
                    case "TEXT" -> addDirectives(text, line, sourceName, directives, issues);
                    default -> {
                        if (!DRAWING_RECORDS.contains(record)) {
                            issue(issues, "UNSUPPORTED_ASC_RECORD", record, "The schematic record is unsupported.", sourceName, line);
                        }
                    }
                }
            } catch (IllegalArgumentException error) {
                issue(issues, "INVALID_ASC_RECORD", record, error.getMessage(), sourceName, line);
            }
        }
    }

    private static void addDirectives(String text, int line, String sourceName, List<Line> directives,
                                      List<ImportIssue> issues) {
        String[] fields = text.split("\\s+", 6);
        requireLength(fields, 6);
        if (!fields[5].startsWith("!")) return;
        for (String part : fields[5].substring(1).split("\\\\n")) {
            String directive = part.split(";", 2)[0].strip().replace('μ', 'µ');
            if (directive.isEmpty() || directive.startsWith("*")) continue;
            if (directive.startsWith("+") && !directives.isEmpty()) {
                Line preceding = directives.removeLast();
                directives.add(new Line(preceding.number(), preceding.text() + " " + directive.substring(1)));
            } else if (!directive.startsWith(".")) {
                issue(issues, "UNSUPPORTED_TEXT_DIRECTIVE", "TEXT", "Use a dot directive in schematic text.", sourceName, line);
            } else {
                directives.add(new Line(line, directive));
            }
        }
    }

    private static Line normalizeDirective(Line directive, String sourceName, List<ImportIssue> issues) {
        if (!directive.text().split("\\s+", 2)[0].equalsIgnoreCase(".tran")) return directive;
        String[] arguments;
        try {
            arguments = SpiceImporter.arguments(directive.text().substring(5));
        } catch (IllegalArgumentException error) {
            return directive; // The SPICE importer records the malformed argument list.
        }
        var times = new ArrayList<String>();
        boolean hasOptions = false;
        for (String argument : arguments) {
            String option = argument.split("=", 2)[0].toLowerCase(Locale.ROOT);
            if (hasOptions || TRANSIENT_OPTIONS.contains(option) || argument.contains("=") || times.size() == 4) {
                hasOptions = true;
                issue(issues, "UNSUPPORTED_TRANSIENT_OPTION", ".tran", "The transient option is unsupported: " + argument,
                        sourceName, directive.number());
            } else times.add(argument);
        }
        if (times.size() == 1) times.addFirst("0");
        return new Line(directive.number(), ".tran " + String.join(" ", times));
    }

    private static List<PlacedSymbol> placeSymbols(List<Symbol> symbols, String sourceName, List<ImportIssue> issues) {
        var result = new ArrayList<PlacedSymbol>();
        var names = new HashSet<String>();
        for (Symbol symbol : symbols) {
            SpicePorts type;
            try {
                type = SpicePorts.valueOf(symbol.kind.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException error) {
                issue(issues, "UNSUPPORTED_SYMBOL", symbol.name(), "Unsupported LTspice symbol: " + symbol.kind,
                        sourceName, symbol.line);
                continue;
            }
            String name = symbol.attributes.get("instname");
            try {
                if (name == null || !name.matches("[A-Za-z0-9_$][A-Za-z0-9_.$:+#-]*")) {
                    throw new IllegalArgumentException("Require an instance name without spaces or special characters.");
                }
                Orientation orientation = Orientation.valueOf(symbol.rotation.toUpperCase(Locale.ROOT));
                List<Point> pins = type.ports().stream().map(SpicePorts.PortSpec::sourceLocalPoint)
                        .map(pin -> orientation.place(symbol.origin, new Point(pin.x(), pin.y()))).toList();
                if (!names.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Require a unique instance name.");
                if (symbol.attributes.containsKey("modelfile")
                        || symbol.attributes.containsKey("spicemodel")
                        || !Character.toString(type.designator()).equalsIgnoreCase(
                                symbol.attributes.getOrDefault("prefix", Character.toString(type.designator())))) {
                    issue(issues, "UNSUPPORTED_SYMBOL_ATTRIBUTE", name, "The symbol overrides its standard model or prefix.",
                            sourceName, symbol.line);
                    continue;
                }
                result.add(new PlacedSymbol(symbol, type, pins));
            } catch (IllegalArgumentException | ArithmeticException error) {
                issue(issues, "INVALID_ASC_SYMBOL", symbol.name(), error.getMessage(), sourceName, symbol.line);
            }
        }
        return result;
    }

    private static String value(Symbol symbol) {
        return String.join(" ", List.of(symbol.attributes.getOrDefault("value", ""),
                symbol.attributes.getOrDefault("value2", ""), symbol.attributes.getOrDefault("spiceline", ""),
                symbol.attributes.getOrDefault("spiceline2", ""))).strip().replace('μ', 'µ');
    }

    private static Map<String, String> nodeLabels(Map<Point, ComponentData> markers, CircuitTopology topology,
                                                   Map<String, String> aliases, Map<Point, String> keys) {
        var labels = new LinkedHashMap<String, String>();
        var reserved = new HashSet<String>();
        aliases.values().forEach(label -> reserved.add(label.toLowerCase(Locale.ROOT)));
        labels.put("0", "0");
        for (var entry : markers.entrySet()) {
            var group = topology.group(entry.getValue(), Side.INPUT, 0);
            String key = group.canonicalLabel().equals("0") ? "0" : "asc" + group.id();
            keys.put(entry.getKey(), key);
            if (labels.containsKey(key)) continue;
            String label = aliases.get(group.canonicalLabel());
            if (label == null || !label.matches("[A-Za-z0-9_.$:+#-]+")
                    || label.equalsIgnoreCase("gnd") || label.equals("NIX_NIX_NIX")) {
                label = "N" + group.id();
                while (!reserved.add(label.toLowerCase(Locale.ROOT))) label = "_" + label;
            }
            labels.put(key, label);
        }
        return labels;
    }

    private static void markerAt(Map<Point, ComponentData> markers, CircuitModel model, Point point) {
        markers.computeIfAbsent(point, key -> addMarker(model, key, ""));
    }

    private static ComponentData addMarker(CircuitModel model, Point point, String label) {
        var marker = addMarker(model, label);
        marker.getTerminalData().add(new CircuitModel.TerminalData("LK", 0, Side.INPUT, 0, point.x, point.y, true));
        return marker;
    }

    private static ComponentData addMarker(CircuitModel model, String label) {
        var marker = new ComponentData(0, "");
        marker.setTerminalXLabels(new String[]{label});
        model.addCircuitComponent(marker);
        return marker;
    }

    private static void addLead(CircuitModel drawing, Map<String, ComponentData> nets, Point center, Point pin, String label) {
        int dx = Integer.compare(pin.x, center.x), dy = Integer.compare(pin.y, center.y);
        Point nativePin = new Point(center.x + dx * 2, center.y + dy * 2);
        var terminal = addMarker(drawing, nativePin, label);
        nets.putIfAbsent(label, terminal);
        if (!nativePin.equals(pin)) drawing.addConnection(connection(List.of(nativePin, pin), label));
    }

    private static ConnectionData connection(List<Point> points, String label) {
        return new ConnectionData("LK", points.stream().map(Point::coordinates).toArray(int[][]::new), label);
    }

    private static Point point(String[] fields, int start) {
        return new Point(Integer.parseInt(fields[start]), Integer.parseInt(fields[start + 1]));
    }

    private static void requireLength(String[] fields, int expected) {
        if (fields.length != expected) throw new IllegalArgumentException("The schematic record has an incorrect field count.");
    }

    private static void issue(List<ImportIssue> issues, String code, String component, String message, String sourceName, int line) {
        issues.add(new ImportIssue(code, "LTSPICE", component, message, sourceName + ":" + line, true));
    }

    private static String decode(byte[] bytes) throws CharacterCodingException {
        if (bytes.length >= 2 && (bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xfe
                || bytes[0] == (byte) 0xfe && bytes[1] == (byte) 0xff)) {
            return StandardCharsets.UTF_16.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }
        int evenZeros = 0, oddZeros = 0;
        for (int i = 0; i < Math.min(32, bytes.length); i++) {
            if (bytes[i] == 0) {
                if (i % 2 == 0) evenZeros++;
                else oddZeros++;
            }
        }
        if (evenZeros >= 4 && oddZeros == 0) {
            return StandardCharsets.UTF_16BE.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }
        if (oddZeros >= 4 && evenZeros == 0) {
            return StandardCharsets.UTF_16LE.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException error) {
            return Charset.forName("windows-1252").newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }
    }
}

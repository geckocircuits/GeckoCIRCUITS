package gecko.core.io.importer;

import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.CircuitModel.ConnectionData;
import gecko.core.io.CircuitTopology;
import gecko.core.io.CircuitTopology.Side;
import gecko.core.io.CircuitHierarchy;
import gecko.core.io.HierarchyMetadataCodec;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.io.ImportIssue;
import gecko.core.io.ArtifactDigest;
import gecko.core.io.BuildIdentity;
import gecko.core.ac.AcInputOrigin;
import gecko.core.io.AcOriginMembership;
import gecko.core.io.AcProvenance;
import gecko.core.io.HierarchyIndex;
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
import java.util.Arrays;
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
    private record Pin(String name, int order, Point position) { }
    private record Call(Symbol symbol, Path schematic, List<Pin> pins) { }
    private record SheetImport(CircuitModel model, Grid grid, List<Call> calls, List<Flag> flags,
                               List<Wire> wires, List<PlacedSymbol> primitives) { }

    public record ImportResult(CircuitModel model, LtspiceImportReceipt receipt) { }

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
        return convert(path, symbols, List.of());
    }

    /** Imports an ASC hierarchy using explicit search roots for its source dependencies. */
    public CircuitModel convert(Path path, Path symbols, List<Path> sourceRoots) throws IOException {
        return convert(path, symbols, sourceRoots, AcInputOrigin.UNSELECTED_ASC);
    }

    /** Imports an ASC using an explicitly selected source profile. */
    public CircuitModel convert(Path path, Path symbols, List<Path> sourceRoots, AcInputOrigin origin) throws IOException {
        return convertWithReceipt(path, symbols, sourceRoots, origin).model();
    }

    /** Returns the native document and its source geometry mapping before any solve. */
    public ImportResult convertWithReceipt(Path path, Path symbols, List<Path> sourceRoots, AcInputOrigin origin)
            throws IOException {
        if (origin != AcInputOrigin.UNSELECTED_ASC && !origin.tested()
                || origin.tested() && !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".asc")) {
            throw new IllegalArgumentException("Select the LTspice profile only for an ASC file.");
        }
        var build = BuildIdentity.current();
        byte[] source = Files.readAllBytes(path);
        var dependencies = new LinkedHashMap<String, String>();
        String text = decode(source);
        var state = new HierarchyImport(sourceRoots, dependencies, origin);
        SheetImport rootSheet = sourceRoots.isEmpty()
                ? convertSheet(text, path.toString(), null, null, false, origin)
                : state.convert(path, text);
        CircuitModel model = rootSheet.model();
        if (model.getAcSettings() == null) throw new IllegalArgumentException("Stock-symbol verification requires an AC schematic.");
        var names = Map.of(SpicePorts.RES, "res.asy", SpicePorts.CAP, "cap.asy",
                SpicePorts.VOLTAGE, "voltage.asy", SpicePorts.E, "e.asy",
                SpicePorts.IND, "ind.asy", SpicePorts.CURRENT, "current.asy");
        var checkedStock = new HashSet<String>();
        for (var component : model.getCircuitComponents()) {
            var descriptor = SpicePorts.find("LK", component.getType());
            String name = descriptor == null ? null : names.get(descriptor);
            if (name == null || !checkedStock.add(name)) continue;
            Path symbolPath = symbols.resolve(name);
            byte[] symbol = Files.readAllBytes(symbolPath);
            checkSymbol(decode(symbol), descriptor, symbolPath);
            String digest = ArtifactDigest.sha256(symbol);
            String previous = dependencies.putIfAbsent(name, digest);
            if (previous != null && !previous.equals(digest)) {
                throw new IllegalArgumentException("Conflicting stock symbol source " + name);
            }
        }
        String sourceDigest = ArtifactDigest.sha256(source);
        if (origin.tested() && !model.getImportIssues().isEmpty()) {
            var issue = model.getImportIssues().getFirst();
            throw new IllegalArgumentException("The ASC profile requires a complete import. "
                    + issue.sourceLocation() + " " + issue.message());
        }
        var definitionSources = origin.tested() ? state.definitionSources : Map.<String, String>of();
        var members = origin.tested() ? AcOriginMembership.bind(model, new HierarchyIndex(model),
                origin, sourceDigest, definitionSources) : Map.<Long, String>of();
        model.setAcProvenance(new AcProvenance(sourceDigest, build.revision(), build.dirty(),
                build.sourceSnapshotSha256(), build.jarSha256(), dependencies, origin, definitionSources, members));
        return new ImportResult(model, origin.tested() ? receipt(rootSheet, sourceDigest, origin) : null);
    }

    private static LtspiceImportReceipt receipt(SheetImport root, String sourceDigest, AcInputOrigin origin) {
        var byName = new HashMap<String, ComponentData>();
        for (var nativeComponent : root.model().getCircuitComponents()) {
            if (HierarchyIndex.parent(nativeComponent.getSavedBlock()) == 0) {
                byName.put(nativeComponent.getName(), nativeComponent);
            }
        }
        var devices = new ArrayList<LtspiceImportReceipt.Device>();
        for (var placed : root.primitives()) {
            var symbol = placed.symbol();
            var component = byName.get(symbol.name());
            var pins = new ArrayList<LtspiceImportReceipt.Pin>();
            for (var port : placed.type().ports()) {
                Point transformed = placed.pins().get(port.order() - 1);
                Point gridPoint = root.grid().position(transformed);
                var offset = port.nativeOffset(component.getOrientation(), placed.mirrored());
                var position = component.getPosition();
                pins.add(new LtspiceImportReceipt.Pin(port.order(),
                        new LtspiceImportReceipt.Point(port.sourceLocalPoint().x(), port.sourceLocalPoint().y()),
                        receiptPoint(transformed), receiptPoint(gridPoint),
                        new LtspiceImportReceipt.Point(position[0] + offset.x(), position[1] + offset.y()),
                        port.side().name(), port.port()));
            }
            devices.add(new LtspiceImportReceipt.Device(symbol.line(), symbol.name(), placed.type().name(),
                    AcOriginMembership.nativeIdentifier(component), receiptPoint(symbol.origin()), symbol.rotation(), pins));
        }
        var wires = root.wires().stream().map(wire -> new LtspiceImportReceipt.Wire(receiptPoint(wire.start()),
                receiptPoint(wire.end()), receiptPoint(root.grid().position(wire.start())),
                receiptPoint(root.grid().position(wire.end())))).toList();
        var flags = root.flags().stream().map(flag -> new LtspiceImportReceipt.Flag(flag.label(),
                receiptPoint(flag.point()), receiptPoint(root.grid().position(flag.point())))).toList();
        return new LtspiceImportReceipt(sourceDigest, origin.profileId(), origin.configurationSha256(), devices, wires, flags);
    }

    private static LtspiceImportReceipt.Point receiptPoint(Point point) {
        return new LtspiceImportReceipt.Point(point.x(), point.y());
    }

    private static void checkSymbol(String text, SpicePorts descriptor, Path path) {
        var order = new HashMap<Integer, SpicePorts.Point>();
        SpicePorts.Point pin = null;
        int lineNumber = 0;
        for (String line : text.split("\\R")) {
            lineNumber++;
            checkSymbolAttribute(line, path, lineNumber, Character.toString(descriptor.designator()), true);
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
        return convertSheet(source, sourceName, null, null, false, AcInputOrigin.UNSELECTED_ASC).model();
    }

    private SheetImport convertSheet(String source, String sourceName, Path path, HierarchyImport hierarchy, boolean child,
                                     AcInputOrigin origin) {
        if (source.isBlank() || source.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Require a nonempty LTspice schematic.");
        }
        var issues = new ArrayList<ImportIssue>();
        var symbols = new ArrayList<Symbol>();
        var wires = new ArrayList<Wire>();
        var flags = new ArrayList<Flag>();
        var directives = new ArrayList<Line>();
        parse(source, sourceName, symbols, wires, flags, directives, issues, origin);
        List<Call> calls = hierarchy == null ? List.of() : hierarchy.resolveCalls(path, symbols, sourceName, issues);
        if (hierarchy != null && (child || !calls.isEmpty())) {
            var namesAtPoint = new HashMap<Point, String>();
            for (Flag flag : flags) {
                String name = flag.label().toLowerCase(Locale.ROOT);
                if (name.startsWith("$g_")) {
                    throw new IllegalArgumentException("Unsupported non-ground global " + flag.label() + " in " + sourceName);
                }
                String existing = namesAtPoint.putIfAbsent(flag.point(), name);
                if (existing != null && !existing.equals(name)) {
                    throw new IllegalArgumentException("Conflicting FLAG labels at " + flag.point() + " in " + sourceName);
                }
            }
        }
        List<PlacedSymbol> placed = placeSymbols(symbols, sourceName, issues, origin);
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
        var anchors = new ArrayList<>(calls.stream().flatMap(call -> call.pins().stream()).map(Pin::position).toList());
        if (hierarchy != null && (child || !calls.isEmpty())) flags.forEach(flag -> anchors.add(flag.point()));
        anchors.forEach(anchor -> markerAt(markers, topologyInput, anchor));
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
            String name = directive.text().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
            if (hierarchy != null && (name.equals(".model") || child && !name.equals(".param"))) {
                issue(issues, "UNSUPPORTED_HIERARCHY_DIRECTIVE", name,
                        "This directive is unsupported in an ASC hierarchy.", sourceName, directive.number());
            } else {
                statements.add(normalizeDirective(directive, sourceName, issues));
            }
        }
        if (child) statements.add(new Line(0, ".ac dec 1 1 10"));
        var model = new SpiceImporter().convert(sourceName, source.hashCode(), statements, nodeLabels, placements, origin);
        issues.forEach(model::addConversionIssue);
        boolean ac = model.getAcSettings() != null;
        Layout layout = planLayout(grid, model, symbolByName, wireVertices, nodeKeys, nodeLabels, ac ? flags : null, anchors);
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
        return new SheetImport(model, layout.grid(), calls, flags, wires, placed);
    }

    private static Layout planLayout(Grid grid, CircuitModel model, Map<String, PlacedSymbol> symbols,
                                     List<List<Point>> wireVertices, Map<Point, String> nodeKeys, Map<String, String> nodeLabels,
                                     List<Flag> flags, List<Point> anchors) {
        Layout original = flags == null ? layout(grid, model, symbols, wireVertices, nodeKeys, nodeLabels)
                : acLayout(grid, model, symbols, wireVertices, nodeKeys, flags, anchors);
        Layout candidate = original;
        // At this scale, a native terminal cannot reach a distinct source point
        // or a terminal from a distinct center.
        while (candidate.conflict != null && candidate.grid.multiplier < 8) {
            try {
                Grid finer = candidate.grid.refine();
                candidate = flags == null ? layout(finer, model, symbols, wireVertices, nodeKeys, nodeLabels)
                        : acLayout(finer, model, symbols, wireVertices, nodeKeys, flags, anchors);
            } catch (ArithmeticException error) {
                break;
            }
        }
        return candidate.conflict == null ? candidate : original;
    }

    private record Witness(ComponentData marker, String sourceGroup) { }

    private static Layout acLayout(Grid grid, CircuitModel model, Map<String, PlacedSymbol> symbols,
                                   List<List<Point>> wireVertices, Map<Point, String> nodeKeys, List<Flag> flags, List<Point> anchors) {
        var drawing = new CircuitModel();
        var centers = new LinkedHashMap<ComponentData, Point>();
        var witnesses = new ArrayList<Witness>();
        for (var anchor : anchors) {
            witnesses.add(new Witness(addMarker(drawing, grid.position(anchor), ""), nodeKeys.get(anchor)));
        }
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
                              List<Flag> flags, List<Line> directives, List<ImportIssue> issues, AcInputOrigin origin) {
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
                        String key = attribute[1].toLowerCase(Locale.ROOT);
                        if (origin.tested() && active.kind().equalsIgnoreCase("ind")
                                && active.attributes.containsKey(key)) {
                            throw new IllegalArgumentException("Use each inductor attribute once.");
                        }
                        active.attributes.put(key, attribute[2]);
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

    private static List<PlacedSymbol> placeSymbols(List<Symbol> symbols, String sourceName,
                                                   List<ImportIssue> issues, AcInputOrigin origin) {
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
                if (origin.tested() && type == SpicePorts.IND && !Set.of(
                        "instname", "value", "value2", "spiceline", "spiceline2", "prefix")
                        .containsAll(symbol.attributes.keySet())) {
                    throw new IllegalArgumentException("Unsupported ASC inductor attribute.");
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

    private final class HierarchyImport {
        private final List<Path> sourceRoots;
        private final Map<String, String> dependencies;
        private final Map<String, String> definitionSources = new LinkedHashMap<>();
        private final AcInputOrigin origin;
        private final Map<Path, String> dependencyKeys = new HashMap<>();
        private final Map<String, CircuitHierarchy.Definition> definitions = new LinkedHashMap<>();
        private final Set<Path> active = new HashSet<>();
        private long nextIdentifier = 1_000_000;

        HierarchyImport(List<Path> sourceRoots, Map<String, String> dependencies, AcInputOrigin origin) {
            this.origin = origin;
            this.sourceRoots = List.copyOf(sourceRoots);
            this.dependencies = dependencies;
        }

        SheetImport convert(Path path, String source) throws IOException {
            Path root = path.toRealPath();
            active.add(root);
            var sheet = convertSheet(source, path.toString(), root, this, false, origin);
            CircuitModel document = sheet.model();
            if (!sheet.calls().isEmpty() && document.getAcSettings() == null) {
                throw new IllegalArgumentException("Select a root AC analysis in " + path);
            }
            for (var call : sheet.calls()) appendCall(document, sheet, call, 0, List.of());
            if (!definitions.isEmpty()) document.setHierarchy(new CircuitHierarchy(List.copyOf(definitions.values())));
            return sheet;
        }

        List<Call> resolveCalls(Path path, List<Symbol> symbols, String sourceName, List<ImportIssue> issues) {
            var calls = new ArrayList<Call>();
            var names = new HashSet<String>();
            for (var symbol : List.copyOf(symbols)) {
                boolean primitive;
                try {
                    SpicePorts.valueOf(symbol.kind().toUpperCase(Locale.ROOT));
                    primitive = true;
                } catch (IllegalArgumentException error) {
                    primitive = false;
                }
                if (!names.add(symbol.name().toLowerCase(Locale.ROOT))) {
                    issue(issues, "DUPLICATE_INSTANCE", symbol.name(), "Require a unique instance name.", sourceName, symbol.line());
                }
                if (primitive) continue;
                symbols.remove(symbol);
                try {
                    if (!symbol.attributes().containsKey("instname")
                            || symbol.attributes().containsKey("spicemodel") || symbol.attributes().containsKey("modelfile")
                            || !symbol.attributes().getOrDefault("prefix", "X").equalsIgnoreCase("X")) {
                        throw new IllegalArgumentException("Require a native ASC block call.");
                    }
                    Path asy = resolveDependency(path, symbol.kind(), ".asy");
                    Path asc = resolveDependency(path, symbol.kind(), ".asc");
                    if (!asy.getParent().equals(asc.getParent())) {
                        throw new IllegalArgumentException("Require the child ASC and ASY in the same directory.");
                    }
                    byte[] raw = Files.readAllBytes(asy);
                    recordDependency(asy, symbol.kind() + ".asy", raw);
                    var orientation = Orientation.valueOf(symbol.rotation().toUpperCase(Locale.ROOT));
                    var pins = readPins(decode(raw), asy).stream().map(pin -> new Pin(pin.name(), pin.order(),
                            orientation.place(symbol.origin(), pin.position()))).toList();
                    calls.add(new Call(symbol, asc, pins));
                } catch (IOException | IllegalArgumentException | ArithmeticException error) {
                    issue(issues, "INVALID_HIERARCHY_CALL", symbol.name(), error.getMessage(), sourceName, symbol.line());
                }
            }
            return calls;
        }

        private Path resolveDependency(Path caller, String literal, String extension) throws IOException {
            String normalized = literal.replace((char) 92, '/');
            String[] segments = (normalized + extension).split("/+");
            if (normalized.startsWith("/") || Arrays.stream(segments)
                    .anyMatch(segment -> segment.equals(".") || segment.equals("..") || segment.contains(":"))) {
                throw new IllegalArgumentException("Require a relative ASC symbol path: " + literal);
            }
            var matches = new java.util.LinkedHashSet<Path>();
            var roots = new ArrayList<>(sourceRoots);
            roots.add(caller.getParent());
            for (var root : roots) {
                Path cursor = root;
                for (String segment : segments) {
                    if (!Files.isDirectory(cursor)) { cursor = null; break; }
                    try (var entries = Files.list(cursor)) {
                        var named = entries.filter(entry -> entry.getFileName().toString().equalsIgnoreCase(segment)).toList();
                        if (named.size() > 1) throw new IllegalArgumentException("Ambiguous source path segment: " + segment);
                        cursor = named.isEmpty() ? null : named.getFirst();
                    }
                    if (cursor == null) break;
                }
                if (cursor != null && Files.isRegularFile(cursor)) matches.add(cursor.toRealPath());
            }
            if (matches.size() != 1) throw new IllegalArgumentException("Require one source match for " + literal + extension);
            return matches.getFirst();
        }

        private void appendCall(CircuitModel document, SheetImport parent, Call call, long parentId, List<String> parentPath)
                throws IOException {
            var path = new ArrayList<>(parentPath);
            path.add(call.symbol().name());
            String context = call.schematic() + " " + path;
            if (!active.add(call.schematic())) {
                throw new IllegalArgumentException("Recursive ASC definition at " + context
                        + " called from " + parent.model().getFilePath() + ":" + call.symbol().line());
            }
            try {
                byte[] childSource = Files.readAllBytes(call.schematic());
                recordDependency(call.schematic(), call.symbol().kind() + ".asc", childSource);
                var child = convertSheet(decode(childSource), context, call.schematic(), this, true, origin);
                child.model().getConversionIssues().forEach(document::addConversionIssue);
                String definitionId = call.schematic().toString();
                definitionSources.putIfAbsent(definitionId, ArtifactDigest.sha256(childSource));
                var formals = call.pins().stream().map(pin -> new CircuitHierarchy.Formal(pin.order(), pin.name(),
                        "formal:" + pin.name().toLowerCase(Locale.ROOT))).toList();
                var definition = new CircuitHierarchy.Definition(definitionId, call.schematic().toString(), formals,
                        child.model().getAcSettings().parameters());
                var existing = definitions.putIfAbsent(definitionId, definition);
                if (existing != null && !existing.equals(definition)) {
                    throw new IllegalArgumentException("Conflicting ASC interfaces in " + context);
                }
                String attributes = value(call.symbol());
                var occurrence = new CircuitHierarchy.Occurrence(definitionId, call.symbol().name(),
                        SpiceImporter.parameterAssignments(attributes));
                long id = nextIdentifier++;
                var placedPins = call.pins().stream().map(Pin::position).map(parent.grid()::position).toList();
                int minX = placedPins.stream().mapToInt(Point::x).min().orElseThrow();
                int maxX = placedPins.stream().mapToInt(Point::x).max().orElseThrow();
                int minY = placedPins.stream().mapToInt(Point::y).min().orElseThrow();
                int maxY = placedPins.stream().mapToInt(Point::y).max().orElseThrow();
                int x = minX + 1, y = minY - 1;
                var block = new ComponentData(27, occurrence.instance(), x, y, 503);
                var blockTokens = new LinkedHashMap<String, String>();
                blockTokens.put(HierarchyMetadataCodec.OCCURRENCE, HierarchyMetadataCodec.encode(occurrence));
                blockTokens.put("blockSizeX", Integer.toString(Math.max(2, maxX - minX - 1)));
                blockTokens.put("blockSizeY", Integer.toString(Math.max(2, maxY - minY + 1)));
                blockTokens.put("worksheetSizeX", Integer.toString(sheetExtent(child, 0)));
                blockTokens.put("worksheetSizeY", Integer.toString(sheetExtent(child, 1)));
                if (parentId != 0) member(blockTokens, "call:" + occurrence.instance().toLowerCase(Locale.ROOT));
                block.setSavedBlock(saved("ElementSPECIAL", id, parentId, blockTokens));
                document.getSpecialComponents().add(block);

                for (var component : child.model().getCircuitComponents()) {
                    component.getImportTokens().remove("uniqueObjectIdentifier");
                    var tokens = new LinkedHashMap<String, String>();
                    member(tokens, "component:" + component.getName().toLowerCase(Locale.ROOT));
                    component.setSavedBlock(saved("ElementLK", nextIdentifier++, id, tokens));
                    document.addCircuitComponent(component);
                }
                int wireNumber = 0;
                for (var wire : child.model().getConnections()) {
                    var tokens = new LinkedHashMap<String, String>();
                    member(tokens, "wire:" + ++wireNumber);
                    wire.setSavedBlock(saved("Verbindung", nextIdentifier++, id, tokens));
                    document.addConnection(wire);
                }
                for (int i = 0; i < formals.size(); i++) {
                    var formal = formals.get(i);
                    var flag = child.flags().stream().filter(item -> item.label().equalsIgnoreCase(formal.name()))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                    "Missing named FLAG " + formal.name() + " in " + context));
                    if (child.flags().stream().anyMatch(item -> item.point().equals(flag.point())
                            && !item.label().equalsIgnoreCase(flag.label()))) {
                        throw new IllegalArgumentException("Conflicting FLAG labels at formal " + formal.name() + " in " + context);
                    }
                    var childPin = child.grid().position(flag.point());
                    var parentPin = placedPins.get(i);
                    var terminal = new ComponentData(29, formal.name(), childPin.x(), childPin.y(), 503);
                    terminal.setTerminalXLabels(new String[]{""});
                    terminal.setTerminalYLabels(new String[]{""});
                    var tokens = new LinkedHashMap<String, String>();
                    member(tokens, formal.boundaryMemberKey());
                    tokens.put(HierarchyMetadataCodec.BOUNDARY_LOCATION,
                            HierarchyMetadataCodec.encodeBoundaryLocation(parentPin.x() - x, parentPin.y() - y));
                    tokens.put("terminalLocation", Integer.toString(parentPin.x() == minX ? 1 : parentPin.x() == maxX
                            ? 2 : parentPin.y() == minY ? 0 : 3));
                    terminal.setSavedBlock(saved("ElementLK", nextIdentifier++, id, tokens));
                    document.addCircuitComponent(terminal);
                }
                for (var nested : child.calls()) appendCall(document, child, nested, id, path);
            } finally {
                active.remove(call.schematic());
            }
        }

        private void recordDependency(Path resolved, String literal, byte[] content) throws IOException {
            String key = resolved.toString();
            for (int i = 0; i < sourceRoots.size(); i++) {
                Path root = sourceRoots.get(i).toRealPath();
                if (!resolved.startsWith(root)) continue;
                String relative = root.relativize(resolved).toString().replace((char) 92, '/');
                String spelled = literal.replace((char) 92, '/').replaceAll("/+", "/");
                if (relative.equalsIgnoreCase(spelled)) relative = literal;
                key = (sourceRoots.size() > 1 ? "source-root-" + (i + 1) + "/" : "") + relative;
                break;
            }
            String existing = dependencyKeys.putIfAbsent(resolved, key);
            if (existing != null) key = existing;
            String hash = ArtifactDigest.sha256(content);
            String previous = dependencies.putIfAbsent(key, hash);
            if (previous != null && !previous.equals(hash)) {
                throw new IllegalArgumentException("Conflicting source dependency " + key);
            }
        }

        private int sheetExtent(SheetImport child, int axis) {
            var points = new ArrayList<int[]>();
            child.model().getCircuitComponents().forEach(component -> points.add(component.getPosition()));
            child.model().getConnections().forEach(wire -> points.addAll(Arrays.asList(wire.getPoints())));
            child.flags().forEach(flag -> points.add(child.grid().position(flag.point()).coordinates()));
            child.calls().forEach(call -> call.pins().forEach(pin -> points.add(child.grid().position(pin.position()).coordinates())));
            return points.stream().mapToInt(point -> point[axis]).max().orElse(40) + 16;
        }

        private SavedCircuitBlock saved(String tag, long id, long parent, Map<String, String> tokens) {
            tokens.put("uniqueObjectIdentifier", Long.toString(id));
            tokens.put("parentSheetIdentifier", Long.toString(parent));
            return SavedCircuitBlock.parse(tag, tokens.entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue()).toArray(String[]::new));
        }

        private void member(Map<String, String> tokens, String key) {
            tokens.put(HierarchyMetadataCodec.MEMBER, HierarchyMetadataCodec.encode(new CircuitHierarchy.Member(key)));
        }
    }

    private static List<Pin> readPins(String source, Path path) {
        var pins = new ArrayList<Pin>();
        Point point = null;
        String name = null;
        Integer order = null;
        boolean block = false;
        int lineNo = 0;
        try {
            for (String line : source.split("\\R")) {
                lineNo++;
                checkSymbolAttribute(line, path, lineNo, "X", false);
                String[] fields = line.strip().split("\\s+", 3);
                if (fields[0].equalsIgnoreCase("SymbolType") && fields.length == 2) {
                    block = fields[1].equalsIgnoreCase("BLOCK");
                } else if (fields[0].equalsIgnoreCase("PIN")) {
                    if (point != null) pins.add(requirePin(point, name, order));
                    point = point(line.strip().split("\\s+"), 1);
                    order = null;
                    name = null;
                } else if (fields[0].equalsIgnoreCase("PINATTR") && fields.length == 3) {
                    if (fields[1].equalsIgnoreCase("PinName")) {
                        if (name != null || point == null) throw new IllegalArgumentException("Duplicate or misplaced PinName.");
                        name = fields[2];
                    }
                    if (fields[1].equalsIgnoreCase("SpiceOrder")) {
                        if (order != null || point == null) throw new IllegalArgumentException("Duplicate or misplaced SpiceOrder.");
                        order = Integer.parseInt(fields[2]);
                    }
                }
            }
            if (point != null) pins.add(requirePin(point, name, order));
            pins.sort(Comparator.comparingInt(Pin::order));
            var names = new HashSet<String>();
            var positions = new HashSet<Point>();
            for (int i = 0; i < pins.size(); i++) {
                var pin = pins.get(i);
                if (pin.order() != i + 1 || !names.add(pin.name().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Require unique contiguous SpiceOrder and unique PinName.");
                }
                if (!positions.add(pin.position())) throw new IllegalArgumentException("Duplicate ASY pin position.");
            }
            if (!block || pins.isEmpty()) throw new IllegalArgumentException("Require an ASY block with formal pins.");
            return List.copyOf(pins);
        } catch (IllegalArgumentException | IndexOutOfBoundsException error) {
            throw new IllegalArgumentException(path + ":" + lineNo + " " + error.getMessage(), error);
        }
    }

    private static Pin requirePin(Point point, String name, Integer order) {
        if (name == null || order == null) throw new IllegalArgumentException("Require PinName and SpiceOrder on each pin.");
        return new Pin(name, order, point);
    }

    private static void checkSymbolAttribute(String line, Path path, int lineNumber, String prefix, boolean stock) {
        String[] fields = line.strip().split("\\s+", 3);
        if (!fields[0].equalsIgnoreCase("SYMATTR")) return;
        if (fields.length == 3 && (fields[1].equalsIgnoreCase("Description")
                || fields[1].equalsIgnoreCase("Prefix") && fields[2].equalsIgnoreCase(prefix)
                || stock && fields[1].equalsIgnoreCase("Value") && fields[2].equalsIgnoreCase(prefix))) return;
        throw new IllegalArgumentException(path + ":" + lineNumber + " Unsupported ASY electrical attribute " + line.strip());
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

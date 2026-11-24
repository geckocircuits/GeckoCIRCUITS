package gecko.core.io;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Resolves terminal identities and signal groups independently of frontend objects. */
public final class CircuitTopology {
    public enum Side { INPUT, OUTPUT }
    /** A nonzero saved identifier takes precedence over the source ordinal. */
    public record ComponentIdentity(String domain, long uniqueIdentifier, int fallbackOrdinal,
                                    ComponentIdentity parent, String role) {
        public ComponentIdentity(String domain, long uniqueIdentifier, int fallbackOrdinal) {
            this(domain, uniqueIdentifier, fallbackOrdinal, null, "");
        }
        /**
         * Creates a canonical child identity without using a historical name or identifier.
         * @param childRole the stable role within this parent
         * @return the parent and role identity
         * @throws IllegalArgumentException if the role is absent or blank
         */
        public ComponentIdentity child(String childRole) {
            if (childRole == null || childRole.isBlank()) throw new IllegalArgumentException("A child role is required");
            return new ComponentIdentity(domain, 0, -1, this, childRole);
        }
    }
    /** Historical names and identifiers do not define the canonical child identity. */
    public record HiddenPrimitive(int type, String legacyName, long legacyIdentifier, List<Double> numericParameters) {
        public HiddenPrimitive { numericParameters = List.copyOf(numericParameters); }
    }
    public record HiddenComponent(ComponentIdentity parent, String role, HiddenPrimitive primitive,
                                  TerminalIdentity input, TerminalIdentity output, String controlTargetRole) {
        /** @return the canonical parent and role identity */
        public ComponentIdentity identity() { return parent.child(role); }
        /** @return the target identity within the same parent */
        public ComponentIdentity controlTarget() { return parent.child(controlTargetRole); }
    }
    public record TerminalIdentity(ComponentIdentity component, Side side, int port) { }
    public record Group(int id, String domain, String canonicalLabel, List<TerminalIdentity> terminals,
                        List<TerminalIdentity> producerTerminals) {
        public Group {
            terminals = List.copyOf(terminals);
            producerTerminals = List.copyOf(producerTerminals);
        }
    }

    private record Alias(String domain, long sheet, String label) { }
    private record Port(TerminalIdentity id, String domain, long sheet, String label, boolean producer,
                        CircuitModel.TerminalData geometry, boolean externalInput, int componentOrder) { }
    private record Position(String domain, long sheet, int x, int y) { }
    private record Wire(CircuitModel.ConnectionData data, String domain, long sheet) { }
    private final Map<CircuitModel.ComponentData, ComponentIdentity> identities;
    private final Map<TerminalIdentity, Integer> terminalGroups;
    private final Map<TerminalIdentity, Integer> externalOutputGroups;
    private final Map<Integer, Group> groups;
    private final Map<Alias, Integer> labelGroups;
    private final List<ComponentIdentity> activeComponents;
    private final List<ComponentIdentity> structuralComponents;
    private final Set<CircuitModel.ConnectionData> activeConnections;
    private final Set<String> connectionDomains;
    private final List<HiddenComponent> hiddenComponents;
    private final List<String> physicalConnectionIssues;
    private final List<Integer> connectionGroupOrder;

    private CircuitTopology(Map<CircuitModel.ComponentData, ComponentIdentity> identities,
            Map<TerminalIdentity, Integer> terminalGroups, Map<TerminalIdentity, Integer> externalOutputGroups,
            Map<Integer, Group> groups, Map<Alias, Integer> labelGroups,
            List<ComponentIdentity> activeComponents, List<ComponentIdentity> structuralComponents, List<Wire> wires,
            List<String> physicalConnectionIssues, List<Integer> connectionGroupOrder) {
        this.labelGroups = Map.copyOf(labelGroups);
        this.connectionGroupOrder = List.copyOf(connectionGroupOrder);
        this.hiddenComponents = List.of();
        this.physicalConnectionIssues = List.copyOf(physicalConnectionIssues);
        this.identities = Map.copyOf(identities);
        this.terminalGroups = Map.copyOf(terminalGroups);
        this.externalOutputGroups = Map.copyOf(externalOutputGroups);
        this.groups = Map.copyOf(groups);
        this.activeComponents = List.copyOf(activeComponents);
        this.structuralComponents = List.copyOf(structuralComponents);
        this.activeConnections = wires.stream().map(Wire::data).collect(Collectors.toUnmodifiableSet());
        this.connectionDomains = wires.stream().map(Wire::domain).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Resolves active terminal groups without changing the source description.
     * @param model the circuit with saved hierarchy and terminal data
     * @return a detached topology description with immutable collections
     */
    public static CircuitTopology resolve(CircuitModel model) {
        for (var components : List.of(model.getCircuitComponents(), model.getControlComponents(),
                model.getThermalComponents(), model.getSpecialComponents())) {
            for (var component : components) {
                if (component == null) throw new IllegalArgumentException("Circuit component cannot be null");
            }
        }
        var identities = new IdentityHashMap<CircuitModel.ComponentData, ComponentIdentity>();
        var ports = new ArrayList<Port>();
        var active = new ArrayList<ComponentIdentity>();
        var structural = new ArrayList<ComponentIdentity>();
        var sheets = new LinkedHashMap<Long, SavedCircuitBlock>();
        model.getSpecialComponents().stream().filter(component -> component.getType() == 27)
                .map(CircuitModel.ComponentData::getSavedBlock).filter(java.util.Objects::nonNull)
                .forEach(block -> sheets.put(block.uniqueObjectIdentifier(), block));
        var wires = new ArrayList<Wire>();
        for (var connection : model.getConnections()) {
            var saved = connection.getSavedBlock();
            if (enabled(saved, sheets)) {
                int[][] points = connection.getPoints();
                if (points == null || points.length < 2) {
                    throw new IllegalArgumentException("Circuit wire requires at least two grid points");
                }
                for (int[] point : points) {
                    if (point == null || point.length != 2) {
                        throw new IllegalArgumentException("Circuit wire requires x/y grid points");
                    }
                }
                String domain = connection.getType();
                if (saved != null && "2".equals(saved.tokens().get("connectorType"))) domain = "RELUCTANCE";
                wires.add(new Wire(connection, domain, saved == null ? 0 : saved.parentSheetIdentifier()));
            }
        }
        boolean electricalGeometry = wires.stream().anyMatch(wire -> "LK".equals(wire.domain()) || "RELUCTANCE".equals(wire.domain()))
                && model.getCircuitComponents().stream().anyMatch(component -> twoPort(component.getType())
                    && (missingLabel(component, Side.INPUT) || missingLabel(component, Side.OUTPUT)));
        addComponents("LK", model.getCircuitComponents(), identities, ports, active, structural, sheets, false, electricalGeometry);
        addComponents("CONTROL", model.getControlComponents(), identities, ports, active, structural, sheets, false, false);
        addComponents("THERMAL", model.getThermalComponents(), identities, ports, active, structural, sheets, false, false);
        addComponents("SPECIAL", model.getSpecialComponents(), identities, ports, active, structural, sheets, false, false);
        for (var block : model.getStructuralComponents()) {
            addComponents(block.domain(), List.of(block.component()), identities, ports, active, structural, sheets, true,
                    electricalGeometry && block.domain().equals("LK"));
        }
        int[] parent = new int[ports.size() + wires.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        var aliases = new LinkedHashMap<Alias, Integer>();
        var positions = new LinkedHashMap<Position, List<Integer>>();
        for (int i = 0; i < ports.size(); i++) {
            Port port = ports.get(i);
            if (!port.label().isEmpty()) {
                Integer previous = aliases.putIfAbsent(new Alias(port.domain(), port.sheet(), port.label()), i);
                if (previous != null) union(parent, i, previous);
            }
            var geometry = port.geometry();
            if (geometry != null && geometry.geometric()) {
                positions.computeIfAbsent(new Position(port.domain(), port.sheet(), geometry.x(), geometry.y()),
                        key -> new ArrayList<>()).add(i);
            }
        }
        for (var colocated : positions.values()) {
            var ordinaryTerminals = colocated.stream().filter(index -> !ports.get(index).externalInput()).toList();
            if (ordinaryTerminals.size() > 1) {
                int first = ordinaryTerminals.getFirst();
                for (int index = 1; index < ordinaryTerminals.size(); index++) {
                    union(parent, first, ordinaryTerminals.get(index));
                }
            }
            var labelledRoots = new LinkedHashMap<Integer, Integer>();
            for (int terminal : colocated) {
                if (!ports.get(terminal).label().isEmpty()) labelledRoots.putIfAbsent(find(parent, terminal), terminal);
            }
            if (labelledRoots.size() == 1) {
                int labelledTerminal = labelledRoots.values().iterator().next();
                for (int terminal : colocated) {
                    if (ports.get(terminal).label().isEmpty()) union(parent, labelledTerminal, terminal);
                }
            } else if (labelledRoots.isEmpty() && !ordinaryTerminals.isEmpty()) {
                // Unlabelled abutting terminals share one potential, like desktop geometricOnSamePotential.
                for (int terminal : colocated) union(parent, ordinaryTerminals.getFirst(), terminal);
            }
        }
        var pointsToWires = new LinkedHashMap<Position, List<Integer>>();
        for (int i = 0; i < wires.size(); i++) {
            Wire wire = wires.get(i);
            int node = ports.size() + i;
            String label = normalizeLabel(wire.data().getLabel());
            if (!label.isEmpty()) {
                Integer previous = aliases.putIfAbsent(new Alias(wire.domain(), wire.sheet(), label), node);
                if (previous != null) union(parent, node, previous);
            }
            for (int[] point : wire.data().getPoints()) {
                var position = new Position(wire.domain(), wire.sheet(), point[0], point[1]);
                pointsToWires.computeIfAbsent(position, key -> new ArrayList<>()).add(node);
                for (int terminal : positions.getOrDefault(position, List.of())) union(parent, node, terminal);
            }
        }
        // Interior wire crossings join only at a wire endpoint or a component terminal.
        for (int i = 0; i < wires.size(); i++) {
            Wire wire = wires.get(i);
            int[][] points = wire.data().getPoints();
            for (int end : new int[]{0, points.length - 1}) {
                var position = new Position(wire.domain(), wire.sheet(), points[end][0], points[end][1]);
                for (int other : pointsToWires.get(position)) union(parent, ports.size() + i, other);
            }
        }
        var portIndices = new LinkedHashMap<TerminalIdentity, Integer>();
        for (int i = 0; i < ports.size(); i++) portIndices.put(ports.get(i).id(), i);
        var globals = new LinkedHashMap<Alias, Integer>();
        identities.forEach((component, identity) -> {
            if (boundary(component, identity.domain())) {
                Integer input = portIndices.get(new TerminalIdentity(identity, Side.INPUT, 0));
                Integer output = portIndices.get(new TerminalIdentity(identity, Side.OUTPUT, 0));
                if (input != null && output != null) union(parent, input, output);
            }
            if (physicalShort(component, identity.domain())) {
                int count = Math.min(portLabels(component, Side.INPUT).length, portLabels(component, Side.OUTPUT).length);
                for (int port = 0; port < count; port++) {
                    Integer input = portIndices.get(new TerminalIdentity(identity, Side.INPUT, port));
                    Integer output = portIndices.get(new TerminalIdentity(identity, Side.OUTPUT, port));
                    if (input != null && output != null && ports.get(input).domain().equals(ports.get(output).domain())) {
                        union(parent, input, output);
                    }
                }
            }
            if (global(component, identity.domain()) && component.getName() != null && !component.getName().isEmpty()) {
                Integer input = portIndices.get(new TerminalIdentity(identity, Side.INPUT, 0));
                if (input != null) {
                    Integer previous = globals.putIfAbsent(new Alias(ports.get(input).domain(), 0, component.getName()), input);
                    if (previous != null) union(parent, input, previous);
                }
            }
        });
        var roots = new LinkedHashMap<Integer, List<Port>>();
        for (int i = 0; i < ports.size(); i++) {
            roots.computeIfAbsent(find(parent, i), key -> new ArrayList<>()).add(ports.get(i));
        }
        var groups = new LinkedHashMap<Integer, Group>();
        var rootGroups = new LinkedHashMap<Integer, Integer>();
        var terminalGroups = new LinkedHashMap<TerminalIdentity, Integer>();
        var physicalConnectionIssues = new ArrayList<String>();
        for (var entry : roots.entrySet()) {
            var members = entry.getValue();
            int id = groups.size();
            String label = members.stream().map(Port::label).filter(value -> !value.isEmpty()).findFirst().orElse("");
            if (!members.getFirst().domain().equals("CONTROL") && members.stream().anyMatch(port -> ground(port.label()))) label = "0";
            boolean hasWire = false;
            for (int i = 0; i < wires.size(); i++) {
                if (find(parent, ports.size() + i) == entry.getKey()) {
                    hasWire = true;
                    String wireLabel = normalizeLabel(wires.get(i).data().getLabel());
                    if (label.isEmpty()) label = wireLabel;
                    if (!members.getFirst().domain().equals("CONTROL") && ground(wireLabel)) label = "0";
                }
            }
            var first = members.getFirst();
            if (members.size() == 1 && !hasWire && label.isEmpty() && first.geometry() != null
                    && first.geometry().geometric() && active.contains(first.id().component())
                    && (first.id().component().domain().equals("LK") || first.id().component().domain().equals("THERMAL"))) {
                String name = identities.entrySet().stream().filter(item -> item.getValue().equals(first.id().component()))
                        .map(item -> java.util.Objects.toString(item.getKey().getName(), "")).findFirst().orElse("");
                physicalConnectionIssues.add("Component '" + name + "' has an unconnected "
                        + (first.id().side() == Side.INPUT ? "input" : "output") + " terminal");
            }
            var group = new Group(id, members.getFirst().domain(), label,
                    members.stream().map(Port::id).toList(),
                    members.stream().filter(Port::producer).map(Port::id).toList());
            groups.put(id, group);
            rootGroups.put(entry.getKey(), id);
            members.forEach(port -> terminalGroups.put(port.id(), id));
        }
        var connectionOrder = new java.util.LinkedHashSet<Integer>();
        for (int i = 0; i < wires.size(); i++) {
            int root = find(parent, ports.size() + i);
            var members = roots.get(root);
            if (members != null) connectionOrder.add(terminalGroups.get(members.getFirst().id()));
        }
        var externalPortsByPosition = new LinkedHashMap<Position, List<Port>>();
        for (var port : ports) {
            var geometry = port.geometry();
            if (port.externalInput() && geometry != null && geometry.geometric()) {
                externalPortsByPosition.computeIfAbsent(new Position(port.domain(), port.sheet(), geometry.x(), geometry.y()),
                        key -> new ArrayList<>()).add(port);
            }
        }
        for (var colocated : externalPortsByPosition.values()) {
            if (colocated.size() < 2) continue;
            var ordered = colocated.stream().sorted(java.util.Comparator.comparingInt(Port::componentOrder)
                    .thenComparingInt(port -> port.id().port())).toList();
            Port firstExternal = ordered.stream().filter(port -> {
                Integer group = terminalGroups.get(port.id());
                return group != null && !groups.get(group).producerTerminals().isEmpty();
            }).findFirst().orElse(ordered.getFirst());
            Integer firstExternalGroup = terminalGroups.get(firstExternal.id());
            if (firstExternalGroup == null) continue;
            for (var external : ordered) {
                if (!external.id().equals(firstExternal.id())) terminalGroups.put(external.id(), firstExternalGroup);
            }
        }
        var updatedGroups = new LinkedHashMap<Integer, Group>();
        for (var entry : groups.entrySet()) {
            int groupId = entry.getKey();
            var members = new java.util.LinkedHashSet<TerminalIdentity>();
            for (var terminal : entry.getValue().terminals()) {
                if (java.util.Objects.equals(terminalGroups.get(terminal), groupId)) members.add(terminal);
            }
            terminalGroups.forEach((terminal, assignedGroup) -> {
                if (assignedGroup == groupId) members.add(terminal);
            });
            updatedGroups.put(groupId, new Group(entry.getValue().id(), entry.getValue().domain(),
                    entry.getValue().canonicalLabel(), new ArrayList<>(members), entry.getValue().producerTerminals()));
        }
        var paddedExternalTerminals = new HashSet<TerminalIdentity>();
        var externalOutputGroups = new LinkedHashMap<TerminalIdentity, Integer>();
        for (var component : model.getControlComponents()) {
            if (component.getType() != 22) continue;
            Object savedWidth = component.getParameters().get("activeInputWidth");
            if (!(savedWidth instanceof Number number)) continue;
            int activeWidth = Math.min(component.getTerminalXLabels().length,
                    Math.max(0, number.intValue()));
            for (int port = activeWidth; port < component.getTerminalXLabels().length; port++) {
                var terminal = new TerminalIdentity(identities.get(component), Side.INPUT, port);
                paddedExternalTerminals.add(terminal);
                Integer group = terminalGroups.get(terminal);
                if (group != null) externalOutputGroups.put(terminal, group);
            }
        }
        var publicTerminalGroups = new LinkedHashMap<TerminalIdentity, Integer>(terminalGroups);
        paddedExternalTerminals.forEach(publicTerminalGroups::remove);
        var publicGroups = new LinkedHashMap<Integer, Group>();
        for (var entry : updatedGroups.entrySet()) {
            var members = entry.getValue().terminals().stream()
                    .filter(terminal -> !paddedExternalTerminals.contains(terminal)).toList();
            var producers = entry.getValue().producerTerminals().stream()
                    .filter(terminal -> !paddedExternalTerminals.contains(terminal)).toList();
            publicGroups.put(entry.getKey(), new Group(entry.getValue().id(), entry.getValue().domain(),
                    entry.getValue().canonicalLabel(), members, producers));
        }
        var labelGroups = new LinkedHashMap<Alias, Integer>();
        aliases.forEach((alias, index) -> {
            Integer group = rootGroups.get(find(parent, index));
            if (group != null) labelGroups.put(alias, group);
        });
        return new CircuitTopology(identities, publicTerminalGroups, externalOutputGroups, publicGroups, labelGroups, active,
                structural, wires,
                physicalConnectionIssues, new ArrayList<>(connectionOrder));
    }

    private CircuitTopology(CircuitTopology source, Map<TerminalIdentity, Integer> terminalGroups,
            Map<Integer, Group> groups, List<ComponentIdentity> activeComponents, List<HiddenComponent> children) {
        this.identities = source.identities;
        this.connectionGroupOrder = source.connectionGroupOrder;
        this.terminalGroups = Map.copyOf(terminalGroups);
        this.externalOutputGroups = source.externalOutputGroups;
        this.groups = Map.copyOf(groups);
        this.labelGroups = source.labelGroups;
        this.activeComponents = List.copyOf(activeComponents);
        this.structuralComponents = source.structuralComponents;
        this.activeConnections = source.activeConnections;
        this.connectionDomains = source.connectionDomains;
        this.hiddenComponents = List.copyOf(children);
        this.physicalConnectionIssues = source.physicalConnectionIssues;
    }

    /**
     * Projects primitive children onto the existing terminal groups of their active parent.
     * The source description and this topology remain unchanged.
     * @param children child primitives with canonical roles and parent terminal references
     * @return a topology whose active rows contain children in place of their parent
     * @throws IllegalArgumentException if a parent, role, target, or terminal reference is invalid
     */
    public CircuitTopology projectHiddenComponents(List<HiddenComponent> children) {
        children = List.copyOf(children);
        var replacements = new LinkedHashMap<ComponentIdentity, List<HiddenComponent>>();
        var byIdentity = new LinkedHashMap<ComponentIdentity, HiddenComponent>();
        hiddenComponents.forEach(child -> byIdentity.put(child.identity(), child));
        for (var child : children) {
            if (!activeComponents.contains(child.parent())) {
                throw new IllegalArgumentException("The hidden component parent must be active");
            }
            if (byIdentity.putIfAbsent(child.identity(), child) != null) {
                throw new IllegalArgumentException("Duplicate hidden component role: " + child.role());
            }
            if (child.primitive() == null) throw new IllegalArgumentException("A hidden primitive is required");
            for (var terminal : List.of(child.input(), child.output())) {
                if (!child.parent().equals(terminal.component()) || !terminalGroups.containsKey(terminal)) {
                    throw new IllegalArgumentException("A hidden terminal must reference an existing parent terminal");
                }
            }
            replacements.computeIfAbsent(child.parent(), key -> new ArrayList<>()).add(child);
        }
        for (var child : children) {
            if (!byIdentity.containsKey(child.controlTarget())) {
                throw new IllegalArgumentException("Unknown hidden control target role: " + child.controlTargetRole());
            }
        }
        var projectedTerminals = new LinkedHashMap<>(terminalGroups);
        var projectedGroups = new LinkedHashMap<>(groups);
        var projectedActive = new ArrayList<ComponentIdentity>();
        for (var component : activeComponents) {
            var replacement = replacements.get(component);
            if (replacement == null) {
                projectedActive.add(component);
            } else {
                for (var child : replacement) {
                    projectedActive.add(child.identity());
                    addHiddenTerminal(projectedTerminals, projectedGroups,
                            new TerminalIdentity(child.identity(), Side.INPUT, 0), child.input());
                    addHiddenTerminal(projectedTerminals, projectedGroups,
                            new TerminalIdentity(child.identity(), Side.OUTPUT, 0), child.output());
                }
            }
        }
        return new CircuitTopology(this, projectedTerminals, projectedGroups, projectedActive,
                new ArrayList<>(byIdentity.values()));
    }

    private static void addHiddenTerminal(Map<TerminalIdentity, Integer> terminalGroups, Map<Integer, Group> groups,
            TerminalIdentity child, TerminalIdentity parent) {
        int id = terminalGroups.get(parent);
        terminalGroups.put(child, id);
        var group = groups.get(id);
        var members = new ArrayList<>(group.terminals());
        members.add(child);
        groups.put(id, new Group(id, group.domain(), group.canonicalLabel(), members, group.producerTerminals()));
    }

    /** @return the immutable hidden primitives in projection order */
    public List<HiddenComponent> hiddenComponents() { return hiddenComponents; }
    /**
     * Resolves a projected control target by canonical child identity.
     * @param child the canonical child to inspect
     * @return the canonical target, or null for a component without a hidden target
     */
    public ComponentIdentity controlTarget(ComponentIdentity child) {
        return hiddenComponents.stream().filter(component -> component.identity().equals(child))
                .map(HiddenComponent::controlTarget).findFirst().orElse(null);
    }

    /**
     * Rejects disconnected physical terminals before numerical construction.
     * Graph inspection retains these terminals and their independent groups.
     * @throws IllegalArgumentException if a physical terminal has no label or geometric connection
     */
    public void validatePhysicalConnections() {
        if (!physicalConnectionIssues.isEmpty()) throw new IllegalArgumentException(physicalConnectionIssues.getFirst());
    }

    /** @return the immutable terminal to group identifier mapping */
    public Map<TerminalIdentity, Integer> terminalGroups() { return terminalGroups; }
    /** @return the immutable groups indexed by their identifiers */
    public Map<Integer, Group> groups() { return groups; }

    /**
     * Finds the group a label names on one sheet, as the desktop resolves signal names per sheet.
     * @return the group, or null when the label is not used on that sheet
     */
    public Group labelGroup(String domain, long sheet, String label) {
        Integer id = labelGroups.get(new Alias(domain, sheet, normalizeLabel(label)));
        return id == null ? null : groups.get(id);
    }

    /** @return terminal groups in saved wire order, before unwired terminal groups */
    public List<Group> connectionGroupOrder() {
        return connectionGroupOrder.stream().map(groups::get).filter(java.util.Objects::nonNull).toList();
    }
    /** @return active computational identities in source and child projection order */
    public List<ComponentIdentity> activeComponents() { return activeComponents; }
    /** @return topology-only identities that do not require computational rows */
    public List<ComponentIdentity> structuralComponents() { return structuralComponents; }
    /**
     * Checks whether a source connection survives its enabled state and ancestors.
     * @param connection the connection from the resolved source model
     * @return true if the connection belongs to the active topology
     */
    public boolean isActiveConnection(CircuitModel.ConnectionData connection) { return activeConnections.contains(connection); }
    /**
     * Checks whether the active topology has saved connections in a domain.
     * @param domain the terminal domain to inspect
     * @return true if the domain has an active connection
     */
    public boolean hasConnections(String domain) { return connectionDomains.contains(domain); }
    /**
     * Finds the identity assigned to a source component.
     * @param component the component from the resolved source model
     * @return its identity, or null if the component was not in that model
     */
    public ComponentIdentity identity(CircuitModel.ComponentData component) { return identities.get(component); }
    /**
     * Finds the signal group for one source component terminal.
     * @param component the component from the resolved source model
     * @param side the saved input or output side
     * @param port the saved zero-based port index
     * @return its group, or null if the terminal is absent or inactive
     */
    public Group group(CircuitModel.ComponentData component, Side side, int port) {
        Integer group = terminalGroups.get(new TerminalIdentity(identities.get(component), side, port));
        return group == null ? null : groups.get(group);
    }

    /**
     * Finds the external output channel group for a padded TO_EXTERNAL input.
     * Padded channels remain absent from ordinary circuit topology while retaining
     * their captured external-channel wiring.
     * @param component the TO_EXTERNAL component from the resolved source model
     * @param port the saved zero-based input port index
     * @return its group, or null if the padded channel has no resolved group
     */
    public Group externalOutputGroup(CircuitModel.ComponentData component, int port) {
        Integer group = externalOutputGroups.get(new TerminalIdentity(identities.get(component), Side.INPUT, port));
        return group == null ? null : groups.get(group);
    }

    private static void addComponents(String domain, List<CircuitModel.ComponentData> components,
            Map<CircuitModel.ComponentData, ComponentIdentity> identities, List<Port> ports,
            List<ComponentIdentity> active, List<ComponentIdentity> structural, Map<Long, SavedCircuitBlock> sheets,
            boolean structuralOnly, boolean legacyGeometry) {
        for (int ordinal = 0; ordinal < components.size(); ordinal++) {
            var component = components.get(ordinal);
            var saved = component.getSavedBlock();
            long identifier = saved == null ? number(component.getParameters().get("uniqueObjectIdentifier"))
                    : saved.uniqueObjectIdentifier();
            int sourceOrdinal = component.getSourceOrdinal() < 0 ? ordinal : component.getSourceOrdinal();
            var identity = new ComponentIdentity(domain, identifier, identifier == 0 ? sourceOrdinal : -1);
            identities.put(component, identity);
            if (!parentsEnabled(saved, sheets)) continue;
            boolean shorted = physicalShort(component, domain);
            boolean controlShorted = domain.equals("CONTROL") && saved != null && saved.enabledState() == 2;
            if (saved != null && saved.enabledState() != 1 && !shorted && !controlShorted) continue;
            boolean topologyOnly = structuralOnly || shorted || boundary(component, domain) || global(component, domain);
            (topologyOnly ? structural : active).add(identity);
            long sheet = saved == null ? 0 : saved.parentSheetIdentifier();
            var registry = domain.equals("LK") && (component.getAcComponent() != null || component.getType() == SpicePorts.E.type())
                    ? SpicePorts.find(domain, component.getType()) : null;
            for (Side side : Side.values()) {
                String[] labels = portLabels(component, side);
                if (registry != null) {
                    int required = (int) registry.nativePorts().stream().filter(pin -> pin.side() == side).count();
                    if (labels.length < required) labels = Arrays.copyOf(labels, required);
                }
                if (labels.length == 0 && (domain.equals("LK") || domain.equals("THERMAL")) && twoPort(component.getType())) {
                    labels = new String[]{""};
                }
                for (int port = 0; port < labels.length; port++) {
                    boolean producer = !topologyOnly && domain.equals("CONTROL") && side == Side.OUTPUT
                            && component.getType() != 78 && component.getType() != 81;
                    long terminalSheet = sheet;
                    if (side == Side.OUTPUT && boundary(component, domain) && sheets.containsKey(sheet)) {
                        terminalSheet = sheets.get(sheet).parentSheetIdentifier();
                    }
                    CircuitModel.TerminalData geometry = null;
                    for (var candidate : component.getTerminalData()) {
                        if (candidate.side() == side && candidate.port() == port) { geometry = candidate; break; }
                    }
                    String terminalDomain = terminalDomain(domain, component.getType(), side);
                    if (geometry == null && registry != null) {
                        for (var pin : registry.nativePorts()) {
                            if (pin.side() != side || pin.port() != port) continue;
                            boolean mirror = component.getAcComponent() != null && component.getAcComponent().mirrored();
                            var offset = pin.nativeOffset(component.getOrientation(), mirror);
                            geometry = new CircuitModel.TerminalData(terminalDomain, terminalSheet, side, port,
                                    component.getPosition()[0] + offset.x(), component.getPosition()[1] + offset.y(), true);
                            break;
                        }
                    }
                    if (geometry == null && legacyGeometry && twoPort(component.getType()) && port == 0) {
                        int distance = side == Side.INPUT ? -2 : 2;
                        int x = component.getPosition()[0];
                        int y = component.getPosition()[1];
                        switch (component.getOrientation()) {
                            case 501 -> y -= distance;
                            case 502 -> x += distance;
                            case 504 -> x -= distance;
                            default -> y += distance;
                        }
                        geometry = new CircuitModel.TerminalData(terminalDomain, terminalSheet, side, port, x, y, true);
                    }
                    ports.add(new Port(new TerminalIdentity(identity, side, port), geometry == null ? terminalDomain : geometry.domain(),
                            geometry == null ? terminalSheet : geometry.sheetIdentifier(), normalizeLabel(labels[port]), producer,
                            geometry, component.getType() == 22 && side == Side.INPUT,
                            component.getSourceOrdinal() < 0 ? ordinal : component.getSourceOrdinal()));
                }
            }
        }
    }

    private static boolean boundary(CircuitModel.ComponentData component, String domain) {
        return switch (domain) {
            case "CONTROL" -> component.getType() == 78;
            case "LK" -> component.getType() == 29 || component.getType() == 30;
            case "THERMAL" -> component.getType() == 49;
            default -> false;
        };
    }

    private static boolean global(CircuitModel.ComponentData component, String domain) {
        return switch (domain) {
            case "CONTROL" -> component.getType() == 81;
            case "LK" -> component.getType() == 31 || component.getType() == 32;
            case "THERMAL" -> component.getType() == 50;
            default -> false;
        };
    }

    private static boolean enabled(SavedCircuitBlock saved, Map<Long, SavedCircuitBlock> sheets) {
        return (saved == null || saved.enabledState() == 1) && parentsEnabled(saved, sheets);
    }

    private static boolean parentsEnabled(SavedCircuitBlock saved, Map<Long, SavedCircuitBlock> sheets) {
        var seen = new HashSet<Long>();
        while (saved != null) {
            long parent = saved.parentSheetIdentifier();
            if (parent == 0) return true;
            if (!seen.add(parent)) throw new IllegalArgumentException("The circuit hierarchy contains a cycle");
            saved = sheets.get(parent);
            if (saved != null && saved.enabledState() != 1) return false;
        }
        return true;
    }

    static String[] portLabels(CircuitModel.ComponentData component, Side side) {
        if (component.getType() == 22 && side == Side.INPUT) {
            String[] labels = component.getTerminalXLabels();
            Object savedWidth = component.getParameters().get("activeInputWidth");
            if (savedWidth instanceof Number number) {
                int activeWidth = Math.min(labels.length, Math.max(0, number.intValue()));
                boolean hasPaddedGeometry = component.getTerminalData().stream().anyMatch(terminal ->
                        terminal.side() == Side.INPUT && terminal.port() >= activeWidth && terminal.geometric());
                if (!hasPaddedGeometry) return Arrays.copyOf(labels, activeWidth);
            }
        }
        String token = side == Side.INPUT ? "labelAnfangsKnoten[]" : "labelEndKnoten[]";
        var saved = component.getSavedBlock();
        if (saved != null && saved.tokens().containsKey(token)) {
            String value = saved.tokens().get(token);
            int start = value.indexOf('/');
            if (start >= 0) return value.substring(start + 1).split("/", -1);
        }
        String[] labels = side == Side.INPUT ? component.getTerminalXLabels() : component.getTerminalYLabels();
        boolean physical = saved != null && (saved.tag().equals("ElementLK") || saved.tag().equals("ElementTHERM"));
        if (labels.length == 0 && physical && twoPort(component.getType())) return new String[]{""};
        return labels;
    }

    private static boolean physicalShort(CircuitModel.ComponentData component, String domain) {
        return (domain.equals("LK") || domain.equals("THERMAL")) && component.getSavedBlock() != null
                && component.getSavedBlock().enabledState() == 2;
    }

    private static boolean twoPort(int type) {
        return switch (type) {
            case 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 24, 26, 28, 44, 45, 46, 47 -> true;
            default -> false;
        };
    }
    private static boolean missingLabel(CircuitModel.ComponentData component, Side side) {
        String[] labels = portLabels(component, side);
        return labels.length == 0 || java.util.Arrays.stream(labels).anyMatch(label -> normalizeLabel(label).isEmpty());
    }
    private static String terminalDomain(String domain, int type, Side side) {
        if (!domain.equals("LK")) return domain;
        return switch (type) {
            case 24, 26, 30, 32, 52 -> "RELUCTANCE";
            case 25 -> side == Side.INPUT ? "LK" : "RELUCTANCE";
            case 41, 42, 44, 45, 46, 47, 48, 49, 50 -> "THERMAL";
            default -> "LK";
        };
    }
    private static boolean ground(String label) { return label.equals("0") || label.equalsIgnoreCase("gnd"); }

    private static String normalizeLabel(String label) {
        return label == null || label.equals("NIX_NIX_NIX") ? "" : label.trim();
    }
    private static long number(Object value) { return value instanceof Number number ? number.longValue() : 0; }
    private static int find(int[] parent, int node) {
        while (parent[node] != node) { parent[node] = parent[parent[node]]; node = parent[node]; }
        return node;
    }
    private static void union(int[] parent, int a, int b) { parent[find(parent, a)] = find(parent, b); }
}

package gecko.core.ac;

import gecko.core.io.CircuitTopology;
import gecko.core.io.HierarchyIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves signed boundary incidence once against the prepared AC branches. */
final class AcHierarchyTraces {
    private record Branch(AcResult.SourceMember positive, AcResult.SourceMember negative) { }
    private record Port(AcResult.SourceMember external, AcResult.SourceMember internal,
                        AcResult.LocalGroup externalGroup, AcResult.LocalGroup internalGroup) { }

    private final CircuitTopology topology;
    private final HierarchyIndex hierarchy;
    private final Map<CircuitTopology.TerminalIdentity, AcResult.SourceMember> members = new LinkedHashMap<>();
    private final List<Branch> branches = new ArrayList<>();
    private final Map<AcResult.SourceMember, Integer> branchSigns = new HashMap<>();
    private final List<Port> ports = new ArrayList<>();
    private Map<CircuitTopology.LocalGroup, AcResult.LocalGroup> localGroups;

    AcHierarchyTraces(CircuitTopology topology, HierarchyIndex hierarchy) {
        this.topology = topology;
        this.hierarchy = hierarchy;
    }

    void addPrimitive(CircuitTopology.TerminalIdentity terminal, List<String> path, String key, String name, int order) {
        members.put(terminal, new AcResult.SourceMember(path, key, name, order, AcResult.TerminalKind.PRIMITIVE));
    }

    void addBranch(CircuitTopology.TerminalIdentity positive, CircuitTopology.TerminalIdentity negative) {
        var branch = new Branch(members.get(positive), members.get(negative));
        branches.add(branch);
        branchSigns.put(branch.positive(), branches.size());
        branchSigns.put(branch.negative(), -branches.size());
    }

    void resolve() {
        var incidences = topology.boundaryIncidences();
        var internal = new HashMap<CircuitTopology.LocalGroup, List<String>>();
        for (var incidence : incidences) {
            var boundary = incidence.boundary();
            var sheet = boundary.sheet();
            var formal = boundary.formal();
            var childPath = hierarchy.path(sheet.id());
            var parentPath = hierarchy.path(sheet.parent());
            String localKey = HierarchyIndex.key(sheet.owner().getSavedBlock());
            if (localKey == null) localKey = sheet.call().instance();
            members.put(incidence.external(), new AcResult.SourceMember(parentPath, localKey,
                    sheet.call().instance(), formal.order(), AcResult.TerminalKind.PARENT_PIN));
            members.put(incidence.internal(), new AcResult.SourceMember(childPath, formal.boundaryMemberKey(),
                    formal.name(), formal.order(), AcResult.TerminalKind.CHILD_BOUNDARY));
            internal.computeIfAbsent(incidence.internalGroup(), ignored -> new ArrayList<>())
                    .add(childPath + "." + formal.name());
        }
        for (var entry : internal.entrySet()) {
            if (entry.getKey().ground() || entry.getValue().size() > 1) {
                throw new IllegalArgumentException("Indeterminate current at internally shorted AC ports " + entry.getValue());
            }
        }
        localGroups = new LinkedHashMap<>();
        for (var terminal : members.keySet()) {
            var local = topology.localGroups().get(terminal);
            localGroups.computeIfAbsent(local, group -> new AcResult.LocalGroup(hierarchy.path(group.sheetIdentifier()),
                    group.ground(), sourceMembers(group.terminals())));
        }
        for (var incidence : incidences) {
            ports.add(new Port(members.get(incidence.external()), members.get(incidence.internal()),
                    localGroups.get(incidence.externalGroup()), localGroups.get(incidence.internalGroup())));
        }
    }

    AcResult.HierarchyResult result(List<List<Complex>> currents, int pointCount) {
        var finalGroups = topology.groups().values().stream()
                .sorted(java.util.Comparator.comparingInt(CircuitTopology.Group::id))
                .map(group -> new AcResult.PhysicalGroup("runtime-group-" + group.id(), group.canonicalLabel().equals("0"),
                        sourceMembers(group.terminals())))
                .filter(group -> !group.members().isEmpty()).toList();
        var branchTraces = new ArrayList<AcResult.BranchTrace>();
        for (int i = 0; i < branches.size(); i++) {
            var branch = branches.get(i);
            branchTraces.add(new AcResult.BranchTrace(branch.positive(), branch.negative(), currents.get(i)));
        }
        var portTraces = new ArrayList<AcResult.PortTrace>();
        var portIndices = new HashMap<AcResult.SourceMember, Integer>();
        for (int i = 0; i < ports.size(); i++) portIndices.put(ports.get(i).external(), i);
        var completed = new HashMap<Integer, List<Complex>>();
        for (int i = 0; i < ports.size(); i++) {
            var port = ports.get(i);
            portTraces.add(new AcResult.PortTrace(port.external(), port.internal(), port.externalGroup(), port.internalGroup(),
                    portCurrent(i, portIndices, currents, pointCount, completed)));
        }
        return new AcResult.HierarchyResult(finalGroups, List.copyOf(localGroups.values()), branchTraces, portTraces);
    }

    private List<Complex> portCurrent(int index, Map<AcResult.SourceMember, Integer> portIndices,
                                     List<List<Complex>> currents, int pointCount, Map<Integer, List<Complex>> completed) {
        var cached = completed.get(index);
        if (cached != null) return cached;
        var result = new ArrayList<>(java.util.Collections.nCopies(pointCount, Complex.ZERO));
        for (var terminal : ports.get(index).internalGroup().members()) {
            int sign = branchSigns.getOrDefault(terminal, 0);
            Integer nested = portIndices.get(terminal);
            if (sign == 0 && nested == null) continue;
            var values = nested == null ? currents.get(Math.abs(sign) - 1)
                    : portCurrent(nested, portIndices, currents, pointCount, completed);
            for (int i = 0; i < pointCount; i++) {
                result.set(i, sign < 0 ? result.get(i).subtract(values.get(i)) : result.get(i).add(values.get(i)));
            }
        }
        var immutable = List.copyOf(result);
        completed.put(index, immutable);
        return immutable;
    }

    private List<AcResult.SourceMember> sourceMembers(List<CircuitTopology.TerminalIdentity> terminals) {
        return terminals.stream().map(members::get).filter(java.util.Objects::nonNull).toList();
    }
}

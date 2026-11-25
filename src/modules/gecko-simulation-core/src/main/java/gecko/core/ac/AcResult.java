package gecko.core.ac;

import java.util.List;

/** Frequency-domain voltages and signed currents of the solved circuit. */
public record AcResult(List<Double> frequencyHz, List<VoltageTrace> groups, List<CurrentTrace> currents,
                       List<Double> residualEta, int complexUnknowns, HierarchyResult hierarchy) {
    public AcResult(List<Double> frequencyHz, List<VoltageTrace> groups, List<CurrentTrace> currents,
                    List<Double> residualEta, int complexUnknowns) {
        this(frequencyHz, groups, currents, residualEta, complexUnknowns, null);
    }

    public AcResult {
        frequencyHz = List.copyOf(frequencyHz);
        groups = List.copyOf(groups);
        currents = List.copyOf(currents);
        residualEta = List.copyOf(residualEta);
    }

    public record Member(String instance, int spiceOrder) { }
    public record VoltageTrace(String id, boolean ground, List<Member> members, List<String> aliases,
                               List<Complex> voltageV, List<SourceMember> physicalMembers, String topologyLabel, String acquiredId) {
        public VoltageTrace(String id, boolean ground, List<Member> members, List<String> aliases, List<Complex> voltageV) {
            this(id, ground, members, aliases, voltageV, List.of(), "", id);
        }
        public String unit() { return "V"; }
        public VoltageTrace {
            members = List.copyOf(members);
            aliases = List.copyOf(aliases);
            voltageV = List.copyOf(voltageV);
            physicalMembers = List.copyOf(physicalMembers);
        }
    }
    public record CurrentTrace(String instance, List<Complex> currentA, SourceMember positive, SourceMember negative,
                               String acquiredInstance) {
        public CurrentTrace(String instance, List<Complex> currentA) { this(instance, currentA, null, null, instance); }
        public String unit() { return "A"; }
        public CurrentTrace { currentA = List.copyOf(currentA); }
    }

    public enum TerminalKind { PRIMITIVE, CHILD_BOUNDARY, PARENT_PIN }

    public record SourceMember(List<String> occurrencePath, String localMemberKey, String instance,
                               int spiceOrder, TerminalKind kind, Long nativeIdentifier,
                               String definitionId, String deviceKind) {
        public SourceMember(List<String> occurrencePath, String localMemberKey, String instance,
                            int spiceOrder, TerminalKind kind) {
            this(occurrencePath, localMemberKey, instance, spiceOrder, kind, null, "", "");
        }
        public SourceMember { occurrencePath = List.copyOf(occurrencePath); }
    }

    public record PhysicalGroup(String voltageTraceId, boolean ground, List<SourceMember> members) {
        public PhysicalGroup { members = List.copyOf(members); }
    }

    public record LocalGroup(List<String> occurrencePath, boolean ground, List<SourceMember> members) {
        public LocalGroup {
            occurrencePath = List.copyOf(occurrencePath);
            members = List.copyOf(members);
        }
    }

    public record BranchTrace(SourceMember positive, SourceMember negative, List<Complex> currentA) {
        public BranchTrace { currentA = List.copyOf(currentA); }
    }

    public record PortTrace(SourceMember external, SourceMember internal, LocalGroup externalGroup,
                            LocalGroup internalGroup, List<Complex> currentIntoChildA) {
        public PortTrace { currentIntoChildA = List.copyOf(currentIntoChildA); }
    }

    public record HierarchyResult(List<PhysicalGroup> groups, List<LocalGroup> localGroups,
                                  List<BranchTrace> currents, List<PortTrace> ports) {
        public HierarchyResult {
            groups = List.copyOf(groups);
            localGroups = List.copyOf(localGroups);
            currents = List.copyOf(currents);
            ports = List.copyOf(ports);
        }
    }
}

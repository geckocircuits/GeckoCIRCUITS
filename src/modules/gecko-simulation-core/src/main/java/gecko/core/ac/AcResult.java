package gecko.core.ac;

import java.util.List;

/** Frequency-domain voltages and signed currents of the solved circuit. */
public record AcResult(List<Double> frequencyHz, List<VoltageTrace> groups, List<CurrentTrace> currents,
                       List<Double> residualEta, int complexUnknowns) {
    public AcResult {
        frequencyHz = List.copyOf(frequencyHz);
        groups = List.copyOf(groups);
        currents = List.copyOf(currents);
        residualEta = List.copyOf(residualEta);
    }

    public record Member(String instance, int spiceOrder) { }
    public record VoltageTrace(String id, boolean ground, List<Member> members, List<String> aliases,
                               List<Complex> voltageV) {
        public VoltageTrace {
            members = List.copyOf(members);
            aliases = List.copyOf(aliases);
            voltageV = List.copyOf(voltageV);
        }
    }
    public record CurrentTrace(String instance, List<Complex> currentA) {
        public CurrentTrace { currentA = List.copyOf(currentA); }
    }
}

package gecko.core.simulation.application;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import gecko.core.testutil.TraceComparisonHelper;
import gecko.core.testutil.TraceComparisonHelper.StepData;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Circuit-topology alignment for the two independently generated desktop fixtures. */
final class ConverterTraceFixture {
    private static final List<String> COMPONENTS = List.of("U.1", "S.1", "L.1", "D.1", "C.1", "R.1");

    private ConverterTraceFixture() {
    }

    static List<StepData> golden(String circuit) throws Exception {
        List<StepData> raw = TraceComparisonHelper.loadTrace("/traces/" + circuit + "_trace.csv.gz");
        List<StepData> result = new ArrayList<>();
        for (StepData step : raw) {
            double[] p = step.nodeVoltages();
            assertEquals(5, p.length, "Fixture must contain four nodes and the source-current unknown");
            assertEquals(COMPONENTS.size(), step.currents().length);
            // Verified from desktop NetListLK, not inferred from startup values:
            // 0=switch node, 1=output, 2=U.1 negative, 3=U.1 positive, 4=source current.
            double reference = p[2];
            result.add(new StepData(step.time(),
                    new double[]{0, p[3] - reference, p[0] - reference, p[1] - reference, p[4]},
                    step.currents(), step.switchR(), step.diodeR(), step.gateSignals()));
        }
        return result;
    }

    static List<StepData> actual(String circuit) throws Exception {
        var resource = Objects.requireNonNull(ConverterTraceFixture.class.getResource("/ipes/" + circuit + ".ipes"));
        Path circuitFile = Path.of(resource.toURI());
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        List<StepData> trace = new ArrayList<>();
        engine.setStepTraceListener((time, p, currents, netlist) -> {
            Map<String, Integer> names = netlist.getComponentNameToIndex();
            assertEquals(new HashSet<>(COMPONENTS), names.keySet(), "Unexpected fixture topology");
            int source = names.get("U.1");
            int capacitor = names.get("C.1");
            int inductor = names.get("L.1");
            int reference = netlist.getNodeY(source);
            assertEquals(reference, netlist.getNodeY(capacitor));
            int switchNode = circuit.equals("buck_simple")
                    ? netlist.getNodeX(inductor) : netlist.getNodeY(inductor);
            double[] physical = {0, p[netlist.getNodeX(source)] - p[reference],
                    p[switchNode] - p[reference],
                    p[netlist.getNodeX(capacitor)] - p[reference],
                    p[netlist.getNodeMax() + netlist.getVoltageSourceNumber(source)]};
            double[] orderedCurrents = COMPONENTS.stream().mapToDouble(name -> currents[names.get(name)]).toArray();
            Map<String, Double> switches = new LinkedHashMap<>();
            Map<String, Double> diodes = new LinkedHashMap<>();
            Map<String, Double> gates = new LinkedHashMap<>();
            for (String name : COMPONENTS) {
                int index = names.get(name);
                CircuitTypCore type = netlist.getType(index);
                double[] parameters = netlist.getParameter(index);
                if (type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_MOSFET) {
                    switches.put(name, parameters[0]);
                    gates.put(name, parameters[8]);
                } else if (type == CircuitTypCore.LK_D || type == CircuitTypCore.LK_THYR
                        || type == CircuitTypCore.LK_IGBT) {
                    diodes.put(name, parameters[0]);
                }
            }
            trace.add(new StepData(time, physical, orderedCurrents, switches, diodes, gates));
        });
        SimulationResult result = engine.runSimulation(SimulationConfig.builder().circuitFile(circuitFile.toString()).build());
        assertTrue(result.isSuccess(), "Simulation failed: " + result.getErrorMessage());
        return trace;
    }
}

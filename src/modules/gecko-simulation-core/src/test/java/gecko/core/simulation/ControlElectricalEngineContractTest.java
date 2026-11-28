package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class ControlElectricalEngineContractTest {
    static String element(String domain, int type, String name, String parameters,
            String inputs, String outputs, String references) {
        return (domain.equals("LK") ? "e" : "c") + "\n<Element" + domain + ">\n"
                + "typ " + type + "\nidStringDialog " + name + "\nx 0\ny 0\norientierung 503\nparameter[] " + parameters
                + "\nlabelAnfangsKnoten[] " + inputs + "\nlabelEndKnoten[] " + outputs
                + "\nparameterString[] " + references + "\n<" + (char) 92 + "Element" + domain + ">\n";
    }

    static Path circuit(CircuitTypCore type, boolean feedback) throws Exception {
        int number = switch (type) {
            case LK_S -> 7;
            case LK_MOSFET -> 28;
            case LK_IGBT -> 10;
            case LK_THYR -> 8;
            default -> throw new IllegalArgumentException("Unsupported test device");
        };
        String parameters = type == CircuitTypCore.LK_S
                ? "1000000 0.25 1000000 0 0 0 0 0 0 0 0 0"
                : "1000000 " + (type == CircuitTypCore.LK_MOSFET ? "0" : "1.5")
                        + " 0.25 1000000 0 0 0 0 0 0 0 0";
        String text = "tDURATION 1\ndt 0.125\n"
                + element("LK", 4, "U.1", "401 6", "/1", "/0", "")
                + element("LK", 1, "R.1", "1.75", "/1", "/2", "")
                + element("LK", number, "device", parameters, "/2", "/0", "");
        text += element("CONTROL", 1, "VOLT.1", "", "", "/voltage", "/2/0");
        text += element("CONTROL", 2, "AMP.1", "", "", "/current", "/device");
        text += feedback
                ? element("CONTROL", 7, "GAIN.1", "0.5", "/voltage", "/gate", "")
                : element("CONTROL", 4, "SIGNAL.1", "404 1 1 0 0 0.5", "", "/gate", "");
        text += element("CONTROL", 6, "GATE.1", "", "/gate", "", "/device");
        text += element("CONTROL", 5, "SCOPE.1", "", "/gate/current/voltage", "", "");
        Path file = Files.createTempFile("control-electrical-", ".ipes");
        Files.writeString(file, text);
        return file;
    }

    static Stream<Arguments> devicesAndMethods() {
        return Stream.of(CircuitTypCore.LK_S, CircuitTypCore.LK_MOSFET,
                CircuitTypCore.LK_THYR, CircuitTypCore.LK_IGBT)
                .flatMap(type -> Stream.of(SolverType.values()).map(method -> Arguments.of(type, method)));
    }

    @ParameterizedTest
    @MethodSource("devicesAndMethods")
    void fileLoadedPulseControlsTheNextSolvedStepAndRestartsWithoutStateLeakage(
            CircuitTypCore type, SolverType method) throws Exception {
        var engine = new HeadlessSimulationEngine();
        var config = SimulationConfig.builder().circuitFile(circuit(type, false).toString())
                .solverType(method).build();
        List<Double> previousRun = null;
        for (int run = 0; run < 2; run++) {
            List<Double> currents = new ArrayList<>();
            engine.setStepTraceListener((time, volts, amps, netlist) -> {
                int device = netlist.getElementIndexByName("device");
                int resistor = netlist.getElementIndexByName("R.1");
                int source = netlist.getElementIndexByName("U.1");
                currents.add(amps[device]);
                assertEquals(amps[device], amps[resistor], 1e-10);
                assertEquals(-amps[device], amps[source], 1e-10);
            });
            var result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertEquals(9, result.getTotalTimeSteps());
            double onCurrent = type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_MOSFET ? 3 : 2.25;
            for (int step = 0; step < 9; step++) {
                boolean conducting = step >= 1 && (type == CircuitTypCore.LK_THYR || step <= 3);
                double expected = conducting ? onCurrent : 6 / 1000001.75;
                assertEquals(expected, currents.get(step), 1e-10, type + " electrical sample " + step);
                assertEquals(expected, result.getSignalData(1)[step], 1e-6);
                double publishedGate = step <= 2 || step == 8 ? 1 : 0;
                assertEquals(publishedGate, result.getSignalData(0)[step], 1e-6);
            }
            if (previousRun != null) assertEquals(previousRun, currents);
            previousRun = currents;
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void solvedVoltageFeedbackChangesTheFollowingElectricalStep(SolverType method) throws Exception {
        var engine = new HeadlessSimulationEngine();
        List<Double> currents = new ArrayList<>();
        engine.setStepTraceListener((time, volts, amps, netlist) ->
                currents.add(amps[netlist.getElementIndexByName("device")]));
        var result = engine.runSimulation(SimulationConfig.builder()
                .circuitFile(circuit(CircuitTypCore.LK_S, true).toString()).solverType(method).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(9, currents.size());
        for (int step = 0; step < currents.size(); step++) {
            boolean on = step % 2 == 1;
            assertEquals(on ? 3 : 6 / 1000001.75, currents.get(step), 1e-10);
            assertEquals(on ? 0.375 : 3000000 / 1000001.75, result.getSignalData(0)[step], 1e-6);
        }
    }
}

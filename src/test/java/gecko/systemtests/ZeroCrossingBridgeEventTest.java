package gecko.systemtests;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the education bridge circuits through the public engine and accounts for every diode transition. */
@Tag("regression")
class ZeroCrossingBridgeEventTest {
    @ParameterizedTest
    @ValueSource(strings = {"2phaseDiodeBridge_DC-Inductor", "2phaseDiodeBridge_AC-Inductor"})
    void everyCommittedDiodeTransitionIsLocatedOrCountedUnlocated(String circuit) throws Exception {
        var resource = Objects.requireNonNull(getClass().getResource("/ipes/education/" + circuit + ".ipes"));
        var engine = new HeadlessSimulationEngine();
        boolean[][] previous = {null};
        int[] observed = {0};
        engine.setStepTraceListener((time, volts, currents, netlist) -> {
            boolean[] state = new boolean[netlist.getElementCount()];
            for (int element = 0; element < state.length; element++) {
                if (netlist.getType(element) != CircuitTypCore.LK_D) continue;
                state[element] = netlist.getParameter(element)[0] < 10000;
                if (previous[0] != null && previous[0][element] != state[element]) observed[0]++;
            }
            previous[0] = state;
        });
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(Path.of(resource.toURI()).toString())
                .solverType(SolverType.SOLVER_BE).zeroCrossing(true).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        long located = ((Number) result.getMetadata().get("zeroCrossingLocated")).longValue();
        long unlocated = ((Number) result.getMetadata().get("zeroCrossingUnlocated")).longValue();
        long dropped = ((Number) result.getMetadata().get("zeroCrossingDropped")).longValue();
        assertTrue(observed[0] > 0, "the bridge switches");
        assertEquals(0, dropped);
        assertEquals(observed[0], located + unlocated,
                "every observed grid transition is a located event or an unlocated count: located=" + located
                        + " unlocated=" + unlocated);
    }
}

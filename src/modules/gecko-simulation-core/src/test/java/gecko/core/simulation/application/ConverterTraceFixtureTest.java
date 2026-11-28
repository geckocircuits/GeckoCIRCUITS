package gecko.core.simulation.application;

import gecko.core.testutil.TraceComparisonHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConverterTraceFixtureTest {
    @ParameterizedTest
    @ValueSource(strings = {"buck_simple", "boost_simple"})
    void desktopGateAndSourceCurrentColumnsDescribeTheAppliedElectricalState(String circuit) throws Exception {
        var trace = TraceComparisonHelper.loadTrace("/traces/" + circuit + "_trace.csv.gz");
        Set<Double> states = new HashSet<>();
        assertFalse(trace.isEmpty());
        for (var step : trace) {
            assertEquals(step.nodeVoltages()[4], step.currents()[0], 1e-12,
                    "Source current must equal the solved MNA unknown");
            assertEquals(step.switchR().keySet(), step.gateSignals().keySet());
            double gate = step.gateSignals().get("S.1");
            double applied = step.switchR().get("S.1") == 0.01 ? 1.0 : 0.0;
            assertEquals(applied, gate, "Gate column must describe the switch used in this solve");
            states.add(gate);
        }
        assertEquals(Set.of(0.0, 1.0), states, "The reference must exercise both switch states");
    }
}

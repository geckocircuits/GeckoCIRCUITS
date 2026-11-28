package gecko.core.preparation;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLimitsTest {
    @Test
    void invalidModesAndDutyRejectTheCompleteOverrideSet() {
        var model = CircuitPreparationTest.resistorCircuit();
        model.addControlComponent(CircuitPreparationTest.component(4, "signal", new double[]{400, 2, 1, 0, 0, 0.5}, "", "wave"));
        var preparation = new CircuitPreparation();
        var base = preparation.capture(model);
        for (String field : new String[]{"U.1.sourceType", "signal.subtype", "signal.duty"}) {
            double[] values = field.endsWith("duty") ? new double[]{-1, 2} : new double[]{999, 1.5};
            for (double value : values) {
                var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(base,
                        new PreparationRequest(SolverOverrides.none(), Map.of(field, value, "R.1.resistance", 20.0))));
                assertTrue(rejected.report().diagnostics().stream().anyMatch(item ->
                        item.code().equals("INVALID_PARAMETER") && field.equals(item.parameterPath())), rejected.report().toString());
            }
        }
        var controlled = assertInstanceOf(PreparationResult.Inspectable.class, preparation.prepare(base,
                new PreparationRequest(SolverOverrides.none(), Map.of("U.1.sourceType", 399.0))));
        assertTrue(controlled.report().diagnostics().stream().anyMatch(item -> item.code().equals("UNSUPPORTED_SOURCE_MODE")));
        assertEquals(java.util.List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(
                assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(base, PreparationRequest.defaults())).execution()));
    }
}

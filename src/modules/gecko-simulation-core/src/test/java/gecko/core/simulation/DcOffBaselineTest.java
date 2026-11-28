package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

/**
 * With the DC operating point off, the raw double traces equal reference fingerprints that were
 * independently verified against the original compiled core. Every dynamic element starts from a nonzero configured state.
 */
class DcOffBaselineTest {
    /** Fingerprints of the step-by-step raw bits, verified against the original compiled core. */
    private static final Map<String, Long> BASELINE = Map.of(
            "SOLVER_BE", -7727171700999625526L,
            "SOLVER_TRZ", -1185240888484387106L,
            "SOLVER_GS", -7196329176843263812L,
            "BUCK", -5403098099056885493L);

    private static CircuitModel configuredStates() {
        var model = new CircuitModel();
        model.setTimeStep(0.01);
        model.setSimulationDuration(0.1);
        model.setDataContainerSignals(new String[]{"ground", "a", "b", "c"});
        model.addCircuitComponent(component(4, "V2", new double[]{401, 6}, new String[]{"3"}, "0"));
        model.addCircuitComponent(component(1, "R1", new double[]{2}, new String[]{"7"}, "5"));
        model.addCircuitComponent(component(2, "L1", new double[]{1e-3, 3.0}, new String[]{"5"}, "3"));
        model.addCircuitComponent(component(3, "C1", new double[]{1e-3, 1.0}, new String[]{"5"}, "0"));
        model.addCircuitComponent(component(4, "V1", new double[]{401, 12}, new String[]{"7"}, "0"));
        model.addCircuitComponent(component(2, "L2", new double[]{2e-3, 0.5}, new String[]{"7"}, "8"));
        model.addCircuitComponent(component(1, "R3", new double[]{3}, new String[]{"8"}, "0"));
        model.addCircuitComponent(component(3, "C2", new double[]{2e-3, 2.0}, new String[]{"8"}, "0"));
        return model;
    }

    private static long fingerprint(CircuitModel model, SolverType solver) {
        long[] hash = {17};
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(solver, null, null), Map.of())));
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            for (int step = 0; step < 10; step++) {
                session.step((time, volts, currents, circuit) -> {
                    for (double value : volts) hash[0] = 31 * hash[0] + Double.doubleToRawLongBits(value);
                    for (double value : currents) hash[0] = 31 * hash[0] + Double.doubleToRawLongBits(value);
                });
            }
        }
        return hash[0];
    }

    private static long buckFingerprint() throws Exception {
        var file = Path.of(DcOffBaselineTest.class.getResource("/ipes/buck_simple.ipes").toURI()).toString();
        long[] hash = {17};
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, circuit) -> {
            for (double value : volts) hash[0] = 31 * hash[0] + Double.doubleToRawLongBits(value);
            for (double value : currents) hash[0] = 31 * hash[0] + Double.doubleToRawLongBits(value);
        });
        assertTrue(engine.runSimulation(gecko.core.simulation.SimulationConfig.builder().circuitFile(file).build()).isSuccess());
        return hash[0];
    }

    @Test
    void theFlagOffTracesEqualTheRecordedBaselineBitForBit() throws Exception {
        Map<String, Long> actual = new LinkedHashMap<>();
        for (var solver : SolverType.values()) actual.put(solver.name(), fingerprint(configuredStates(), solver));
        actual.put("BUCK", buckFingerprint());
        assertEquals(new LinkedHashMap<>(BASELINE), actual);
    }
}

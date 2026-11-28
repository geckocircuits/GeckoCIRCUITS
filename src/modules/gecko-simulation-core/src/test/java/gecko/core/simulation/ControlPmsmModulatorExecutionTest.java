package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class ControlPmsmModulatorExecutionTest {
    @ParameterizedTest
    @CsvSource({
        "0.4330127018922193,0.25,0.4,1,0,1,1",
        "0.4330127018922193,0.25,0.6,1,0,0,1",
        "0,0.5,0.4,1,1,0,1",
        "0,0.5,0.6,1,0,0,1",
        "-0.4330127018922193,0.25,0.4,1,1,0,1",
        "-0.4330127018922193,0.25,0.6,1,1,0,0",
        "-0.4330127018922193,-0.25,0.4,1,1,1,0",
        "-0.4330127018922193,-0.25,0.6,1,1,0,0",
        "0,-0.5,0.4,1,1,1,0",
        "0,-0.5,0.6,1,0,1,0",
        "0.4330127018922193,-0.25,0.4,1,0,1,1",
        "0.4330127018922193,-0.25,0.6,1,0,1,0",
        "0,0,0.4,1,1,1,1",
        "0,0,0.5,1,0,0,0",
        "0,0,0.6,1,0,0,0",
        "2,0,0.98,1,0,0,0",
        "0,0,0.6,0,1,1,1"
    })
    void admittedModulationPreservesSectorsComparatorEqualityAndZeroDcBehavior(
            double alpha, double beta, double carrier, double dc, int u, int v, int w) {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"gateW", "gateU", "gateV"}));
        model.addControlComponent(component(77, "modulator", new double[0],
                new String[]{"alpha", "beta", "carrier", "dc"}, "gateU", "gateV", "gateW"));
        model.addControlComponent(component(3, "dc", new double[]{dc}, new String[0], "dc"));
        model.addControlComponent(component(3, "carrier", new double[]{carrier}, new String[0], "carrier"));
        model.addControlComponent(component(3, "beta", new double[]{beta}, new String[0], "beta"));
        model.addControlComponent(component(3, "alpha", new double[]{alpha}, new String[0], "alpha"));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new String[]{"gateW", "gateU", "gateV"}, result.getSignalNames());
        float[] expected = {w, u, v};
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(new float[]{expected[row], expected[row], expected[row]}, result.getSignalData(row));
        }
    }
}

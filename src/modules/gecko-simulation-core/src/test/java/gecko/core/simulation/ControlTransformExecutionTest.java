package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class ControlTransformExecutionTest {
    @ParameterizedTest
    @CsvSource({
        "1,-0.5,-0.5,0,1,0,1,-0.5,-0.5",
        "1,-0.5,-0.5,1.5707963267948966,0,-1,1,-0.5,-0.5",
        "1,2,3,0,-1,-0.5773502691896257,-1,0,1"
    })
    void admittedTransformsPreserveAxesAndRemoveOnlyCommonMode(double a, double b, double c, double angle,
            double d, double q, double reconstructedA, double reconstructedB, double reconstructedC) {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        model.addControlComponent(component(5, "scope", new double[0],
                new String[]{"d", "q", "phaseA", "phaseB", "phaseC"}));
        model.addControlComponent(component(66, "inverse", new double[0],
                new String[]{"d", "q", "angle"}, "phaseA", "phaseB", "phaseC"));
        model.addControlComponent(component(65, "forward", new double[0],
                new String[]{"a", "b", "c", "angle"}, "d", "q"));
        model.addControlComponent(component(3, "theta", new double[]{angle}, new String[0], "angle"));
        model.addControlComponent(component(3, "c", new double[]{c}, new String[0], "c"));
        model.addControlComponent(component(3, "b", new double[]{b}, new String[0], "b"));
        model.addControlComponent(component(3, "a", new double[]{a}, new String[0], "a"));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new String[]{"d", "q", "phaseA", "phaseB", "phaseC"}, result.getSignalNames());
        double[] expected = {d, q, reconstructedA, reconstructedB, reconstructedC};
        for (int row = 0; row < expected.length; row++) {
            float value = (float) expected[row];
            assertArrayEquals(new float[]{value, value, value}, result.getSignalData(row), 1e-6f);
        }
    }
}

package gecko.core.simulation;

import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.control.calculators.PmsmControlCalculator;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class ControlPmsmControllerExecutionTest {
    private static final String[] OUTPUTS = {"alpha", "beta", "vq", "vd", "iqRef", "idRef", "iq", "id"};

    private record Trial(String name, double[] inputs, float[][] expected) {
        @Override public String toString() { return name; }
    }

    static Stream<Trial> trials() {
        return Stream.of(
            new Trial("normal", inputs(0, 0, 0, 1, 10, 10), new float[][]{
                {0,0,0}, {2.25f,5.25f,9.25f}, {2.25f,5.25f,9.25f}, {0,0,0},
                {1.5f,2.5f,3.5f}, {0,0,0}, {0,0,0}, {0,0,0}
            }),
            new Trial("sample-disabled", inputs(0, 0, 0, 0.999, 10, 10), new float[][]{
                {0,0,0}, {0,0,0}, {2.25f,5.25f,9.25f}, {0,0,0},
                {1.5f,2.5f,3.5f}, {0,0,0}, {0,0,0}, {0,0,0}
            }),
            new Trial("saturated", inputs(0, 0, 0, 1, 2, 3), new float[][]{
                {0,0,0}, {2.25f,3,3}, {2.25f,3,3}, {0,0,0},
                {1.5f,2,2}, {0,0,0}, {0,0,0}, {0,0,0}
            }),
            new Trial("d-current", inputs(1, -0.5, 0, 1, 10, 10), new float[][]{
                {-1.5f,-2.5f,-3.5f}, {2.25f,5.25f,9.25f}, {2.25f,5.25f,9.25f},
                {-1.5f,-2.5f,-3.5f}, {1.5f,2.5f,3.5f}, {0,0,0}, {0,0,0}, {1,1,1}
            }),
            new Trial("rotated-current", inputs(1, -0.5, 1.5707963267948966, 1, 10, 10), new float[][]{
                {-3.75f,-7.75f,-10}, {0,0,0}, {3.75f,7.75f,10}, {0,0,0},
                {1.5f,2.5f,3.5f}, {0,0,0}, {-1,-1,-1}, {0,0,0}
            })
        );
    }

    static Stream<Trial> restartTrials() {
        return trials().filter(trial -> trial.name.equals("normal") || trial.name.equals("d-current"));
    }

    @ParameterizedTest
    @MethodSource("trials")
    void preparedControllerPreservesAllEightOutputsAndSamplingBehavior(Trial trial) {
        var model = model(trial.inputs);
        var ready = prepare(model);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(OUTPUTS, result.getSignalNames());
        for (int row = 0; row < OUTPUTS.length; row++) {
            assertArrayEquals(trial.expected[row], result.getSignalData(row), 1e-6f, OUTPUTS[row]);
        }
    }

    @ParameterizedTest
    @MethodSource("restartTrials")
    void aFreshStartClearsHistoryAndPublishedOutputsWithoutRewiring(Trial trial) {
        var build = build(trial.inputs);
        var netlist = build.getControlNetlist();
        var scope = ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos());
        netlist.initializeAtSimulationStart(1);
        for (int step = 0; step < 3; step++) netlist.executeTimeStep(1, step);
        assertArrayEquals(column(trial.expected, 2), scope.extractCurrentValues(), 1e-6f);
        netlist.initializeAtSimulationStart(1);
        assertArrayEquals(new float[8], scope.extractCurrentValues());
        netlist.executeTimeStep(1, 0);
        assertArrayEquals(column(trial.expected, 0), scope.extractCurrentValues(), 1e-6f);
    }

    @Test
    void pauseAndCommittedNewStepKeepControllerHistory() {
        try (var runtime = ControlRuntime.fromDeferred(build(inputs(0, 0, 0, 1, 10, 10)), 1)) {
            var electrical = new gecko.core.circuit.netlist.CircuitNetlist();
            runtime.advanceAfterElectricalStep(electrical, 0);
            runtime.advanceAfterElectricalStep(electrical, 1);
            assertArrayEquals(new float[]{0,5.25f,5.25f,0,2.5f,0,0,0}, runtime.sampleScope(), 1e-6f);
            runtime.tearDownOnPause();
            runtime.prepareTimeStepChange(0.5).run();
            runtime.initializeAtSimulationStart();
            runtime.advanceAfterElectricalStep(electrical, 1.5);
            assertArrayEquals(new float[]{0,7.125f,7.125f,0,3,0,0,0}, runtime.sampleScope(), 1e-6f);
            runtime.advanceAfterElectricalStep(electrical, 2);
            assertArrayEquals(new float[]{0,9.25f,9.25f,0,3.5f,0,0,0}, runtime.sampleScope(), 1e-6f);
        }
    }

    @Test
    void disabledSamplingLatchesVoltageWhileTheControllerKeepsAdvancing() {
        var build = build(inputs(0, 0, 0, 1, 10, 10));
        var controller = Arrays.stream(build.getControlNetlist().getSortedCalculators())
                .filter(PmsmControlCalculator.class::isInstance).map(PmsmControlCalculator.class::cast)
                .findFirst().orElseThrow();
        var flag = Arrays.stream(build.getControlNetlist().getSortedCalculators())
                .filter(ConstantCalculator.class::isInstance).map(ConstantCalculator.class::cast)
                .filter(source -> source._outputSignal[0] == controller._inputSignal[11]).findFirst().orElseThrow();
        try (var runtime = ControlRuntime.fromDeferred(build, 1)) {
            var electrical = new gecko.core.circuit.netlist.CircuitNetlist();
            runtime.advanceAfterElectricalStep(electrical, 0);
            flag.setConst(0.999);
            runtime.advanceAfterElectricalStep(electrical, 1);
            assertArrayEquals(new float[]{0,2.25f,5.25f,0,2.5f,0,0,0}, runtime.sampleScope(), 1e-6f);
            runtime.advanceAfterElectricalStep(electrical, 2);
            assertArrayEquals(new float[]{0,2.25f,9.25f,0,3.5f,0,0,0}, runtime.sampleScope(), 1e-6f);
            flag.setConst(1);
            runtime.advanceAfterElectricalStep(electrical, 3);
            assertArrayEquals(new float[]{0,10,10,0,4.5f,0,0,0}, runtime.sampleScope(), 1e-6f);
        }
    }

    private static double[] inputs(double a, double b, double angle, double sample, double speedLimit, double currentLimit) {
        return new double[]{a,b,0,angle,9.549296585513721,1,1,speedLimit,1,1,currentLimit,sample};
    }

    private static float[] column(float[][] values, int sample) {
        float[] result = new float[values.length];
        for (int row = 0; row < values.length; row++) result[row] = values[row][sample];
        return result;
    }

    private static CircuitModel model(double[] values) {
        var model = new CircuitModel();
        model.setTimeStep(1);
        model.setSimulationDuration(2);
        String[] labels = new String[12];
        for (int port = 0; port < labels.length; port++) labels[port] = "input" + port;
        model.addControlComponent(component(5, "scope", new double[0], OUTPUTS));
        model.addControlComponent(component(76, "controller", new double[0], labels, OUTPUTS));
        for (int port = 11; port >= 0; port--) {
            model.addControlComponent(component(3, "source" + port, new double[]{values[port]}, new String[0], labels[port]));
        }
        return model;
    }

    private static PreparationResult.Ready prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
    }

    private static ControlCalculatorFactory.BuildResult build(double[] inputs) {
        var model = model(inputs);
        prepare(model);
        return new ControlCalculatorFactory().buildCircuit(model, 1);
    }
}

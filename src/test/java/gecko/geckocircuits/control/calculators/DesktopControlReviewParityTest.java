package gecko.geckocircuits.control.calculators;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class DesktopControlReviewParityTest {
    @Test
    void preparedPt2FeedbackMatchesTheLegacyCalculatorForKnownSettledInputs() {
        var model = model();
        model.setSimulationDuration(1);
        model.addControlComponent(component(3, "one", new double[]{1}, new String[0], "one"));
        model.addControlComponent(component(12, "sum", new double[0], new String[]{"one", "feedback"}, "sum"));
        model.addControlComponent(component(9, "pt2", new double[]{1, 0.5}, new String[]{"sum"}, "feedback"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"sum", "feedback"}));
        var result = run(model);
        var legacy = new PT2Calculator(1, 0.5);
        legacy.checkInputWithoutConnectionAndFill(0);
        double[] settledInputs = {1, 1.125, 1.359375};
        double[] expected = {0, 0.125, 0.359375};
        for (int step = 0; step < expected.length; step++) {
            legacy._inputSignal[0][0] = settledInputs[step];
            legacy.berechneYOUT(0.5);
            assertEquals(expected[step], legacy._outputSignal[0][0]);
            assertEquals(legacy._outputSignal[0][0], result.getSignalData(1)[step]);
            assertEquals(settledInputs[step], result.getSignalData(0)[step]);
        }
    }

    static Stream<Arguments> disconnectedAuxiliaryBlocks() {
        return Stream.of(
                Arguments.of(64, new IntegratorCalculation(1, 0, -10, 10), new double[]{1, 0, -10, 10}, new float[]{0.5f, 1.5f}),
                Arguments.of(53, new CounterCalculatable(), new double[0], new float[]{1, 1}),
                Arguments.of(26, new SampleHoldCalculator(), new double[0], new float[]{0, 0}));
    }

    @ParameterizedTest
    @MethodSource("disconnectedAuxiliaryBlocks")
    void preparedDisconnectedAuxiliaryPortMatchesDesktopZeroFill(int type, AbstractControlCalculatable legacy,
            double[] settings, float[] expected) {
        for (int port = 0; port < legacy._inputSignal.length; port++) legacy.checkInputWithoutConnectionAndFill(port);
        if (legacy instanceof InitializableAtSimulationStart initializable) initializable.initializeAtSimulationStart(0.5);
        legacy._inputSignal[0][0] = 2;
        var model = model();
        model.addControlComponent(component(3, "source", new double[]{2}, new String[0], "drive"));
        model.addControlComponent(component(type, "block", settings, new String[]{"drive", ""}, "result"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"result"}));
        var result = run(model);
        for (int step = 0; step < expected.length; step++) {
            legacy.berechneYOUT(0.5);
            assertEquals(expected[step], legacy._outputSignal[0][0]);
            assertEquals(legacy._outputSignal[0][0], result.getSignalData(0)[step]);
        }
    }

    private static CircuitModel model() {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        return model;
    }

    private static gecko.core.simulation.SimulationResult run(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result;
    }

    private static CircuitModel.ComponentData component(int type, String name, double[] values, String[] inputs, String... outputs) {
        var data = new CircuitModel.ComponentData(type, name);
        data.setNumericParameters(values);
        data.setTerminalXLabels(inputs);
        data.setTerminalYLabels(outputs);
        return data;
    }
}

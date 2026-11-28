package gecko.core.simulation;

import gecko.core.control.calculators.DelayCalculator;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class ControlLifecycleReviewTest {
    @ParameterizedTest
    @CsvSource({"10,3", "-10,-2", "1,1"})
    void externalLimitsIgnoreReversedUnusedInternalBounds(double input, float expected) {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.1);
        model.addControlComponent(component(3, "drive", new double[]{input}, new String[0], "drive"));
        model.addControlComponent(component(3, "lower", new double[]{-2}, new String[0], "lower"));
        model.addControlComponent(component(3, "upper", new double[]{3}, new String[0], "upper"));
        model.addControlComponent(component(27, "limit", new double[]{10, -10, 1},
                new String[]{"drive", "lower", "upper"}, "limited"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"limited"}));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var saved = ready.circuit().toModel().getControlComponents().get(3).getNumericParameters();
        assertEquals(10, saved[0]);
        assertEquals(-10, saved[1]);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{expected, expected}, result.getSignalData(0));
    }

    @Test
    void internalLimitsStillRejectReversedBounds() {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.1);
        model.addControlComponent(component(27, "limit", new double[]{10, -10, 0}, new String[]{""}, "limited"));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        assertInstanceOf(PreparationResult.Inspectable.class, outcome);
        assertEquals(ReadinessReport.Outcome.INVALID, outcome.report().outcome());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anExplicitReplacementCannotRestoreTheFactoryGraph(boolean initialize) {
        var runtime = factoryChain().getControlNetlist();
        var replacement = new gecko.core.control.calculators.ConstantCalculator(7);
        runtime.setSortedCalculators(List.of(replacement));
        if (initialize) runtime.initializeAtSimulationStart(0.1);
        runtime.executeTimeStep(0.1, 0);
        assertEquals(1, runtime.getCalculatorCount());
        assertEquals(7, replacement._outputSignal[0][0]);
    }

    @Test
    void anExplicitEmptyOrderStaysEmpty() {
        var runtime = factoryChain().getControlNetlist();
        runtime.setSortedCalculators(List.of());
        runtime.initializeAtSimulationStart(0.1);
        runtime.executeTimeStep(0.1, 0);
        assertFalse(runtime.hasCalculators());
    }

    @Test
    void anExplicitOrderKeepsTheCallersCombinationalSequence() throws Exception {
        var runtime = factoryChain().getControlNetlist();
        var source = new gecko.core.control.calculators.TimeCalculator();
        var gain = new gecko.core.control.calculators.GainCalculator(3);
        gain.setInputSignal(0, source, 0);
        runtime.setSortedCalculators(List.of(gain, source));
        runtime.initializeAtSimulationStart(0.1);
        runtime.executeTimeStep(0.1, 1);
        assertEquals(0, gain._outputSignal[0][0]);
        runtime.executeTimeStep(0.1, 2);
        assertEquals(3, gain._outputSignal[0][0]);
    }

    private static ControlCalculatorFactory.BuildResult factoryChain() {
        return new ControlCalculatorFactory().build(List.of(
                component(3, "source", new double[]{1}, new String[0], "signal"),
                component(5, "scope", new double[0], new String[]{"signal"})), 0.1);
    }

    @ParameterizedTest
    @CsvSource({"8,1;1,0,0,0.3333333333333333", "8,2;3,0,0,0.6", "9,1;1,0,0,0", "9,2;3,0,0,0", "10,1;1,0,0,1.5",
            "11,0.5;0,0,0,1", "11,0.5;1,0.5,0,1", "25,1,0,0,0", "26,0,1,0,1",
            "29,1,0,0,1", "53,0,0,0,1", "64,1;2;-10;10,0,2,2.5",
            "4,400;1;0.125;0;0;0.5,0,0,0", "4,401;1;0.125;0;0;0.5,0,0,0.5",
            "4,404;1;0.125;0;0;0.5,0,0,1"})
    void restartingTheSameRuntimeRestoresFreshState(int type, String saved, double auxiliary,
            double initial, double first) {
        double[] parameters = java.util.Arrays.stream(saved.split(";")).mapToDouble(Double::parseDouble).toArray();
        boolean twoInputs = type == 26 || type == 53 || type == 64 || type == 11 && parameters[1] != 0;
        String[] inputs = type == 4 ? new String[0] : twoInputs ? new String[]{"drive", "aux"} : new String[]{"drive"};
        var built = new ControlCalculatorFactory().build(List.of(
                component(3, "drive", new double[]{1}, new String[0], "drive"),
                component(3, "aux", new double[]{auxiliary}, new String[0], "aux"),
                component(type, "stateful", parameters, inputs, "result"),
                component(5, "scope", new double[0], new String[]{"result"})), 1);
        var runtime = built.getControlNetlist();
        var scope = ScopeDataExtractor.fromScopeInfo(built.getMapping().getScopeInfo());
        runtime.initializeAtSimulationStart(1);
        runtime.executeTimeStep(1, 0);
        assertEquals(first, scope.extractCurrentValues()[0], 1e-6);
        runtime.executeTimeStep(1, 1);
        float second = scope.extractCurrentValues()[0];
        runtime.executeTimeStep(1, 2);
        runtime.initializeAtSimulationStart(1);
        assertEquals(initial, scope.extractCurrentValues()[0], 1e-6, "published restart state");
        runtime.executeTimeStep(1, 0);
        assertEquals(first, scope.extractCurrentValues()[0], 1e-6, "first restarted step");
        runtime.executeTimeStep(1, 1);
        assertEquals(second, scope.extractCurrentValues()[0], 1e-6, "second restarted step preserves configuration");
    }

    @Test
    void restartingRandomWalkClearsItsAccumulatedValue() {
        var built = new ControlCalculatorFactory().build(List.of(
                component(4, "random", new double[]{405, 1, 1, 0, 0, 0.5}, new String[0], "random"),
                component(5, "scope", new double[0], new String[]{"random"})), 1);
        var runtime = built.getControlNetlist();
        var scope = ScopeDataExtractor.fromScopeInfo(built.getMapping().getScopeInfo());
        runtime.initializeAtSimulationStart(1);
        for (int step = 0; step < 100; step++) runtime.executeTimeStep(1, step);
        runtime.initializeAtSimulationStart(1);
        assertEquals(0, scope.extractCurrentValues()[0]);
        runtime.executeTimeStep(1, 0);
        assertTrue(Math.abs(scope.extractCurrentValues()[0]) <= 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aLaterOversizedDelayCannotChangeAnEarlierHistory(boolean restart) {
        var subject = delayedPair();
        var reference = delayedPair();
        subject.getControlNetlist().initializeAtSimulationStart(0.1);
        reference.getControlNetlist().initializeAtSimulationStart(0.1);
        for (int step = 0; step < 4; step++) {
            subject.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            reference.getControlNetlist().executeTimeStep(0.1, step * 0.1);
        }
        for (var calculator : subject.getControlNetlist().getSortedCalculators()) {
            if (calculator instanceof DelayCalculator delay) {
                delay.setDelayTime(delay.getConfiguredDelayTime() == 0.2 ? 0.4 : Double.MAX_VALUE);
            }
        }
        double previousTime = subject.getControlNetlist().getSimulationTime();
        assertThrows(IllegalArgumentException.class, () -> {
            if (restart) subject.getControlNetlist().initializeAtSimulationStart(0.2);
            else subject.getControlNetlist().executeTimeStep(0.2, 0.5);
        });
        assertEquals(previousTime, subject.getControlNetlist().getSimulationTime());
        var actual = ScopeDataExtractor.fromScopeInfo(subject.getMapping().getScopeInfo());
        var expected = ScopeDataExtractor.fromScopeInfo(reference.getMapping().getScopeInfo());
        for (int step = 4; step < 10; step++) {
            subject.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            reference.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            assertArrayEquals(expected.extractCurrentValues(), actual.extractCurrentValues(), "retry " + step);
        }
    }

    @Test
    void preparationRejectsAnUnrepresentableDelayHistory() {
        var model = new CircuitModel();
        model.setTimeStep(1e-9);
        model.setSimulationDuration(1e-9);
        model.addControlComponent(component(25, "delay", new double[]{3}, new String[]{""}, "delayed"));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class, outcome);
        assertEquals(ReadinessReport.Outcome.INVALID, invalid.report().outcome());
        assertTrue(invalid.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PARAMETER")
                && "delay".equals(d.component())));
        assertThrows(IllegalArgumentException.class,
                () -> new ControlCalculatorFactory().build(model.getControlComponents(), 1e-9));
    }

    private static ControlCalculatorFactory.BuildResult delayedPair() {
        return new ControlCalculatorFactory().build(List.of(
                component(58, "clock", new double[0], new String[0], "drive"),
                component(25, "first", new double[]{0.2}, new String[]{"drive"}, "first"),
                component(25, "second", new double[]{0.3}, new String[]{"drive"}, "second"),
                component(5, "scope", new double[0], new String[]{"first", "second"})), 0.1);
    }
}

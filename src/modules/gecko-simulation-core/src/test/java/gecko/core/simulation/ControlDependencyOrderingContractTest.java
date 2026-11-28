package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class ControlDependencyOrderingContractTest {
    @Test
    void reversedGainChainReachesTheScopeOnTheFirstTimeStep() {
        var result = new ControlCalculatorFactory().build(List.of(
                block(5, "scope", new double[0], new String[]{"out"}, new String[0]),
                block(7, "second", new double[]{4}, new String[]{"middle"}, new String[]{"out"}),
                block(7, "first", new double[]{2}, new String[]{"raw"}, new String[]{"middle"}),
                block(3, "source", new double[]{3}, new String[0], new String[]{"raw"})));

        result.getControlNetlist().executeTimeStep(0.01, 0);

        var scope = ScopeDataExtractor.fromScopeInfo(result.getMapping().getScopeInfo());
        assertEquals(24, scope.extractCurrentValues()[0], 1e-6);
    }

    @ParameterizedTest
    @MethodSource("orders")
    void shuffledMultiStageChainsDoNotAddTimeStepDelays(int seed) {
        var components = List.of(
                block(3, "source", new double[]{3}, new String[0], new String[]{"a"}),
                block(7, "first", new double[]{2}, new String[]{"a"}, new String[]{"b"}),
                block(7, "second", new double[]{-1}, new String[]{"b"}, new String[]{"c"}),
                block(7, "third", new double[]{0.5}, new String[]{"c"}, new String[]{"d"}),
                block(7, "fourth", new double[]{-4}, new String[]{"d"}, new String[]{"out"}),
                block(5, "scope", new double[0], new String[]{"out"}, new String[0]));
        var result = new ControlCalculatorFactory().build(shuffled(components, seed));
        var scope = ScopeDataExtractor.fromScopeInfo(result.getMapping().getScopeInfo());
        for (int step = 0; step < 4; step++) {
            result.getControlNetlist().executeTimeStep(0.01, step * 0.01);
            assertEquals(12, scope.extractCurrentValues()[0], 1e-6);
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    void shuffledFanOutPreservesEveryScopeChannel(int seed) {
        var components = List.of(
                block(3, "source", new double[]{12}, new String[0], new String[]{"raw"}),
                block(7, "left", new double[]{2}, new String[]{"raw"}, new String[]{"left"}),
                block(7, "right", new double[]{-3}, new String[]{"raw"}, new String[]{"right"}),
                block(7, "tail", new double[]{0.5}, new String[]{"left"}, new String[]{"tail"}),
                block(5, "scope", new double[0], new String[]{"left", "right", "tail", "left"}, new String[0]));
        var result = new ControlCalculatorFactory().build(shuffled(components, seed));
        var scope = ScopeDataExtractor.fromScopeInfo(result.getMapping().getScopeInfo());
        result.getControlNetlist().executeTimeStep(0.01, 0);
        assertArrayEquals(new float[]{24, -36, 12, 24}, scope.extractCurrentValues(), 1e-6f);
        assertArrayEquals(new String[]{"left", "right", "tail", "left"}, scope.getSignalNames());
    }

    @Test
    void changingSourceValuesTraverseAReversedChainInTheSameTimeStep() {
        var result = new ControlCalculatorFactory().build(List.of(
                block(5, "scope", new double[0], new String[]{"out"}, new String[0]),
                block(7, "second", new double[]{0.5}, new String[]{"middle"}, new String[]{"out"}),
                block(7, "first", new double[]{2}, new String[]{"wave"}, new String[]{"middle"}),
                block(4, "source", new double[]{400, 2, 1, 0, 10, 0.5}, new String[0], new String[]{"wave"})));
        var scope = ScopeDataExtractor.fromScopeInfo(result.getMapping().getScopeInfo());
        double[] expected = {10, 12, 10, 8};
        for (int step = 0; step < expected.length; step++) {
            result.getControlNetlist().executeTimeStep(0.25, step * 0.25);
            assertEquals(expected[step], scope.extractCurrentValues()[0], 1e-6);
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    void algebraicFeedbackCannotUseFileOrderAsAnImplicitDelay(int seed) {
        var components = List.of(
                block(7, "first", new double[]{2}, new String[]{"b"}, new String[]{"a"}),
                block(7, "second", new double[]{0.25}, new String[]{"a"}, new String[]{"b"}),
                block(5, "scope", new double[0], new String[]{"b"}, new String[0]),
                block(3, "unrelated", new double[]{12}, new String[0], new String[]{"unused"}));
        var error = assertThrows(IllegalArgumentException.class,
                () -> new ControlCalculatorFactory().build(shuffled(components, seed)));
        assertTrue(error.getMessage().contains("CONTROL dependency cycle"), error.getMessage());
        assertTrue(error.getMessage().contains("first"), error.getMessage());
        assertTrue(error.getMessage().contains("second"), error.getMessage());
        assertFalse(error.getMessage().contains("unrelated"), error.getMessage());
    }

    @Test
    void anUnconnectedFactoryInputKeepsItsExistingZeroDefault() {
        var result = new ControlCalculatorFactory().build(List.of(
                block(7, "gain", new double[]{2}, new String[]{"missing"}, new String[]{"out"}),
                block(5, "scope", new double[0], new String[]{"out"}, new String[0])));
        result.getControlNetlist().executeTimeStep(0.01, 0);
        assertEquals(0, ScopeDataExtractor.fromScopeInfo(
                result.getMapping().getScopeInfo()).extractCurrentValues()[0]);
    }

    @Test
    void aFactoryGainWithoutNumericParametersKeepsUnityGain() {
        var result = new ControlCalculatorFactory().build(List.of(
                block(7, "gain", new double[0], new String[]{"raw"}, new String[]{"out"}),
                block(3, "source", new double[]{3}, new String[0], new String[]{"raw"}),
                block(5, "scope", new double[0], new String[]{"out"}, new String[0])));
        result.getControlNetlist().executeTimeStep(0.01, 0);
        assertEquals(3, ScopeDataExtractor.fromScopeInfo(
                result.getMapping().getScopeInfo()).extractCurrentValues()[0]);
    }

    @Test
    void theLegacyShortSignalDefaultStillExecutesBeforeItsConsumer() {
        var result = new ControlCalculatorFactory().build(List.of(
                block(7, "gain", new double[]{2}, new String[]{"wave"}, new String[]{"out"}),
                block(4, "source", new double[0], new String[0], new String[]{"wave"}),
                block(5, "scope", new double[0], new String[]{"out"}, new String[0])));
        var scope = ScopeDataExtractor.fromScopeInfo(result.getMapping().getScopeInfo());
        for (var calculator : result.getControlNetlist().getSortedCalculators()) {
            if (calculator instanceof InitializableAtSimulationStart initializable) {
                initializable.initializeAtSimulationStart(0.000125);
            }
        }
        double[] expected = {2, 2, 2, 0};
        for (int step = 0; step < expected.length; step++) {
            result.getControlNetlist().executeTimeStep(0.000125, (step + 1) * 0.000125);
            assertEquals(expected[step], scope.extractCurrentValues()[0]);
        }
    }

    @Test
    void aSelfConnectedGainIsAlsoAnAlgebraicCycle() {
        assertThrows(IllegalArgumentException.class, () -> new ControlCalculatorFactory().build(List.of(
                block(7, "self", new double[]{0.5}, new String[]{"loop"}, new String[]{"loop"}))));
    }

    static IntStream orders() {
        return IntStream.rangeClosed(-1, 31);
    }

    private static List<CircuitModel.ComponentData> shuffled(List<CircuitModel.ComponentData> components, int seed) {
        var result = new ArrayList<>(components);
        if (seed < 0) Collections.reverse(result);
        else Collections.shuffle(result, new Random(seed));
        return result;
    }

    private static CircuitModel.ComponentData block(int type, String name, double[] parameters,
                                                     String[] inputs, String[] outputs) {
        var component = new CircuitModel.ComponentData(type, name);
        component.setNumericParameters(parameters);
        component.setTerminalXLabels(inputs);
        component.setTerminalYLabels(outputs);
        return component;
    }
}

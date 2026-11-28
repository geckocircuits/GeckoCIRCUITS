package gecko.core.preparation;

import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;

import static gecko.core.preparation.CircuitPreparationTest.component;
import static gecko.core.preparation.CircuitPreparationTest.resistorCircuit;
import static org.junit.jupiter.api.Assertions.*;

class ControlOrderingPreparationContractTest {
    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true", "false,true"})
    void unusedNullTerminalSlotsPreserveReadyAndRejectedOutcomes(boolean nullOutput, boolean invalidSetting) {
        var model = resistorCircuit();
        var constant = component(3, "constant", new double[]{1}, "", "a");
        if (nullOutput) constant.setTerminalYLabels(new String[]{"a", null});
        else constant.setTerminalXLabels(new String[]{null});
        model.addControlComponent(constant);
        model.addControlComponent(component(5, "scope", new double[0], "a", ""));
        var preparation = new CircuitPreparation();
        var request = new PreparationRequest(
                invalidSetting ? new SolverOverrides(null, -1.0, null) : SolverOverrides.none(), Map.of());
        var result = preparation.prepare(preparation.capture(model), request);
        if (invalidSetting) {
            var rejected = assertInstanceOf(PreparationResult.Rejected.class, result);
            assertTrue(rejected.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_SETTING")
                    && "timeStep".equals(d.parameterPath())), rejected.report().toString());
        } else {
            var ready = assertInstanceOf(PreparationResult.Ready.class, result);
            var run = new HeadlessSimulationEngine().runSimulation(
                    ready.execution(), ExecutionOptions.defaults(), () -> false);
            assertTrue(run.isSuccess(), run.getErrorMessage());
            assertArrayEquals(new float[]{1, 1, 1}, run.getSignalData(0));
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    void preparedProbeChainsKeepTheSameTraceAfterShufflingAndRepeatedExecution(int seed) {
        var model = resistorCircuit();
        var probe = component(2, "probe", new double[0], "", "current");
        probe.setParameterStrings(new String[]{"R.1"});
        var controls = new ArrayList<>(List.of(probe,
                component(7, "first", new double[]{2}, "current", "middle"),
                component(7, "second", new double[]{-3}, "middle", "out"),
                component(5, "scope", new double[0], "out", "")));
        Collections.shuffle(controls, new Random(seed));
        controls.forEach(model::addControlComponent);
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        for (int run = 0; run < 2; run++) {
            var result = new HeadlessSimulationEngine().runSimulation(
                    ready.execution(), ExecutionOptions.defaults(), () -> false);
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertArrayEquals(new float[]{-3.6f, -3.6f, -3.6f}, result.getSignalData(0), 1e-6f);
        }
    }

    static IntStream orders() {
        return IntStream.range(0, 16);
    }

    @Test
    void invalidControlValuesReturnDiagnosticsBeforeConstructingTheDependencyGraph() {
        var model = resistorCircuit();
        model.addControlComponent(component(4, "signal", new double[]{400, -1, 1, 0, 0, 0.5}, "", "wave"));
        model.addControlComponent(component(5, "scope", new double[0], "wave", ""));
        var preparation = new CircuitPreparation();
        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> "CONTROL".equals(d.domain())
                && "signal".equals(d.component()) && d.category() == Diagnostic.Category.CONFIGURATION),
                result.report().toString());
    }

    @Test
    void algebraicFeedbackIsInspectableBeforeAnyExecution() {
        var model = resistorCircuit();
        model.addControlComponent(component(7, "first", new double[]{2}, "b", "a"));
        model.addControlComponent(component(7, "second", new double[]{0.25}, "a", "b"));
        model.addControlComponent(component(5, "scope", new double[0], "b", ""));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);

        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));

        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("CONTROL_DEPENDENCY_CYCLE")
                && d.category() == Diagnostic.Category.REFERENCES && "CONTROL".equals(d.domain())),
                result.report().toString());
        assertEquals(result.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
    }
}

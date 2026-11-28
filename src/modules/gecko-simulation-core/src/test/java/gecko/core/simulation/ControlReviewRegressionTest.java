package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ControlReviewRegressionTest {
    @Test
    void rejectedPendingDelayActivationPreservesTheActiveHistory() {
        var subject = delayedFeedback();
        var reference = delayedFeedback();
        subject.getControlNetlist().initializeAtSimulationStart(0.1);
        reference.getControlNetlist().initializeAtSimulationStart(0.1);
        for (int step = 0; step < 4; step++) {
            subject.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            reference.getControlNetlist().executeTimeStep(0.1, step * 0.1);
        }
        for (var calculator : subject.getControlNetlist().getSortedCalculators()) {
            if (calculator instanceof gecko.core.control.calculators.DelayCalculator delay) delay.setDelayTime(0);
        }
        assertThrows(ControlCalculatorFactory.DependencyCycleException.class,
                () -> subject.getControlNetlist().initializeAtSimulationStart(0.1));
        var actual = ScopeDataExtractor.fromScopeInfo(subject.getMapping().getScopeInfo());
        var expected = ScopeDataExtractor.fromScopeInfo(reference.getMapping().getScopeInfo());
        for (int step = 4; step < 8; step++) {
            subject.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            reference.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            assertArrayEquals(expected.extractCurrentValues(), actual.extractCurrentValues());
        }
    }

    @Test
    void disconnectedInputWarningsCannotHideAnInstantaneousCycle() {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        model.addControlComponent(component(7, "gain", new double[]{2}, new String[]{"loop"}, "loop"));
        model.addControlComponent(component(53, "counter", new double[0], new String[]{"loop", ""}, "count"));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class, outcome);
        assertTrue(invalid.report().diagnostics().stream().anyMatch(d -> d.code().equals("CONTROL_DEPENDENCY_CYCLE")));
        assertTrue(invalid.report().diagnostics().stream().anyMatch(d -> d.code().equals("MISSING_CONTROL_INPUT")
                && d.severity() == Diagnostic.Severity.WARNING));
    }

    @ParameterizedTest
    @CsvSource({"12,0", "14,1", "19,1", "20,0", "47,1"})
    void definedZeroInputIdentitiesRemainExecutable(int type, float expected) {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(type, "empty", new double[0], new String[0], "result"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"result"}));
        var ready = prepare(model);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{expected, expected}, result.getSignalData(0));
    }

    @Test
    void publishedAliasesCannotChangeThePreparationCatalog() {
        var model = new CircuitModel();
        model.addControlComponent(component(7, "gain", new double[]{3}, new String[]{""}, "output"));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var before = preparation.configurationFields(snapshot);
        Object exposed = ControlBlockDescriptor.find(7).settings().get(0).aliases();
        String original = exposed instanceof String[] array ? array[0] : null;
        try {
            if (exposed instanceof String[] array) array[0] = "changedAlias";
            else if (exposed instanceof List<?> list) assertThrows(UnsupportedOperationException.class, list::clear);
            assertEquals(before, preparation.configurationFields(snapshot));
        } finally {
            if (exposed instanceof String[] array) array[0] = original;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 405, 406})
    void randomAndImportSourcesRejectExternalModulationInPreparationAndAssembly(int subtype) {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        var source = component(4, "source", new double[]{subtype, 1, 1, 0, 0, 0.5, 1},
                new String[]{"", "", "", "", ""}, "signal");
        source.getImportTokens().put("externalDataFileHashValue", "42");
        model.getResources().put(42L, new gecko.core.io.CircuitResource(42, "wave.txt", "owned bytes",
                "0 1\n1 2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), List.of()));
        model.addControlComponent(source);
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class, outcome);
        assertEquals(ReadinessReport.Outcome.INVALID, invalid.report().outcome());
        assertTrue(invalid.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PARAMETER")
                && d.message().contains("external modulation")), invalid.report().toString());
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new ControlCalculatorFactory().buildCircuit(model, 0.1));
        assertTrue(failure.getMessage().contains("external modulation"));
    }

    @ParameterizedTest
    @ValueSource(ints = {13, 49, 50})
    void variableBlocksThatReadTheirFirstInputRejectZeroPorts(int type) {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(type, "empty", new double[0], new String[0], "result"));
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class, outcome);
        assertEquals(ReadinessReport.Outcome.INVALID, invalid.report().outcome());
        assertTrue(invalid.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PARAMETER")
                && "empty".equals(d.component())));
        assertThrows(IllegalArgumentException.class, () -> new ControlCalculatorFactory().build(model.getControlComponents()));
    }

    @ParameterizedTest
    @ValueSource(ints = {13, 49, 50})
    void oneInputRemainsValidForVariableSubtractionAndExtrema(int type) {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(3, "source", new double[]{7}, new String[0], "drive"));
        model.addControlComponent(component(type, "single", new double[0], new String[]{"drive"}, "result"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"result"}));
        var ready = prepare(model);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{7, 7}, result.getSignalData(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"blank", "omitted", "floatingLabel"})
    void disconnectedIntegratorResetUsesZero(String reset) {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(3, "source", new double[]{2}, new String[0], "drive"));
        String[] inputs = reset.equals("omitted") ? new String[]{"drive"}
                : new String[]{"drive", reset.equals("blank") ? "" : reset};
        model.addControlComponent(component(64, "integrator", new double[]{1, 0, -10, 10}, inputs, "integral"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"integral"}));
        var ready = prepare(model);
        assertTrue(ready.report().diagnostics().stream().anyMatch(d -> d.severity() == Diagnostic.Severity.WARNING
                && "integrator".equals(d.component())));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{0.5f, 1.5f}, result.getSignalData(0));
    }

    @ParameterizedTest
    @CsvSource({"53,1", "26,0"})
    void disconnectedCounterResetAndSampleClockUseZero(int type, float expected) {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(3, "source", new double[]{2}, new String[0], "drive"));
        model.addControlComponent(component(type, "block", new double[0], new String[]{"drive", ""}, "result"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"result"}));
        var ready = prepare(model);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{expected, expected}, result.getSignalData(0));
    }

    private static PreparationResult.Ready prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pt2FeedbackUsesThePreviousSettledInputInEitherSavedOrder(boolean reverse) {
        var blocks = new ArrayList<>(List.of(
                component(12, "sum", new double[0], new String[]{"one", "feedback"}, "sum"),
                component(9, "pt2", new double[]{1, 0.5}, new String[]{"sum"}, "feedback"),
                component(3, "one", new double[]{1}, new String[0], "one"),
                component(5, "scope", new double[0], new String[]{"sum", "feedback"})));
        if (reverse) Collections.reverse(blocks);
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(1);
        model.getControlComponents().addAll(blocks);
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{1, 1.125f, 1.359375f}, result.getSignalData(0));
        assertArrayEquals(new float[]{0, 0.125f, 0.359375f}, result.getSignalData(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aRejectedStepChangeOrRestartPreservesThePreviousHistory(boolean restart) {
        var subject = delayedFeedback();
        var reference = delayedFeedback();
        subject.getControlNetlist().initializeAtSimulationStart(0.1);
        reference.getControlNetlist().initializeAtSimulationStart(0.1);
        for (int step = 0; step < 4; step++) {
            subject.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            reference.getControlNetlist().executeTimeStep(0.1, step * 0.1);
        }
        double previousTime = subject.getControlNetlist().getSimulationTime();
        assertThrows(ControlCalculatorFactory.DependencyCycleException.class, () -> {
            if (restart) subject.getControlNetlist().initializeAtSimulationStart(0.3);
            else subject.getControlNetlist().executeTimeStep(0.3, 0.6);
        });
        assertEquals(previousTime, subject.getControlNetlist().getSimulationTime());
        var actual = ScopeDataExtractor.fromScopeInfo(subject.getMapping().getScopeInfo());
        var expected = ScopeDataExtractor.fromScopeInfo(reference.getMapping().getScopeInfo());
        for (int step = 4; step < 12; step++) {
            subject.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            reference.getControlNetlist().executeTimeStep(0.1, step * 0.1);
            assertArrayEquals(expected.extractCurrentValues(), actual.extractCurrentValues(), "retry step " + step);
        }
    }

    private static ControlCalculatorFactory.BuildResult delayedFeedback() {
        return new ControlCalculatorFactory().build(List.of(
                component(12, "sum", new double[0], new String[]{"one", "feedback"}, "sum"),
                component(25, "delay", new double[]{0.2}, new String[]{"sum"}, "feedback"),
                component(3, "one", new double[]{1}, new String[0], "one"),
                component(5, "scope", new double[0], new String[]{"sum", "feedback"})), 0.1);
    }

    static CircuitModel.ComponentData component(int type, String name, double[] values, String[] inputs, String... outputs) {
        var data = new CircuitModel.ComponentData(type, name);
        data.setNumericParameters(values);
        data.setTerminalXLabels(inputs);
        data.setTerminalYLabels(outputs);
        return data;
    }
}

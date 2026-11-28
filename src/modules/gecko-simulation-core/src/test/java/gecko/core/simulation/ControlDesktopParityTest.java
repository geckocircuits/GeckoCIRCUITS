package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.SignalCalculatorSinus;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.ExecutionOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ControlDesktopParityTest {
    @Test
    void externallyModulatedSourceUsesItsFiveInputSignals() {
        var model = new CircuitModel();
        model.setTimeStep(0.25);
        model.setSimulationDuration(0.5);
        String[] names = {"amplitude", "frequency", "offset", "phase", "duty"};
        double[] values = {2, 1, 3, 0, 0.5};
        for (int index = 0; index < names.length; index++) {
            model.addControlComponent(component(3, names[index], new double[]{values[index]}, new String[0], names[index]));
        }
        model.addControlComponent(component(4, "modulated", new double[]{400, 1, 1, 0, 0, 0.5, 1}, names, "signal"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"signal"}));
        var result = runPrepared(model);
        assertEquals(3, result.getDataContainer().getValue(0, 0), 1e-6);
        assertEquals(5, result.getDataContainer().getValue(0, 1), 1e-6);
        assertEquals(3, result.getDataContainer().getValue(0, 2), 1e-6);
    }

    @Test
    void importedSourceUsesOwnedResourceBytesAndRepeatsItsSignal() {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(2.5);
        var source = component(4, "import", new double[]{406, 1, 1, 0, 0, 0.5}, new String[0], "signal");
        source.getImportTokens().put("externalDataFileHashValue", "42");
        source.setSavedBlock(gecko.core.io.SavedCircuitBlock.parse("ElementCONTROL",
                new String[]{"uniqueObjectIdentifier 123", "externalDataFileHashValue 42"}));
        model.addControlComponent(source);
        model.getResources().put(42L, new gecko.core.io.CircuitResource(42, "wave.txt", "/missing/wave.txt",
                "10 2\n11 4\n12 2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), List.of(123L)));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"signal"}));
        var result = runPrepared(model);
        double[] expected = {2, 3, 4, 3, 2, 3};
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], result.getDataContainer().getValue(0, index), 1e-6);
        }
    }

    private static SimulationResult runPrepared(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result;
    }
    @Test
    void delayedFeedbackPreservesHistoryWhenTheStepChanges() {
        var built = new ControlCalculatorFactory().build(List.of(
                component(12, "sum", new double[0], new String[]{"one", "feedback"}, "sum"),
                component(25, "delay", new double[]{1}, new String[]{"sum"}, "feedback"),
                component(3, "one", new double[]{1}, new String[0], "one"),
                component(5, "scope", new double[0], new String[]{"sum"})), 1);
        var runtime = built.getControlNetlist();
        runtime.initializeAtSimulationStart(1);
        runtime.executeTimeStep(1, 0);
        runtime.executeTimeStep(1, 1);
        runtime.executeTimeStep(0.5, 1.5);
        assertEquals(2.5, built.getMapping().getScopeInfo().getCalculator()._inputSignal[0][0], 1e-12);
        runtime.executeTimeStep(0.5, 2);
        assertEquals(3, built.getMapping().getScopeInfo().getCalculator()._inputSignal[0][0], 1e-12);
        assertThrows(ControlCalculatorFactory.DependencyCycleException.class,
                () -> runtime.executeTimeStep(2, 4));
    }
    @Test
    void preparedDelayFeedbackUsesThePreviousCommittedSample() {
        var model = new CircuitModel();
        model.setTimeStep(1);
        model.setSimulationDuration(2);
        model.addControlComponent(component(12, "sum", new double[0], new String[]{"one", "feedback"}, "sum"));
        model.addControlComponent(component(25, "delay", new double[]{1}, new String[]{"sum"}, "feedback"));
        model.addControlComponent(component(3, "one", new double[]{1}, new String[0], "one"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"sum", "feedback"}));
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(1, result.getDataContainer().getValue(0, 0));
        assertEquals(2, result.getDataContainer().getValue(0, 1));
        assertEquals(3, result.getDataContainer().getValue(0, 2));
        assertEquals(0, result.getDataContainer().getValue(1, 0));
        assertEquals(1, result.getDataContainer().getValue(1, 1));
        assertEquals(2, result.getDataContainer().getValue(1, 2));
    }
    @Test
    void multipleScopesKeepChannelOrderAndDisconnectedChannels() {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.1);
        model.addControlComponent(component(3, "left", new double[]{2}, new String[0], "leftValue"));
        model.addControlComponent(component(3, "right", new double[]{7}, new String[0], "rightValue"));
        model.addControlComponent(component(5, "scopeA", new double[0], new String[]{"rightValue", "", "leftValue"}));
        model.addControlComponent(component(5, "scopeB", new double[0], new String[]{"leftValue", "rightValue"}));
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(3, result.getDataContainer().getRowLength());
        assertEquals(2, result.getDataContainer().getValue(0, 0));
        assertEquals(7, result.getDataContainer().getValue(1, 0));
        assertEquals(0, result.getDataContainer().getValue(2, 0));
        assertEquals("leftValue", result.getDataContainer().getSignalName(0));
        assertEquals("rightValue", result.getDataContainer().getSignalName(1));
        @SuppressWarnings("unchecked")
        var channels = (List<ScopeDataExtractor.ScopeChannel>) result.getMetadata().get("scopeChannels");
        assertEquals(List.of(1, 2, 0, 0, 1), channels.stream().map(ScopeDataExtractor.ScopeChannel::row).toList());
    }
    @ParameterizedTest
    @CsvSource({"9,1;3,0,1.5", "10,1;2,3,5", "11,0.5;0,1,1",
            "25,0.5,0,2", "26,0,2,2", "27,-1;1;0,1,1", "29,3,12,0",
            "53,0,1,1", "64,2;4;-10;10,5,7"})
    void preparedStatefulBlocksKeepTheirHistory(int type, String saved, double first, double second) {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(3, "source", new double[]{2}, new String[0], "input"));
        model.addControlComponent(component(3, "auxiliary", new double[]{type == 26 ? 1 : 0}, new String[0], "aux"));
        double[] parameters = java.util.Arrays.stream(saved.split(";")).mapToDouble(Double::parseDouble).toArray();
        boolean twoInputs = type == 26 || type == 53 || type == 64;
        model.addControlComponent(component(type, "stateful", parameters,
                twoInputs ? new String[]{"input", "aux"} : new String[]{"input"}, "result"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"result"}));
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var engine = new HeadlessSimulationEngine();
        var result = engine.runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(first, result.getDataContainer().getValue(0, 0), 1e-6);
        assertEquals(second, result.getDataContainer().getValue(0, 1), 1e-6);
        var restarted = engine.runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(restarted.isSuccess(), restarted.getErrorMessage());
        assertEquals(first, restarted.getDataContainer().getValue(0, 0), 1e-6);
    }

    @Test
    void independentNetlistsKeepTheirTimeWhenCalculationsInterleave() {
        var firstSignal = new SignalCalculatorSinus(0, 1, 1, 0, 0, 0.5);
        var secondSignal = new SignalCalculatorSinus(0, 1, 1, 0, 0, 0.5);
        var secondRun = ControlNetlist.fromOrderedCalculators(List.of(secondSignal));
        var interleaving = new AbstractControlCalculatable(0, 0) {
            @Override public void berechneYOUT(double dt) { secondRun.executeTimeStep(dt, 0.75); }
        };
        var firstRun = ControlNetlist.fromOrderedCalculators(List.of(interleaving, firstSignal));
        firstRun.executeTimeStep(0.01, 0.25);
        assertEquals(1, firstSignal._outputSignal[0][0], 1e-12);
        assertEquals(-1, secondSignal._outputSignal[0][0], 1e-12);
    }
    @Test
    void preparedNamedPt1SettingsExecuteTheSavedFilter() {
        var model = new CircuitModel();
        model.setTimeStep(0.5);
        model.setSimulationDuration(1);
        model.addControlComponent(component(3, "source", new double[]{2}, new String[0], "input"));
        var filter = component(8, "filter", new double[0], new String[]{"input"}, "filtered");
        filter.getParameters().put("T", 1.0);
        filter.getParameters().put("a1", 3.0);
        model.addControlComponent(filter);
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"filtered"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var runtime = new ControlCalculatorFactory().build(ready.execution().newRunModel().getControlComponents());
        runtime.getControlNetlist().executeTimeStep(0.5, 0);
        assertEquals(1.2, runtime.getMapping().getScopeInfo().getCalculator()._inputSignal[0][0], 1e-12);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(1.2, result.getDataContainer().getValue(0, 0), 1e-6);
        assertEquals(3.12, result.getDataContainer().getValue(0, 1), 1e-6);
    }
    @ParameterizedTest
    @CsvSource({"12,3.5", "13,-2.5", "14,1.5", "15,0.16666666666666666",
            "18,0", "19,0", "20,1", "21,0", "32,0.5", "33,1",
            "34,0.479425538604203", "35,0.5235987755982989", "36,0.8775825618903728",
            "37,1.0471975511965979", "38,0.5463024898437905", "39,0.4636476090008061",
            "40,1.6487212707001282", "41,-0.6931471805599453", "42,0.25", "43,0.7071067811865476",
            "44,0.125", "45,0", "46,0", "47,0", "48,1", "49,0.5", "50,3", "51,1"})
    void savedMathBlocksEvaluateTheirInputs(int type, double expected) {
        boolean unary = type == 18 || type >= 32 && type <= 43 || type == 51;
        var left = component(3, "left", new double[]{0.5}, new String[0], "leftValue");
        var right = component(3, "right", new double[]{3}, new String[0], "rightValue");
        var operation = component(type, "math", new double[0], unary
                ? new String[]{"leftValue"} : new String[]{"leftValue", "rightValue"}, "result");
        var scope = component(5, "scope", new double[0], new String[]{"result"});
        var runtime = new ControlCalculatorFactory().build(List.of(scope, operation, right, left));
        runtime.getControlNetlist().executeTimeStep(0.1, 0);
        assertEquals(expected, runtime.getMapping().getScopeInfo().getCalculator()._inputSignal[0][0], 1e-12);
    }

    @Test
    void savedPt1FiltersAConstantInput() {
        var source = component(3, "source", new double[]{2}, new String[0], "input");
        var filter = component(8, "filter", new double[]{1, 3}, new String[]{"input"}, "filtered");
        var scope = component(5, "scope", new double[0], new String[]{"filtered"});
        var runtime = new ControlCalculatorFactory().build(List.of(scope, filter, source));
        runtime.getControlNetlist().executeTimeStep(0.5, 0);
        assertEquals(1.2, runtime.getMapping().getScopeInfo().getCalculator()._inputSignal[0][0], 1e-12);
    }

    static CircuitModel.ComponentData component(int type, String name, double[] parameters, String[] inputs, String... outputs) {
        var component = new CircuitModel.ComponentData(type, name);
        component.setNumericParameters(parameters);
        component.setTerminalXLabels(inputs);
        component.setTerminalYLabels(outputs);
        return component;
    }
}

package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class ControlBusExecutionTest {
    @Test
    void preparedMuxAndDemuxPreserveBusAndScopePortOrder() {
        var model = model();
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"out0", "out1", "out2"}));
        model.addControlComponent(component(85, "demux", new double[0], new String[]{"bus"}, "out2", "out0", "out1"));
        model.addControlComponent(component(84, "mux", new double[0], new String[]{"left", "middle", "right"}, "bus"));
        model.addControlComponent(component(3, "right", new double[]{9}, new String[0], "right"));
        model.addControlComponent(component(3, "left", new double[]{2}, new String[0], "left"));
        model.addControlComponent(component(3, "middle", new double[]{5}, new String[0], "middle"));
        var result = run(model);
        assertArrayEquals(new String[]{"out0", "out1", "out2"}, result.getSignalNames());
        assertArrayEquals(new float[]{5, 5, 5}, result.getSignalData(0));
        assertArrayEquals(new float[]{9, 9, 9}, result.getSignalData(1));
        assertArrayEquals(new float[]{2, 2, 2}, result.getSignalData(2));
    }

    @Test
    void disconnectedDemuxProvidesOneZeroForEachOutput() {
        var model = model();
        model.addControlComponent(component(85, "demux", new double[0], new String[]{""}, "first", "second", "third"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"third", "first", "second"}));
        var result = run(model);
        assertEquals(3, result.getSignalNames().length);
        for (int row = 0; row < 3; row++) assertArrayEquals(new float[]{0, 0, 0}, result.getSignalData(row));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 2, 4})
    void mismatchedDemuxWidthIsRejectedBeforeExecution(int busWidth) {
        var model = model();
        model.addControlComponent(component(84, "mux", new double[0], new String[busWidth], "bus"));
        model.addControlComponent(component(85, "demux", new double[0], new String[]{"bus"}, "first", "second", "third"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"first"}));
        var preparation = new CircuitPreparation();
        var outcome = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, outcome.report().outcome());
        assertTrue(outcome.report().diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("INVALID_CONTROL_WIDTH") && "demux".equals(diagnostic.component())),
                outcome.report().toString());
        assertThrows(IllegalArgumentException.class, () -> new ControlCalculatorFactory().buildCircuit(model, 0.1));
    }

    private static CircuitModel model() {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        return model;
    }

    private static SimulationResult run(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result;
    }
}

package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.io.CircuitModel;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExternalRuntimeValidationTest {
    @Test
    void invalidExternalPortsAndWidthsDoNotCorruptTheNextExchange() {
        var model = new CircuitModel();
        var input = new CircuitModel.ComponentData(23, "input");
        input.setTerminalYLabels(new String[]{"a", "b"});
        input.setParameter("externalWidth", 2);
        var output = new CircuitModel.ComponentData(22, "output");
        output.setTerminalXLabels(new String[]{"a", "b"});
        model.addControlComponent(input);
        model.addControlComponent(output);
        try (var runtime = ControlRuntime.fromDeferred(model, 0.1)) {
            assertEquals(1, runtime.externalInputPorts());
            assertEquals(1, runtime.externalOutputPorts());
            assertEquals(2, runtime.externalInputWidth(0));
            assertEquals(2, runtime.externalOutputWidth(0));
            for (int port : new int[]{-1, 1}) {
                assertThrows(IllegalArgumentException.class, () -> runtime.externalInputWidth(port));
                assertThrows(IllegalArgumentException.class, () -> runtime.externalOutputWidth(port));
                assertThrows(IllegalArgumentException.class, () -> runtime.setExternalInputs(port, new double[2]));
                assertThrows(IllegalArgumentException.class, () -> runtime.externalOutputs(port));
            }
            assertThrows(IllegalArgumentException.class, () -> runtime.setExternalInputs(0, null));
            assertThrows(IllegalArgumentException.class, () -> runtime.setExternalInputs(0, new double[1]));
            runtime.setExternalInputs(0, new double[]{3, -4});
            runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
            assertArrayEquals(new double[]{3, -4}, runtime.externalOutputs(0));
        }
    }

    @Test
    void invalidStepIsRejectedAndRepeatedCalculatorsHaveOneRuntimeOwner() {
        for (double dt : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> ControlRuntime.fromDeferred((CircuitModel) null, dt));
        }
        var calculator = new NothingToDoCalculator(0, 1);
        var netlist = ControlNetlist.fromOrderedCalculators(Arrays.asList(calculator, null, calculator));
        try (var runtime = ControlRuntime.fromDeferred(
                new ControlCalculatorFactory.BuildResult(netlist, new ControlBlockMapping()), 0.1)) {
            runtime.initializeAtSimulationStart();
            runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
            assertArrayEquals(new float[0], runtime.sampleScope());
        }
    }
}

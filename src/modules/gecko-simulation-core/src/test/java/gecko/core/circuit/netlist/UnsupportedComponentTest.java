package gecko.core.circuit.netlist;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.ControlCalculatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnsupportedComponentTest {
    @Test
    void unknownElectricalTypesFailWithAndWithoutTerminalLabels() {
        for (boolean labels : new boolean[]{false, true}) {
            CircuitModel model = new CircuitModel();
            CircuitModel.ComponentData component = new CircuitModel.ComponentData(999999, "unknown-electrical");
            if (labels) {
                component.setTerminalXLabels(new String[]{"1"});
                component.setTerminalYLabels(new String[]{"0"});
            }
            model.addCircuitComponent(component);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> NetlistBuilder.buildFromCircuitModel(model));
            assertTrue(error.getMessage().contains("999999"));
            assertTrue(error.getMessage().contains("unknown-electrical"));
        }
    }

    @Test
    void knownElectricalTypesWithoutAStamperAreRejected() {
        CircuitModel model = new CircuitModel();
        model.addCircuitComponent(new CircuitModel.ComponentData(CircuitTypCore.LK_MOTOR.getTypeNumber(), "motor"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().contains("motor"));
    }

    @Test
    void unknownControlTypesCannotDisappearFromTheModel() {
        CircuitModel.ComponentData component = new CircuitModel.ComponentData(999999, "unknown-control");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ControlCalculatorFactory().build(List.of(component)));
        assertTrue(error.getMessage().contains("999999"));
        assertTrue(error.getMessage().contains("unknown-control"));
    }
}

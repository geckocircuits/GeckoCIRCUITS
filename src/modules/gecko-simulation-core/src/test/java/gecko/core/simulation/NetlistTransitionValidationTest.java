package gecko.core.simulation;

import gecko.core.control.calculators.NothingToDoCalculator;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetlistTransitionValidationTest {
    @Test
    void stagedStepChangeRequiresInitializationAndRejectsInvalidGrids() {
        var netlist = new ControlNetlist();
        assertThrows(IllegalStateException.class, () -> netlist.prepareTimeStepChange(0.1));
        for (double dt : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> netlist.prepareInitialization(dt));
            assertThrows(IllegalArgumentException.class, () -> netlist.executeTimeStep(dt, 0));
            assertThrows(IllegalArgumentException.class, () -> netlist.prepareTimeStepChange(dt));
        }
        netlist.setSortedCalculators(Arrays.asList(null, new NothingToDoCalculator(0, 1)));
        netlist.initializeAtSimulationStart(0.1);
        netlist.prepareTimeStepChange(0.2).run();
        netlist.executeTimeStep(0.2, 1);
        assertEquals(1, netlist.getSimulationTime());
    }
}

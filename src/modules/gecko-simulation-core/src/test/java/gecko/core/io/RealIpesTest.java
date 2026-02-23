package gecko.core.io;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: parses a real .ipes file and verifies component extraction.
 */
public class RealIpesTest {

    private static final String IPES_DIR =
        "/home/tinix/claude_wsl/GeckoCIRCUITS/resources/application_examples/education/Education_ETHZ/";

    @Test
    void parseEx1_simulationParameters() throws Exception {
        File f = new File(IPES_DIR + "ex_1.ipes");
        assumeFileExists(f);

        CircuitModel model = new CircuitFileParser().parse(f.getAbsolutePath());
        assertTrue(model.getSimulationDuration() > 0, "Duration > 0");
        assertTrue(model.getTimeStep() > 0, "TimeStep > 0");
        System.out.println("ex_1.ipes  duration=" + model.getSimulationDuration()
                + " dt=" + model.getTimeStep()
                + " components=" + model.getCircuitComponents().size());
    }

    @Test
    void parseEx1_circuitComponents() throws Exception {
        File f = new File(IPES_DIR + "ex_1.ipes");
        assumeFileExists(f);

        CircuitModel model = new CircuitFileParser().parse(f.getAbsolutePath());
        for (CircuitModel.ComponentData c : model.getCircuitComponents()) {
            System.out.printf("  [type=%d] %-12s params=%s  xL=%s  yL=%s%n",
                    c.getType(), c.getName(), c.getParameters(),
                    Arrays.toString(c.getTerminalXLabels()),
                    Arrays.toString(c.getTerminalYLabels()));
        }
        // At minimum the file should parse without throwing
        assertNotNull(model.getCircuitComponents());
    }

    @Test
    void parseEx1_netlistFromLabels() throws Exception {
        File f = new File(IPES_DIR + "ex_1.ipes");
        assumeFileExists(f);

        CircuitModel model = new CircuitFileParser().parse(f.getAbsolutePath());
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        System.out.printf("Netlist: nodes=%d  vSrc=%d  elements=%d%n",
                netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());

        // If components were parsed, netlist should reflect them
        if (!model.getCircuitComponents().isEmpty()) {
            assertEquals(model.getCircuitComponents().size(), netlist.getElementCount(),
                    "Element count should match component count");
        }
    }

    @Test
    void parseEx3Pwm_simulationParameters() throws Exception {
        File f = new File(IPES_DIR + "ex_3_pwm.ipes");
        assumeFileExists(f);

        CircuitModel model = new CircuitFileParser().parse(f.getAbsolutePath());
        assertTrue(model.getSimulationDuration() > 0, "Duration > 0");
        System.out.println("ex_3_pwm.ipes  duration=" + model.getSimulationDuration()
                + " dt=" + model.getTimeStep()
                + " components=" + model.getCircuitComponents().size());
        for (CircuitModel.ComponentData c : model.getCircuitComponents()) {
            System.out.printf("  [type=%d] %-12s param0=%s  xL=%s  yL=%s%n",
                    c.getType(), c.getName(),
                    c.getParameters().get("param0"),
                    Arrays.toString(c.getTerminalXLabels()),
                    Arrays.toString(c.getTerminalYLabels()));
        }
    }

    /** Skip the test gracefully if the .ipes file is absent (CI/CD without resources). */
    private static void assumeFileExists(File f) {
        org.junit.jupiter.api.Assumptions.assumeTrue(f.exists(),
                "Skipping: .ipes file not found at " + f.getAbsolutePath());
    }
}

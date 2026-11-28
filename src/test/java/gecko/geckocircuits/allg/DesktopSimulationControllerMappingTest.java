package gecko.geckocircuits.allg;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class DesktopSimulationControllerMappingTest {
    @ParameterizedTest
    @ValueSource(strings = {"auto", "core"})
    void aMappingDecisionPreservesTheSelectedBackendBehavior(String backend) throws Exception {
        String previous = System.getProperty("gecko.backend");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        var aborted = new ArrayList<String>();
        var messages = new ArrayList<String>();
        var logger = java.util.logging.Logger.getLogger("gecko.backend.fallback");
        var handler = new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            System.setProperty("gecko.backend", backend);
            GeckoExternal.openFile(Path.of("src/test/resources/ipes/education/buck_simple.ipes").toAbsolutePath().toString());
            var window = GeckoSim._win;
            for (var component : window.jtfStatus.getComponents()) {
                if (component instanceof JLabel label) label.addPropertyChangeListener("text", event -> {
                    if ("Simulation aborted.".equals(event.getNewValue())) aborted.add(event.getNewValue().toString());
                });
            }
            var runner = new SimulationRunner(window, MainWindow._se, () -> {
                var netlist = NetListContainer.fabricStartSimulation(MainWindow._se, null);
                NetzlisteCONTROL.globalData.init(1, new String[]{"missing"}, "t");
                return netlist;
            });
            if ("core".equals(backend)) {
                var failure = assertThrows(IllegalStateException.class,
                        () -> runner.startCalculation(false, MainWindow._solverSettings));
                assertTrue(failure.getMessage().contains("SIGNAL_MAP:missing"), failure::toString);
                assertTrue(messages.isEmpty());
            } else {
                runner.startCalculation(false, MainWindow._solverSettings);
                assertNotNull(runner.simKern, "AUTO must complete through legacy");
                assertTrue(runner.getCurrentTime() > 0);
                assertEquals(ContainerStatus.PAUSED, NetzlisteCONTROL.globalData.getContainerStatus());
                assertEquals(java.util.List.of("buck_simple: SIGNAL_MAP:missing"), messages);
                assertTrue(aborted.isEmpty(), "AUTO mapping fallback must not report a failed run: " + aborted);
            }
        } finally {
            logger.removeHandler(handler);
            if (previous == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", previous);
            SwingUtilities.invokeAndWait(() -> {
                for (var window : java.awt.Window.getWindows()) window.dispose();
            });
        }
    }
}

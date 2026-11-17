package gecko;

import gecko.geckocircuits.allg.OperatingMode;
import java.nio.file.Path;
import java.awt.Window;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("regression")
class GeckoSimLocalTestModeTest {
    @Test
    void testingModeStartsLocalGuiAfterExternalOpenChangedModeToRemote() throws Exception {
        boolean previousTestingMode = GeckoSim._isTestingMode;
        OperatingMode previousMode = GeckoSim.operatingmode;
        try {
            GeckoSim._isTestingMode = true;
            GeckoSim.operatingmode = OperatingMode.REMOTE;

            GeckoSim.main(new String[0]);

            assertNotNull(GeckoSim._win, "Testing mode must initialize the local desktop window");
            assertNotNull(GeckoSim._win._simRunner, "Testing mode must initialize the local simulation runner");

            Path circuit = Path.of("src/test/resources/ipes/education/buck_simple.ipes").toAbsolutePath();
            GeckoExternal.openFile(circuit.toString());
            var previousWindow = GeckoSim._win;
            GeckoSim.main(new String[0]);
            assertNotNull(GeckoSim._win, "Testing mode must reinitialize the local desktop after a REMOTE transition");
            assertNotNull(GeckoSim._win._simRunner, "Reinitialized desktop must have a simulation runner");
            GeckoExternal.openFile(circuit.toString());
            assertEquals(circuit.toString(), GeckoSim._win.getOpenFileName(),
                    "External API must bind to the recreated local desktop");
            assertNotNull(previousWindow, "First local desktop must be initialized before recreation");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) window.dispose();
            });
            GeckoExternal.external = null;
            GeckoSim._isTestingMode = previousTestingMode;
            GeckoSim.operatingmode = previousMode;
        }
    }
}

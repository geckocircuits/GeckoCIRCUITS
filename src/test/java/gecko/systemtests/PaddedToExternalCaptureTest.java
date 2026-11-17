package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.simulation.ControlBlockDescriptor;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JDialog;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class PaddedToExternalCaptureTest {
    private static Path fixture;

    @BeforeAll
    static void startGecko() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        fixture = Path.of("resources/tutorials/7xx_scripting_automation/"
                + "703_simulink_cosimulation/vr1_simulink.ipes").toAbsolutePath();
        SwingUtilities.invokeAndWait(() -> { });
    }

    @AfterAll
    static void closeGecko() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var window : java.awt.Window.getWindows()) window.dispose();
        });
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 2, unit = TimeUnit.MINUTES)
    void unchangedSimulinkFixtureKeepsPaddedDataVectorAcrossParserAndEditorCapture() throws Exception {
        var parsed = new CircuitFileParser().parse(fixture.toString());
        Timer dismissCompatibilityNotice = new Timer(250, event -> {
            for (var window : java.awt.Window.getWindows()) {
                if (window instanceof JDialog dialog && ("Info".equals(dialog.getTitle())
                        || "Error!".equals(dialog.getTitle())) && dialog.isShowing()) {
                    dialog.dispose();
                }
            }
        });
        dismissCompatibilityNotice.start();
        try {
            GeckoExternal.openFile(fixture.toString());
        } finally {
            dismissCompatibilityNotice.stop();
        }
        var scripts = MainWindow._scripter;
        var captured = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                        scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), fixture.toString()).toModel();

        assertToExternalShape(parsed, false);
        assertToExternalShape(captured, true);
    }

    private static void assertToExternalShape(CircuitModel model, boolean expectCapturedAliases) {
        var component = model.getControlComponents().stream()
                .filter(candidate -> candidate.getType() == 22 && candidate.getName().equals("ToEXT.4"))
                .findFirst().orElseThrow();
        var descriptor = ControlBlockDescriptor.find(22);
        assertEquals(28, descriptor.inputs(component));
        assertEquals(28, descriptor.createCalculator(component, model)._inputSignal.length);
        assertEquals(28, component.getTerminalXLabels().length);
        assertEquals(3, component.getParameters().get("activeInputWidth"));

        var topology = CircuitTopology.resolve(model);
        for (int port = 0; port < 3; port++) {
            assertNotNull(topology.group(component, CircuitTopology.Side.INPUT, port), "active input " + port);
        }
        for (int port = 3; port < 28; port++) {
            assertNull(topology.group(component, CircuitTopology.Side.INPUT, port), "inactive input " + port);
        }
        if (expectCapturedAliases) {
            assertNotNull(topology.externalOutputGroup(component, 3),
                    "captured padded channel aliases must remain available to external output routing");
        }
    }
}

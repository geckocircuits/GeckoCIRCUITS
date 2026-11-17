package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.GeckoSimulink;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.allg.OperatingMode;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.control.ReglerFromEXTERNAL;
import gecko.geckocircuits.control.ReglerToEXTERNAL;
import java.io.InputStream;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class SimulinkCoreParityTest {
    private static final int SAMPLES = 1000;
    private static Properties previousProperties;
    private static boolean previousTestingMode;
    private static OperatingMode previousOperatingMode;

    @BeforeAll
    static void startApplication() throws Exception {
        previousProperties = GeckoSim.applicationProps;
        previousTestingMode = GeckoSim._isTestingMode;
        previousOperatingMode = GeckoSim.operatingmode;
        GeckoSim._isTestingMode = true;
        GeckoSim.applicationProps = new Properties();
        try (InputStream defaults = GeckoSim.class.getResourceAsStream("/defaultProperties.prp")) {
            assertNotNull(defaults);
            GeckoSim.applicationProps.load(defaults);
        }
        GeckoSim.applicationProps.setProperty("UPDATE_FREQ", "NEVER");
        GeckoSim.main(new String[]{});
        Thread.sleep(2000);
    }

    @AfterAll
    static void restoreApplication() {
        try {
            if (GeckoSim._win != null) GeckoSim._win._simRunner.endSimulation();
        } finally {
            ReglerFromEXTERNAL.clearFromExternals();
            ReglerToEXTERNAL.clearToExternals();
            GeckoSim.operatingmode = previousOperatingMode;
            GeckoSim._isTestingMode = previousTestingMode;
            GeckoSim.applicationProps = previousProperties;
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void externalStepsMatchLegacyOnEveryOutput() throws Exception {
        String previousBackend = System.getProperty("gecko.backend");
        double[][][] legacy = null;
        double[][][] core = null;
        try {
            for (String backend : new String[]{"legacy", "core"}) {
                System.setProperty("gecko.backend", backend);
                GeckoSimulink facade = new GeckoSimulink();
                openFixture();
                if (backend.equals("core")) {
                    var scripts = MainWindow._scripter;
                    var snapshot = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                            MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                            new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                                    scripts.getDeclarationCode(), scripts.getExtraFilesHashes()),
                            "resources/tutorials/7xx_scripting_automation/703_simulink_cosimulation/vr1_simulink.ipes");
                    var readiness = new CircuitPreparation().prepare(snapshot, PreparationRequest.defaults());
                    assertInstanceOf(PreparationResult.Ready.class, readiness,
                            () -> "Simulink fixture must be core-ready: " + readiness.report().diagnostics());
                }
                double dt = MainWindow._solverSettings.dt.getValue();
                boolean initialized = false;
                try {
                    facade.external_init((SAMPLES + 1) * dt);
                    initialized = true;
                    assertEquals(dt, facade.external_getdt());
                    if (backend.equals("core")) {
                        assertNotNull(GeckoSim._win._simRunner.getCoreStatus(), "core must own a core session");
                        assertEquals(0, GeckoSim._win._simRunner.getCoreStatus().nextTime(), dt / 1000);
                        assertNull(GeckoSim._win._simRunner.simKern);
                    } else {
                        assertNull(GeckoSim._win._simRunner.getCoreStatus());
                    }
                    assertTrue(facade.getNumInputPorts() > 0);
                    assertTrue(facade.getNumOutputPorts() > 0);
                    double[][][] outputs = new double[SAMPLES][facade.getNumOutputPorts()][];
                    long started = System.nanoTime();
                    for (int k = 0; k < SAMPLES; k++) {
                        facade.external_setScalarInputValue(Math.sin(k * dt), 0);
                        facade.external_step(k * dt);
                        for (int port = 0; port < outputs[k].length; port++) {
                            outputs[k][port] = facade.external_getValues(port).clone();
                            assertEquals(facade.external_getTerminalNumber_TO_EXTERNAL(port), outputs[k][port].length);
                        }
                    }
                    long elapsed = System.nanoTime() - started;
                    System.out.printf("SIMULINK %s: %d steps, %.3f ms, %.3f us/step%n",
                            backend, SAMPLES, elapsed / 1e6, elapsed / 1e3 / SAMPLES);
                    if (backend.equals("legacy")) legacy = outputs;
                    else core = outputs;
                } finally {
                    if (initialized) facade.external_end();
                    GeckoSim._win._simRunner.endSimulation();
                }
            }
        } finally {
            if (previousBackend == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", previousBackend);
        }
        assertNotNull(legacy);
        assertNotNull(core);
        for (int k = 0; k < SAMPLES; k++) {
            assertEquals(legacy[k].length, core[k].length);
            for (int port = 0; port < legacy[k].length; port++) {
                assertEquals(legacy[k][port].length, core[k][port].length);
                for (int index = 0; index < legacy[k][port].length; index++) {
                    double expected = legacy[k][port][index];
                    assertEquals(expected, core[k][port][index], Math.max(1e-9, 0.01 * Math.abs(expected)),
                            "step " + k + " port " + port + " channel " + index);
                }
            }
        }
    }

    private static void openFixture() throws Exception {
        Timer dismissCompatibilityNotice = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                // A throwing listener stalls the coalescing Timer, so skip dialogs without a root pane.
                if (window instanceof JDialog dialog && dialog.getRootPane() != null
                        && findOptionPane(dialog.getContentPane()) instanceof JOptionPane pane) {
                    String message = String.valueOf(pane.getMessage());
                    if (("Info".equals(dialog.getTitle()) && message.startsWith(
                            "This Model file was created with an older version of GeckoCIRCUITS."))
                            || ("Error!".equals(dialog.getTitle()) && message.startsWith(
                                    "There are circuit components located outside the selected sheet size "))
                            || message.equals("Cannot resize due to bad component location.")) {
                        pane.setValue(JOptionPane.OK_OPTION);
                    }
                }
            }
        });
        dismissCompatibilityNotice.setInitialDelay(100);
        SwingUtilities.invokeAndWait(dismissCompatibilityNotice::start);
        try {
            GeckoExternal.openFile(Path.of("resources/tutorials/7xx_scripting_automation/"
                    + "703_simulink_cosimulation/vr1_simulink.ipes").toAbsolutePath().toString());
        } finally {
            SwingUtilities.invokeAndWait(dismissCompatibilityNotice::stop);
        }
    }

    private static Component findOptionPane(Container container) {
        if (container instanceof JOptionPane) return container;
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                Component found = findOptionPane(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

}

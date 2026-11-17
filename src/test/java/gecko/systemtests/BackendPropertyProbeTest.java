package gecko.systemtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class BackendPropertyProbeTest {
    /** Editor-capture fallbacks expected under AUTO: none, every education circuit routes to core. */
    private static final Map<String, Set<String>> CAPTURE_FALLBACKS = Map.of();

    @Test
    void surefireReceivesBackendProperty() {
        assertEquals(System.getProperty("gecko.backend.expected", "auto"),
                System.getProperty("gecko.backend", "auto"));
    }

    @Test
    void circuitFilterSelectsExactlyTheRequestedNames() throws Exception {
        String previous = System.getProperty("gecko.regression.circuits");
        try {
            System.setProperty("gecko.regression.circuits", "cuk_simple,buck_simple");
            assertEquals(List.of("cuk_simple", "buck_simple"), RegressionTest.circuitFiles().toList());
        } finally { restore("gecko.regression.circuits", previous); }
    }

    @Test
    void circuitFilterRejectsUnknownAndEmptyNames() {
        String previous = System.getProperty("gecko.regression.circuits");
        try {
            System.setProperty("gecko.regression.circuits", "unknown");
            assertThrows(IllegalArgumentException.class, RegressionTest::circuitFiles);
            System.setProperty("gecko.regression.circuits", "");
            assertThrows(IllegalArgumentException.class, RegressionTest::circuitFiles);
        } finally { restore("gecko.regression.circuits", previous); }
    }

    @Test
    @Tag("regression")
    void synchronousCoreStartUsesTheSessionAndRejectsUnsupportedCircuits() throws Exception {
        String previous = System.getProperty("gecko.backend");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        try {
            System.setProperty("gecko.backend", "core");
            GeckoExternal.openFile(java.nio.file.Path.of("src/test/resources/ipes/education/buck_simple.ipes")
                    .toAbsolutePath().toString());
            GeckoExternal.runSimulation();
            assertNull(GeckoSim._win._simRunner.simKern, "Core starts must not create a legacy numerical kernel");
            assertTrue(GeckoExternal.getSimulationTime() > 0);
            assertEquals(ContainerStatus.PAUSED, NetzlisteCONTROL.globalData.getContainerStatus());
            GeckoExternal.openFile(java.nio.file.Path.of(
                    "src/modules/gecko-simulation-core/src/test/resources/ipes/coupled_rectifier.ipes")
                    .toAbsolutePath().toString());
            RuntimeException failure = assertThrows(RuntimeException.class, GeckoExternal::runSimulation);
            assertTrue(failure.toString().contains("UNSUPPORTED_CONTROL_COMPONENT"), failure::toString);
        } finally {
            restore("gecko.backend", previous);
            SwingUtilities.invokeAndWait(() -> {
                for (var window : java.awt.Window.getWindows()) window.dispose();
            });
        }
    }

    @Test
    @Tag("regression")
    void editorCaptureCharacterizesDiagnosticsSeparatelyFromTheDirectParser() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        try {
            var preparation = new CircuitPreparation();
            int rejected = 0;
            for (String circuit : SignalOrderProbeTest.circuitFiles().toList()) {
                Path file = circuitPath(circuit);
                var parsed = preparation.prepare(preparation.capture(new CircuitFileParser().parse(file.toString())),
                        PreparationRequest.defaults());
                GeckoExternal.openFile(file.toString());
                var scripts = MainWindow._scripter;
                var snapshot = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                        MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                        new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                                scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), file.toString());
                var captured = preparation.prepare(snapshot, PreparationRequest.defaults());
                Set<String> actual = codes(captured);
                System.out.println("CAPTURE: " + circuit + " | " + actual + " | PARSER: " + codes(parsed));
                if (CAPTURE_FALLBACKS.containsKey(circuit)) {
                    rejected++;
                    assertFalse(captured instanceof PreparationResult.Ready, circuit);
                    assertEquals(CAPTURE_FALLBACKS.get(circuit), actual, circuit);
                    assertEquals(actual, codes(parsed), circuit);
                } else {
                    assertInstanceOf(PreparationResult.Ready.class, captured, circuit);
                    assertInstanceOf(PreparationResult.Ready.class, parsed, circuit);
                }
            }
            assertEquals(CAPTURE_FALLBACKS.size(), rejected);
        } finally { closeWindows(); }
    }

    @Test
    @Tag("regression")
    void anInterruptedCoreRunDoesNotPublishSuccessfulCompletion() throws Exception {
        String previous = System.getProperty("gecko.backend");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        var successfulCompletions = new ArrayList<ContainerStatus>();
        java.util.Observer saverTrigger = (source, change) -> {
            ContainerStatus status = NetzlisteCONTROL.globalData.getContainerStatus();
            if (status == ContainerStatus.PAUSED) successfulCompletions.add(status);
        };
        try {
            System.setProperty("gecko.backend", "core");
            GeckoExternal.openFile(circuitPath("buck_simple").toString());
            MainWindow._solverSettings._tDURATION.setValueWithoutUndo(0.1);
            NetzlisteCONTROL.globalData.addObserver(saverTrigger);
            Thread.currentThread().interrupt();
            try {
                assertThrows(InterruptedException.class,
                        () -> GeckoSim._win._simRunner.startCalculation(false, MainWindow._solverSettings));
            } finally { Thread.interrupted(); }
            assertTrue(successfulCompletions.isEmpty(), "A failed run must not emit the DataSaver PAUSED trigger");
            assertEquals(ContainerStatus.DELETED, NetzlisteCONTROL.globalData.getContainerStatus());
            assertFalse(GeckoSim._win.isSimulationRunning(), "Failure must restore the start action");
            var continueItems = new ArrayList<javax.swing.JMenuItem>();
            collectContinueItems(GeckoSim._win.getJMenuBar(), continueItems);
            assertFalse(continueItems.isEmpty());
            assertTrue(continueItems.stream().noneMatch(javax.swing.JMenuItem::isEnabled),
                    "A failed run must not enable endReached actions");
        } finally {
            NetzlisteCONTROL.globalData.deleteObserver(saverTrigger);
            restore("gecko.backend", previous);
            closeWindows();
        }
    }

    @Test
    @Tag("regression")
    void defaultAutoLogsExactlyTheVerifiedEditorCaptureFallbacks() throws Exception {
        String previous = System.getProperty("gecko.backend");
        var logger = java.util.logging.Logger.getLogger("gecko.backend.fallback");
        var previousLevel = logger.getLevel();
        var messages = new ArrayList<String>();
        var handler = new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord record) {
                assertEquals(java.util.logging.Level.INFO, record.getLevel());
                messages.add(record.getMessage());
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        logger.addHandler(handler);
        logger.setLevel(java.util.logging.Level.INFO);
        try {
            System.clearProperty("gecko.backend");
            for (String circuit : SignalOrderProbeTest.circuitFiles().toList()) {
                int before = messages.size();
                GeckoExternal.openFile(circuitPath(circuit).toString());
                GeckoExternal.runSimulation();
                if (CAPTURE_FALLBACKS.containsKey(circuit)) {
                    assertEquals(before + 1, messages.size(), circuit);
                    String message = messages.get(before);
                    assertTrue(message.startsWith(circuit + ": "), message);
                    assertEquals(CAPTURE_FALLBACKS.get(circuit),
                            Set.of(message.substring(circuit.length() + 2).split(",")), circuit);
                    assertNotNull(GeckoSim._win._simRunner.simKern, circuit);
                } else {
                    assertEquals(before, messages.size(), circuit);
                    assertNull(GeckoSim._win._simRunner.simKern, circuit);
                }
            }
            assertEquals(CAPTURE_FALLBACKS.size(), messages.size());
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            restore("gecko.backend", previous);
            closeWindows();
        }
    }

    private static Set<String> codes(PreparationResult result) {
        return result.report().diagnostics().stream().map(diagnostic -> diagnostic.code()).collect(Collectors.toSet());
    }

    private static Path circuitPath(String circuit) {
        Path path = Path.of("src/test/resources/ipes/education", circuit + ".ipes").toAbsolutePath();
        assertTrue(java.nio.file.Files.isRegularFile(path), path::toString);
        return path;
    }

    private static void collectContinueItems(javax.swing.MenuElement parent, List<javax.swing.JMenuItem> items) {
        if (parent instanceof javax.swing.JMenuItem item && "Continue".equals(item.getActionCommand())) items.add(item);
        for (var child : parent.getSubElements()) collectContinueItems(child, items);
    }

    private static void closeWindows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var window : java.awt.Window.getWindows()) window.dispose();
        });
    }

    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}

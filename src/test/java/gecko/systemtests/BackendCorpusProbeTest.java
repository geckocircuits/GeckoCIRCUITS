package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.simulation.SessionHost;
import gecko.geckocircuits.allg.DesktopSimulationController;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.control.ReglerSaveData;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Inventories the desktop controller's route for every corpus circuit without starting a simulation. */
@Tag("regression")
public class BackendCorpusProbeTest {
    private static final Set<String> READY_EDUCATION = Set.of(
            "buck_simple", "boost_simple", "buckBoost_simple", "cuk_simple", "sepic_simple",
            "boostPFC", "boostPFC_currentControl", "2phaseDiodeBridge_AC-Inductor",
            "2phaseDiodeBridge_DC-Inductor", "thyristor_RL_single", "thyristor_RL_2phBridge",
            "thyristor_RL_3phBridge", "thyristor_freeWheelingDiode", "thyristor_interface_trafo",
            "thyristor_Jakopovic", "diode_RL_singlePH_trafo", "thyristor_RL_singlePh_trafo",
            "thyristor_RL_3ph_trafo", "thyristor_commutation_3ph_trafo", "thyristor_lossOfCommutation",
            "singlePhase_PWM_converter");
    private static final Map<String, Set<String>> FALLBACK_EDUCATION = Map.of();
    private static void startApplication() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.applicationProps = new Properties();
        try (InputStream defaults = GeckoSim.class.getResourceAsStream("/defaultProperties.prp")) {
            assertNotNull(defaults, "default properties must be available");
            GeckoSim.applicationProps.load(defaults);
        }
        GeckoSim.applicationProps.setProperty("UPDATE_FREQ", "NEVER");
        SwingUtilities.invokeAndWait(() -> GeckoSim.main(new String[0]));
        Thread.sleep(2000);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void inventoriesControllerRoutesForTheWholeCorpus() throws Exception {
        String classPath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        Process worker = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath, ProbeProcess.class.getName())
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true).start();
        var transcript = CompletableFuture.supplyAsync(() -> {
            try (InputStream output = worker.getInputStream()) {
                return output.readAllBytes();
            } catch (java.io.IOException failure) {
                throw new UncheckedIOException(failure);
            }
        });
        try {
            boolean completed = worker.waitFor(150, TimeUnit.SECONDS);
            if (!completed) worker.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            System.out.print(new String(transcript.get(5, TimeUnit.SECONDS), StandardCharsets.UTF_8));
            assertTrue(completed, "The isolated corpus probe timed out");
            assertEquals(0, worker.exitValue(), "The isolated corpus probe failed");
        } finally {
            if (worker.isAlive()) worker.destroyForcibly();
            worker.getInputStream().close();
        }
    }

    /** Owns all desktop windows and globals in an isolated JVM. */
    public static final class ProbeProcess {
        public static void main(String[] arguments) {
            try {
                Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
                    if (SwingUtilities.isEventDispatchThread()) {
                        new AssertionError("Unexpected event dispatch failure", failure).printStackTrace();
                        System.exit(1);
                    } else {
                        failure.printStackTrace();
                    }
                });
                startApplication();
                String summary = inventoryRoutes();
                SwingUtilities.invokeLater(() -> finishWhenIdle(summary));
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        }

        private static void finishWhenIdle(String summary) {
            assertNoModalDialog();
            // Let queued events and the follow-up events they post complete before accepting success.
            if (Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null) {
                // Give lower-priority paint events time to run before the next check.
                var completionCheck = new Timer(10, event -> finishWhenIdle(summary));
                completionCheck.setRepeats(false);
                completionCheck.start();
                return;
            }
            System.out.print(summary);
            System.exit(0);
        }
    }

    private static String inventoryRoutes() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        List<Path> files = corpusFiles(root);
        assertFalse(files.isEmpty(), "The corpus must contain circuit files");
        int core = 0, fallback = 0, captureErrors = 0, education = 0;
        System.out.println("CORPUS | Routes come from DesktopSimulationController.route plus the GUI start path's"
                + " presentation signal map check.");
        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            boolean isEducation = relative.startsWith("src/test/resources/ipes/education/");
            String name = file.getFileName().toString().replaceFirst("\\.ipes$", "");
            System.out.println("CORPUS_OPEN | " + relative);
            var capture = new FutureTask<>(() -> {
                boolean missingSignalFile = openCircuit(file);
                var route = DesktopSimulationController.route(MainWindow._se, MainWindow._solverSettings);
                if (missingSignalFile) {
                    assertFalse(route.core(), relative + " must not enter the core route without its signal file");
                }
                return route.core() ? signalMapGap(route) : route.fallback();
            });
            SwingUtilities.invokeAndWait(capture);
            // Process events queued by the open before reading the result or replacing the circuit.
            SwingUtilities.invokeAndWait(BackendCorpusProbeTest::assertNoModalDialog);
            try {
                String reason = capture.get();
                boolean routesToCore = reason == null;
                if (routesToCore) core++; else fallback++;
                System.out.println("CORPUS | " + relative + " | "
                        + (routesToCore ? "core | " : "fallback | " + reason));
                if (isEducation) {
                    education++;
                    if (READY_EDUCATION.contains(name)) {
                        assertTrue(routesToCore, relative + ": " + reason);
                    } else {
                        assertTrue(FALLBACK_EDUCATION.containsKey(name), "Unknown education circuit: " + name);
                        assertFalse(routesToCore, relative);
                        assertEquals(FALLBACK_EDUCATION.get(name), Set.of(reason.split(",")), relative);
                    }
                }
            } catch (ExecutionException captureFailure) {
                Throwable failure = captureFailure.getCause();
                if (failure instanceof Error error) throw error;
                captureErrors++;
                System.out.println("CORPUS | " + relative + " | unsupported capture | "
                        + failure.getClass().getSimpleName() + ": "
                        + String.valueOf(failure.getMessage()).replace('\n', ' ').replace('\r', ' '));
                if (isEducation) throw captureFailure;
            }
        }
        assertEquals(21, education, "All education circuits must be inventoried");
        return "CORPUS_SUMMARY | files=%d | core=%d | fallback=%d | unsupported capture=%d%n"
                .formatted(files.size(), core, fallback, captureErrors);
    }

    private static void assertNoModalDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Dialog dialog && dialog.isShowing() && dialog.isModal()) {
                fail("Unexpected modal dialog: " + dialog.getTitle());
            }
        }
    }

    /** Applies the GUI start path's presentation signal map check to a core route, then releases it. */
    private static String signalMapGap(DesktopSimulationController.Route route) throws Exception {
        try (var host = SessionHost.open(route.ready().execution(), ExecutionOptions.defaults())) {
            var netlist = NetListContainer.fabricStartSimulation(MainWindow._se, null);
            try {
                String missing = DesktopSimulationController.unmappedSignal(
                        DesktopSimulationController.coreRows(host, false),
                        new int[NetzlisteCONTROL.globalData.getRowLength()]);
                return missing == null ? null : "SIGNAL_MAP:" + missing;
            } finally {
                for (var block : netlist._nlControl._orderedControlBlocks) {
                    if (block instanceof ReglerSaveData exporter) exporter.cancelSimulationExport();
                }
            }
        }
    }

    private static List<Path> corpusFiles(Path root) throws Exception {
        var roots = new ArrayList<Path>();
        roots.add(root.resolve("src/test/resources"));
        roots.add(root.resolve("resources/tutorials"));
        try (var modules = Files.list(root.resolve("src/modules"))) {
            modules.filter(Files::isDirectory).map(module -> module.resolve("src/test/resources"))
                    .filter(Files::isDirectory).forEach(roots::add);
        }
        var files = new TreeSet<Path>();
        for (Path directory : roots) {
            assertTrue(Files.isDirectory(directory), "Missing corpus directory: " + directory);
            try (var paths = Files.walk(directory)) {
                paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".ipes"))
                        .filter(path -> !path.toString().contains("/target/"))
                        .map(Path::normalize).forEach(files::add);
            }
        }
        return List.copyOf(files);
    }

    private static boolean openCircuit(Path file) throws Exception {
        boolean[] missingSignalFile = {false};
        Timer notices = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog dialog) || !dialog.isShowing()) continue;
                // A closed dialog can remain in Window.getWindows after its root pane is removed.
                var rootPane = dialog.getRootPane();
                if (rootPane == null) continue;
                if (findOptionPane(rootPane.getContentPane()) instanceof JOptionPane pane) {
                    String message = String.valueOf(pane.getMessage());
                    String title = dialog.getTitle();
                    // Some archived tutorials refer to waveform files that are absent from the corpus.
                    boolean missingExternalFileNotice = "ERROR: File not found".equals(title)
                            && message.startsWith("Error: Specified external file with relative path: ")
                            && message.contains("\n and absolute path:") && message.endsWith(" NOT FOUND!");
                    boolean missingSignalNotice = title != null && title.endsWith(": ERROR - File not found!")
                            && message.startsWith("External data file missing in signal source ")
                            && message.endsWith("Desired file not found!");
                    if (("Info".equals(title) && message.startsWith(
                            "This Model file was created with an older version of GeckoCIRCUITS."))
                            || ("Error!".equals(title) && message.startsWith(
                                    "There are circuit components located outside the selected sheet size "))
                            || message.equals("Cannot resize due to bad component location.")
                            || missingExternalFileNotice || missingSignalNotice) {
                        if (missingSignalNotice) missingSignalFile[0] = true;
                        System.out.println("CORPUS_NOTICE | " + file.getFileName() + " | "
                                + message.replace('\n', ' '));
                        pane.setValue(JOptionPane.OK_OPTION);
                    }
                }
            }
        });
        notices.start();
        try {
            GeckoExternal.openFile(file.toString());
        } finally {
            notices.stop();
        }
        return missingSignalFile[0];
    }

    private static Component findOptionPane(Container container) {
        if (container == null) return null;
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

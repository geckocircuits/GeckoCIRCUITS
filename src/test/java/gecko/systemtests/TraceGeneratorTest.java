/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.geckocircuits.circuit.SimulationTraceLogger;

import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to generate golden trace CSV files for the headless simulation engine.
 *
 * <p>Runs the proven SimulationsKern via GeckoSim in testing mode, with a
 * {@link SimulationTraceLogger} attached. The per-step trace is written to
 * a gzip-compressed CSV file for use as golden reference in gecko-simulation-core
 * integration tests.
 *
 * <p>Requires a display (Swing). On headless CI, run with Xvfb:
 * {@code xvfb-run mvn test -Dtest=TraceGeneratorTest -Dgroups=trace-gen -Dgecko.generateTraces=true}
 *
 * <p>Output files go to {@code target directories with a traces- prefix}.
 *
 * @author Golden reference trace generation
 */
@Tag("trace-gen")
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "gecko.generateTraces", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TraceGeneratorTest {

    private static final String IPES_RESOURCE_DIR = "/ipes/education/";
    private static final int SIM_TIMEOUT_MS = 120_000;
    private static final int POLL_INTERVAL_MS = 200;

    private Path tracesDir;
    private Path tempDir;

    @BeforeAll
    void initGeckoSim() throws Exception {
        // Create output directory
        Files.createDirectories(Paths.get("target"));
        tracesDir = Files.createTempDirectory(Paths.get("target"), "traces-");

        tempDir = Files.createTempDirectory("gecko-trace-gen-");

        // Auto-dismiss any JOptionPane dialogs that appear during init.
        // The syntaxpane library shows a blocking error dialog when its
        // ScriptAction fails to load (Nashorn removed in Java 15+).
        AtomicBoolean stopDismisser = new AtomicBoolean(false);
        Thread dismisser = new Thread(() -> {
            while (!stopDismisser.get()) {
                try {
                    Thread.sleep(500);
                    SwingUtilities.invokeAndWait(() -> {
                        for (Window w : Window.getWindows()) {
                            if (w instanceof JDialog) {
                                JDialog d = (JDialog) w;
                                if (d.isVisible()) {
                                    System.out.println("[TraceGen] Auto-dismissing dialog: " + d.getTitle());
                                    d.dispose();
                                }
                            }
                        }
                    });
                } catch (Exception ignored) {}
            }
        }, "dialog-dismisser");
        dismisser.setDaemon(true);
        dismisser.start();

        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[]{});
        Thread.sleep(2000);

        stopDismisser.set(true);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void generateBuckSimpleTrace() throws Exception {
        generateTrace("buck_simple");
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void generateBoostSimpleTrace() throws Exception {
        generateTrace("boost_simple");
    }

    private void generateTrace(String circuitName) throws Exception {
        // Extract .ipes file from classpath
        Path ipesFile = extractResource(IPES_RESOURCE_DIR + circuitName + ".ipes",
                tempDir.resolve(circuitName + ".ipes"));

        assertTrue(Files.exists(ipesFile), "Could not extract " + circuitName + ".ipes");

        // Read simulation end time from golden CSV (if available) or use default
        double tEnd = readTEndFromGolden(circuitName);

        // Create trace logger
        File traceFile = tracesDir.resolve(circuitName + "_trace.csv.gz").toFile();
        try (SimulationTraceLogger traceLogger = new SimulationTraceLogger(traceFile)) {
            GeckoExternal.openFile(ipesFile.toAbsolutePath().toString());
            Thread.sleep(500);
            assertNotNull(GeckoSim._win, "GeckoSim._win not available");
            GeckoSim._win._simRunner.setPendingTraceLogger(traceLogger);
            try {
                GeckoExternal.runSimulation();
                long startWait = System.currentTimeMillis();
                boolean completed = false;
                while (System.currentTimeMillis() - startWait < SIM_TIMEOUT_MS) {
                    Thread.sleep(POLL_INTERVAL_MS);
                    try {
                        if (GeckoExternal.getSimulationTime() >= tEnd * 0.99) {
                            completed = true;
                            break;
                        }
                    } catch (Exception ignored) {
                        // The simulation may still be initializing.
                    }
                }
                assertTrue(completed,
                        "Simulation of " + circuitName + " did not complete within timeout");
                Thread.sleep(500);
            } finally {
                GeckoSim._win._simRunner.setPendingTraceLogger(null);
                try {
                    GeckoSim._win._simRunner.pauseSimulation();
                } finally {
                    if (GeckoSim._win._simRunner.simKern != null) {
                        GeckoSim._win._simRunner.simKern.setTraceLogger(null);
                    }
                }
            }
        }

        // Verify trace file was generated with data
        assertTrue(traceFile.exists(), "Trace file not created: " + traceFile);
        assertTrue(traceFile.length() > 100,
                "Trace file too small (" + traceFile.length() + " bytes), likely empty");

        System.out.println("Generated trace: " + traceFile.getAbsolutePath()
                + " (" + traceFile.length() + " bytes)");
    }

    private double readTEndFromGolden(String circuitName) {
        String goldenPath = "/golden/" + circuitName + ".golden.csv";
        try {
            double[] params = GoldenReferenceHelper.readSimulationParams(
                    extractResource(goldenPath, tempDir.resolve(circuitName + ".golden.csv")));
            if (params != null) {
                return params[0]; // tEnd
            }
        } catch (Exception e) {
            // Golden CSV not found, use default
        }
        return 0.001; // Default 1ms simulation
    }

    private Path extractResource(String resourcePath, Path targetFile) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            Files.createDirectories(targetFile.getParent());
            Files.copy(is, targetFile);
        }
        return targetFile;
    }
}

/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  terms of the GNU General Public License as published by the Free Software
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
import gecko.systemtests.GoldenReferenceHelper.GoldenEntry;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests that run each .ipes example circuit and compare steady-state
 * results against v2.02 golden references (1% relative tolerance).
 *
 * <p>These tests are tagged with "regression" and excluded from the default
 * Maven test run. Run with: {@code mvn test -Pregression}
 *
 * <p>Each test loads a circuit, runs the full simulation, extracts SCOPE signal
 * characteristics over the steady-state window (last 20%), and compares against
 * golden CSV references generated from the v2.02 release.
 *
 * <p>These tests require a display (Xvfb in CI) and are expected to:
 * <ul>
 *   <li>PASS on main (v2.02 baseline)</li>
 *   <li>FAIL on dev (proving they catch bugs #47/#48)</li>
 * </ul>
 */
@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class RegressionTest {

    private static final String IPES_RESOURCE_DIR = "/ipes/education/";
    private static final String GOLDEN_RESOURCE_DIR = "/golden/";
    private static final double RELATIVE_TOLERANCE = 0.01; // 1%
    private static final double ABSOLUTE_FLOOR = 1e-10;
    private static final int SIM_TIMEOUT_MS = 120_000; // 2 minutes per circuit
    private static final int POLL_INTERVAL_MS = 200;

    private Path tempDir;

    @BeforeAll
    void initGeckoSim() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[]{});
        // Wait for full initialization
        Thread.sleep(2000);
    }

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("gecko-regression-");
    }

    @AfterEach
    void tearDown() {
        // Clean up temp files
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
            } catch (IOException ignored) {}
        }
    }

    /**
     * Provides test arguments: one per .ipes file that has a matching golden CSV.
     */
    static Stream<String> circuitFiles() throws IOException {
        List<String> circuits = new ArrayList<>();
        // Known circuit files (matching files in src/test/resources/ipes/education/)
        String[] knownCircuits = {
            "buck_simple",
            "boost_simple",
            "buckBoost_simple",
            "cuk_simple",
            "sepic_simple",
            "boostPFC",
            "boostPFC_currentControl",
            "singlePhase_PWM_converter",
            "diode_RL_singlePH_trafo",
            "2phaseDiodeBridge_AC-Inductor",
            "2phaseDiodeBridge_DC-Inductor",
            "thyristor_RL_single",
            "thyristor_RL_singlePh_trafo",
            "thyristor_RL_2phBridge",
            "thyristor_RL_3phBridge",
            "thyristor_RL_3ph_trafo",
            "thyristor_freeWheelingDiode",
            "thyristor_interface_trafo",
            "thyristor_commutation_3ph_trafo",
            "thyristor_lossOfCommutation",
            "thyristor_Jakopovic"
        };

        for (String circuit : knownCircuits) {
            // Only include circuits that have both .ipes and .golden.csv
            if (RegressionTest.class.getResource(IPES_RESOURCE_DIR + circuit + ".ipes") != null
                    && RegressionTest.class.getResource(GOLDEN_RESOURCE_DIR + circuit + ".golden.csv") != null) {
                circuits.add(circuit);
            }
        }

        if (circuits.isEmpty()) {
            fail("No regression test circuits found. Ensure .ipes and .golden.csv files are in test resources.");
        }

        return circuits.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("circuitFiles")
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void regressionTest(String circuitName) throws Exception {
        // Extract .ipes file from classpath to temp directory (GeckoExternal needs filesystem path)
        Path ipesFile = extractResource(IPES_RESOURCE_DIR + circuitName + ".ipes",
                tempDir.resolve(circuitName + ".ipes"));

        // Load golden reference
        Path goldenCsv = extractResource(GOLDEN_RESOURCE_DIR + circuitName + ".golden.csv",
                tempDir.resolve(circuitName + ".golden.csv"));
        List<GoldenEntry> goldenEntries = GoldenReferenceHelper.readGoldenCsv(goldenCsv);

        assertFalse(goldenEntries.isEmpty(),
                "Golden reference for " + circuitName + " has no entries");

        // Read simulation parameters from golden CSV
        double[] simParams = GoldenReferenceHelper.readSimulationParams(goldenCsv);
        assertNotNull(simParams, "Golden CSV missing tEnd/dt metadata for " + circuitName);
        double tEnd = simParams[0];

        // Open and run simulation
        GeckoExternal.openFile(ipesFile.toAbsolutePath().toString());
        Thread.sleep(500);

        GeckoExternal.runSimulation();

        // Wait for simulation to complete with timeout
        long startWait = System.currentTimeMillis();
        boolean completed = false;
        while (System.currentTimeMillis() - startWait < SIM_TIMEOUT_MS) {
            Thread.sleep(POLL_INTERVAL_MS);
            try {
                double simTime = GeckoExternal.getSimulationTime();
                if (simTime >= tEnd * 0.99) {
                    completed = true;
                    break;
                }
            } catch (Exception e) {
                // Simulation may still be initializing
            }
        }

        assertTrue(completed,
                "Simulation of " + circuitName + " did not complete within " + (SIM_TIMEOUT_MS / 1000) + "s (bug #48?)");

        // Wait for results to settle
        Thread.sleep(500);

        // Steady-state window: last 20%
        double startTime = tEnd * 0.8;
        double endTime = tEnd;

        // Compare each golden entry against current results
        List<String> mismatches = new ArrayList<>();
        int compared = 0;

        for (GoldenEntry golden : goldenEntries) {
            try {
                double[] actual = GeckoExternal.getSignalCharacteristics(
                        golden.scopeName, golden.port, startTime, endTime);

                assertNotNull(actual,
                        "getSignalCharacteristics returned null for " + golden.scopeName + " port " + golden.port + " (bug #47?)");
                assertTrue(actual.length > 0,
                        "getSignalCharacteristics returned empty for " + golden.scopeName + " port " + golden.port + " (bug #47?)");

                // Compare each characteristic
                int numChars = Math.min(golden.characteristics.length, actual.length);
                numChars = Math.min(numChars, GoldenEntry.CHARACTERISTIC_NAMES.length);

                for (int i = 0; i < numChars; i++) {
                    double expected = golden.characteristics[i];
                    double actualVal = actual[i];

                    if (!GoldenReferenceHelper.withinTolerance(expected, actualVal, RELATIVE_TOLERANCE, ABSOLUTE_FLOOR)) {
                        mismatches.add(GoldenReferenceHelper.formatMismatch(
                                golden.scopeName, golden.port,
                                GoldenEntry.CHARACTERISTIC_NAMES[i],
                                expected, actualVal, RELATIVE_TOLERANCE));
                    }
                    compared++;
                }
            } catch (Exception e) {
                mismatches.add(golden.scopeName + " port " + golden.port + ": exception: " + e.getMessage());
            }
        }

        if (!mismatches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Regression failures for ").append(circuitName)
                    .append(" (").append(mismatches.size()).append(" of ").append(compared).append(" comparisons failed):\n");
            for (String m : mismatches) {
                sb.append("  - ").append(m).append("\n");
            }
            fail(sb.toString());
        }

        System.out.println("PASS: " + circuitName + " (" + compared + " values compared, " + goldenEntries.size() + " scope ports)");
    }

    /**
     * Extracts a classpath resource to a filesystem path.
     */
    private Path extractResource(String resourcePath, Path target) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}

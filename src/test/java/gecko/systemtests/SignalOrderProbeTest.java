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
import gecko.geckocircuits.allg.MainWindow;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.Diagnostic;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.control.NetzlisteCONTROL;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reports desktop and core signal names without requiring equal names or row order. */
@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class SignalOrderProbeTest {

    private static final String IPES_RESOURCE_DIR = "/ipes/education/";
    private static final String GOLDEN_RESOURCE_DIR = "/golden/";
    private static final int SIM_TIMEOUT_MS = 120_000;
    private static final int POLL_INTERVAL_MS = 200;

    private Path tempDir;
    private String previousBackend;
    private double previousDuration;
    private double previousDt;

    @BeforeAll
    void initGeckoSim() throws Exception {
        previousBackend = System.getProperty("gecko.backend");
        System.setProperty("gecko.backend", "legacy");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[]{});
        // Wait for full initialization
        Thread.sleep(2000);
        previousDuration = MainWindow._solverSettings._tDURATION.getValue();
        previousDt = MainWindow._solverSettings.dt.getValue();
    }

    @AfterAll
    void restoreBackend() {
        if (previousBackend == null) System.clearProperty("gecko.backend");
        else System.setProperty("gecko.backend", previousBackend);
        MainWindow._solverSettings._tDURATION.setValueWithoutUndo(previousDuration);
        MainWindow._solverSettings.dt.setValueWithoutUndo(previousDt);
    }

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory(Path.of("target"), "gecko-signal-order-");
    }

    static Stream<String> circuitFiles() {
        return Stream.of(
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
                "thyristor_Jakopovic");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("circuitFiles")
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void compareSignalOrder(String circuitName) throws Exception {
        Path ipesFile = extractResource(IPES_RESOURCE_DIR + circuitName + ".ipes",
                tempDir.resolve(circuitName + ".ipes"));
        Path goldenCsv = extractResource(GOLDEN_RESOURCE_DIR + circuitName + ".golden.csv",
                tempDir.resolve(circuitName + ".golden.csv"));
        double[] simParams = Objects.requireNonNull(GoldenReferenceHelper.readSimulationParams(goldenCsv),
                "Golden CSV missing tEnd/dt metadata for " + circuitName);
        double dt = simParams[1];
        assertTrue(dt > 0, "Golden CSV must include a positive dt for " + circuitName);
        // Run two steps because this probe checks signal names, not waveform characteristics.
        double tEnd = Math.min(simParams[0], 2 * dt);

        GeckoExternal.openFile(ipesFile.toAbsolutePath().toString());
        Thread.sleep(500);
        MainWindow._solverSettings._tDURATION.setValueWithoutUndo(tEnd);
        MainWindow._solverSettings.dt.setValueWithoutUndo(dt);

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
        if (!completed) {
            throw new IllegalStateException("Simulation of " + circuitName
                    + " did not complete within " + (SIM_TIMEOUT_MS / 1000) + "s");
        }

        // Wait for results to settle
        Thread.sleep(500);

        List<String> desktopNames = new ArrayList<>();
        for (int row = 0; row < NetzlisteCONTROL.globalData.getRowLength(); row++) {
            desktopNames.add(NetzlisteCONTROL.globalData.getSignalName(row));
        }

        var model = new CircuitFileParser().parse(ipesFile.toAbsolutePath().toString());
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        List<String> coreNames = List.of();
        String comparison;
        if (result instanceof PreparationResult.Ready ready) {
            try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
                coreNames = Arrays.asList(session.signalNames());
            }
            comparison = desktopNames.equals(coreNames) ? "EQUAL"
                    : new HashSet<>(desktopNames).equals(new HashSet<>(coreNames))
                            ? "SAME_SET_OTHER_ORDER" : "DIFFERENT_SET";
        } else {
            comparison = "core not Ready (" + result.report().diagnostics().stream()
                    .map(Diagnostic::code).distinct().collect(Collectors.joining(",")) + ")";
        }
        System.out.println(circuitName + " | " + comparison
                + ("EQUAL".equals(comparison) ? "" : " | desktop=" + desktopNames + " | core=" + coreNames));

        assertFalse(desktopNames.isEmpty(), "Desktop signal names are empty for " + circuitName);
        if (result instanceof PreparationResult.Ready) {
            assertFalse(coreNames.isEmpty(), "Core signal names are empty for " + circuitName);
        }
    }

    private Path extractResource(String resourcePath, Path target) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}

/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.geckocircuits.allg.MainWindow;
import gecko.systemtests.GoldenReferenceHelper.GoldenEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class ScriptSteppingParityTest {
    private static final double RELATIVE_TOLERANCE = 0.01;
    private static final double ABSOLUTE_FLOOR = 1e-10;
    private static Properties previousProperties;
    private static boolean previousTestingMode;
    private static Path tempDir;

    @BeforeAll
    static void startApplication() throws Exception {
        previousProperties = GeckoSim.applicationProps;
        previousTestingMode = GeckoSim._isTestingMode;
        GeckoSim._isTestingMode = true;
        GeckoSim.applicationProps = new Properties();
        try (InputStream defaults = GeckoSim.class.getResourceAsStream("/defaultProperties.prp")) {
            assertNotNull(defaults, "default properties must be available");
            GeckoSim.applicationProps.load(defaults);
        }
        GeckoSim.applicationProps.setProperty("UPDATE_FREQ", "NEVER");
        GeckoSim.main(new String[]{});
        Thread.sleep(2000);
        tempDir = Files.createTempDirectory("gecko-script-stepping-");
    }

    @AfterAll
    static void stopApplication() {
        try {
            if (MainWindow._scripter != null) MainWindow._scripter.endSimulation();
        } catch (RuntimeException ignored) { }
        GeckoSim._isTestingMode = previousTestingMode;
        GeckoSim.applicationProps = previousProperties;
        if (tempDir != null) {
            try (var paths = Files.walk(tempDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            } catch (IOException ignored) { }
        }
    }

    @Test
    void scriptSteppingMatchesBetweenLegacyAndCore() throws Exception {
        Path circuit = extract("/ipes/education/buck_simple.ipes", "buck_simple.ipes");
        Path goldenPath = extract("/golden/buck_simple.golden.csv", "buck_simple.golden.csv");
        List<GoldenEntry> goldens = GoldenReferenceHelper.readGoldenCsv(goldenPath);
        assertEquals(8, goldens.size(), "buck_simple fixture contains six live ports and two zero placeholders");
        for (GoldenEntry placeholder : goldens.subList(6, 8)) {
            assertTrue(Arrays.stream(placeholder.characteristics).allMatch(value -> value == 0.0),
                    "placeholder port " + placeholder.port + " must remain an all-zero row");
        }
        double[] parameters = GoldenReferenceHelper.readSimulationParams(goldenPath);
        assertNotNull(parameters);
        double end = parameters[0];
        double dt = parameters[1];
        String previousBackend = System.getProperty("gecko.backend");
        List<double[]> legacy = null;
        List<double[]> core = null;
        List<Double> checkpointTimes = new ArrayList<>();
        try {
            for (String backend : List.of("legacy", "core")) {
                System.setProperty("gecko.backend", backend);
                GeckoExternal.openFile(circuit.toAbsolutePath().toString());
                MainWindow._scripter.initializeSimulation(dt, end);
                if (backend.equals("core")) {
                    assertNotNull(GeckoSim._win._simRunner.getCoreStatus(), "core backend must own a core session");
                } else {
                    assertNull(GeckoSim._win._simRunner.getCoreStatus(), "legacy backend must not own a core session");
                }
                assertEquals(0.0, MainWindow._scripter.getSimulationTime(), dt / 1000.0,
                        "initialize must not advance simulation");
                AtomicReference<Throwable> edtFailure = new AtomicReference<>();
                long edtCheckStarted = System.nanoTime();
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        assertThrows(IllegalStateException.class,
                                () -> MainWindow._scripter.initializeSimulation(dt, end));
                        assertThrows(IllegalStateException.class, () -> MainWindow._scripter.simulateOneStep());
                        assertThrows(IllegalStateException.class, () -> MainWindow._scripter.simulateSpecifiedTime(dt));
                    } catch (Throwable error) {
                        edtFailure.set(error);
                    }
                });
                assertNull(edtFailure.get(), "EDT stepping calls must fail fast");
                assertTrue(System.nanoTime() - edtCheckStarted < 1_000_000_000L,
                        "EDT initialization and stepping calls must fail without waiting");
                for (int i = 0; i < 50; i++) MainWindow._scripter.simulateOneStep();
                double afterSteps = MainWindow._scripter.getSimulationTime();
                MainWindow._scripter.simulateSpecifiedTime(Double.NaN);
                assertEquals(afterSteps, MainWindow._scripter.getSimulationTime(), dt / 1000.0,
                        "NaN relative time must not advance the script cursor");
                MainWindow._scripter.simulateSpecifiedTime(0.2 * end);
                double afterRelativeTime = MainWindow._scripter.getSimulationTime();
                double relativeTarget = afterSteps + 0.2 * end;
                assertTrue(afterRelativeTime + dt / 1000.0 >= relativeTarget
                                && afterRelativeTime <= relativeTarget + dt + dt / 1000.0,
                        "relative stepping must stop at the first grid point after its target");
                checkpointTimes.add(afterSteps);
                checkpointTimes.add(afterRelativeTime);
                MainWindow._scripter.simulateSpecifiedTime(end - MainWindow._scripter.getSimulationTime());
                assertEquals(end, MainWindow._scripter.getSimulationTime(), dt / 1000.0);
                List<double[]> actual = new ArrayList<>();
                for (GoldenEntry entry : goldens.subList(0, 6)) {
                    actual.add(MainWindow._scripter.getSignalCharacteristics(entry.scopeName, entry.port,
                            end * 0.8, end));
                }
                for (GoldenEntry placeholder : goldens.subList(6, 8)) {
                    assertThrows(Exception.class, () -> MainWindow._scripter.getSignalCharacteristics(
                            placeholder.scopeName, placeholder.port, end * 0.8, end),
                            backend + " must report zero placeholder port " + placeholder.port + " unsupported");
                }
                if (backend.equals("legacy")) legacy = actual;
                else core = actual;
                double finalCursor = MainWindow._scripter.getSimulationTime();
                MainWindow._scripter.endSimulation();
                assertEquals(finalCursor, MainWindow._scripter.getSimulationTime(), dt / 1000.0,
                        "endSimulation must retain the final script cursor");

                GeckoExternal.openFile(circuit.toAbsolutePath().toString());
                MainWindow._scripter.initializeSimulation(dt, end);
                Exception overrun = assertThrows(Exception.class,
                        () -> MainWindow._scripter.simulateSpecifiedTime(2 * end));
                assertEquals("Specified simulation time goes beyond specified simulated end time; simulated only up to end time",
                        overrun.getMessage());
                double clampedCursor = MainWindow._scripter.getSimulationTime();
                assertTrue(clampedCursor >= end - dt / 1000.0 && clampedCursor <= end + dt + dt / 1000.0,
                        "overrun must advance to the end before reporting the legacy error");
                MainWindow._scripter.endSimulation();
            }
        } finally {
            if (previousBackend == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", previousBackend);
            MainWindow._scripter.endSimulation();
        }
        assertNotNull(legacy);
        assertNotNull(core);
        assertEquals(4, checkpointTimes.size());
        assertEquals(checkpointTimes.get(0), checkpointTimes.get(2), dt / 1000.0,
                "legacy/core parity after 50 steps");
        assertEquals(checkpointTimes.get(1), checkpointTimes.get(3), dt / 1000.0,
                "legacy/core parity after relative simulate time");
        assertEquals(6, legacy.size(), "all six live golden ports must be compared");
        assertEquals(6, core.size(), "all six live golden ports must be compared");
        for (int entry = 0; entry < 6; entry++) {
            double[] expected = legacy.get(entry);
            double[] actual = core.get(entry);
            assertEquals(expected.length, actual.length, "characteristic count for " + goldens.get(entry).scopeName);
            for (int i = 0; i < expected.length; i++) {
                assertTrue(GoldenReferenceHelper.withinTolerance(expected[i], actual[i], RELATIVE_TOLERANCE,
                        ABSOLUTE_FLOOR), "backend parity for " + goldens.get(entry).scopeName + " port "
                        + goldens.get(entry).port + " characteristic " + GoldenEntry.CHARACTERISTIC_NAMES[i]);
            }
        }
    }

    private static Path extract(String resource, String filename) throws IOException {
        Path target = tempDir.resolve(filename);
        try (InputStream input = ScriptSteppingParityTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, "missing resource " + resource);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}

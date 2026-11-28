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
package gecko.core.simulation;

import gecko.core.datacontainer.DataContainerGlobal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end integration test for the HeadlessSimulationEngine with buck_simple.ipes.
 *
 * <p>Validates that the control block chain fix (Bug #47) produces correct scope signals:
 * <ul>
 *   <li>Scope data is NOT all zeros</li>
 *   <li>Output voltage Vo (u2) ≈ 19.2V (48V × 0.4 duty)</li>
 *   <li>Inductor current IL (iL) > 0</li>
 * </ul>
 */
class HeadlessSimulationIntegrationTest {

    private static File findBuckSimple() {
        // Try from module root (Maven runs tests from src/modules/gecko-simulation-core/)
        File f = new File("../../test/resources/ipes/education/buck_simple.ipes");
        if (f.exists()) return f;
        // Try from project root (IDE or manual runs)
        f = new File("src/test/resources/ipes/education/buck_simple.ipes");
        return f;
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void buckSimple_scopeDataNotAllZeros() {
        File f = findBuckSimple();
        assumeTrue(f.exists(), "buck_simple.ipes not found at " + f.getAbsolutePath());

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(f.getAbsolutePath())
                .build();

        SimulationResult result = engine.runSimulation(config);
        assertEquals(SimulationResult.Status.SUCCESS, result.getStatus(),
                "Simulation should succeed: " + result.getErrorMessage());

        DataContainerGlobal data = result.getDataContainer();
        assertNotNull(data, "Data container should not be null");

        int signalCount = data.getRowLength();
        assertTrue(signalCount > 0, "Should have at least one signal row");

        // Check that at least one signal has non-zero values
        boolean hasNonZero = false;
        for (int sig = 0; sig < signalCount && !hasNonZero; sig++) {
            int maxIdx = data.getMaximumTimeIndex(sig);
            for (int t = 0; t <= maxIdx; t++) {
                float val = data.getValue(sig, t);
                if (Math.abs(val) > 1e-6) {
                    hasNonZero = true;
                    break;
                }
            }
        }

        assertTrue(hasNonZero, "Scope data should contain non-zero values (Bug #47 fix)");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void buckSimple_steadyStateVoltage() {
        File f = findBuckSimple();
        assumeTrue(f.exists(), "buck_simple.ipes not found");

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(f.getAbsolutePath())
                .build();

        SimulationResult result = engine.runSimulation(config);
        assertEquals(SimulationResult.Status.SUCCESS, result.getStatus(),
                "Simulation should succeed");

        DataContainerGlobal data = result.getDataContainer();
        assertNotNull(data);

        // Find "u2" signal index (output voltage)
        int signalCount = data.getRowLength();
        int u2Index = -1;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < signalCount; i++) {
            String name = data.getSignalName(i);
            names.append(name).append(", ");
            if ("u2".equals(name)) {
                u2Index = i;
            }
        }
        assertTrue(u2Index >= 0, "Should find 'u2' signal in: [" + names + "]");

        // Average over last 20% of simulation for steady-state
        int maxIdx = data.getMaximumTimeIndex(u2Index);
        assertTrue(maxIdx > 0, "Should have data points");
        int startSlot = (int) (maxIdx * 0.8);
        double sum = 0;
        int count = 0;
        for (int t = startSlot; t <= maxIdx; t++) {
            sum += data.getValue(u2Index, t);
            count++;
        }
        double avgVo = sum / count;

        // Expected: 48V × 0.4 duty = 19.2V ± 5%
        double expectedVo = 48.0 * 0.4;
        double tolerance = expectedVo * 0.05;
        assertEquals(expectedVo, avgVo, tolerance,
                String.format("Steady-state Vo should be ~%.1fV (48V × 0.4 duty), got %.3fV", expectedVo, avgVo));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void buckSimple_positiveInductorCurrent() {
        File f = findBuckSimple();
        assumeTrue(f.exists(), "buck_simple.ipes not found");

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(f.getAbsolutePath())
                .build();

        SimulationResult result = engine.runSimulation(config);
        assertEquals(SimulationResult.Status.SUCCESS, result.getStatus());

        DataContainerGlobal data = result.getDataContainer();
        assertNotNull(data);

        // Find "iL" signal
        int signalCount = data.getRowLength();
        int iLIndex = -1;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < signalCount; i++) {
            String name = data.getSignalName(i);
            names.append(name).append(", ");
            if ("iL".equals(name)) {
                iLIndex = i;
            }
        }
        assertTrue(iLIndex >= 0, "Should find 'iL' signal in: [" + names + "]");

        // Average over last 20% for steady-state
        int maxIdx = data.getMaximumTimeIndex(iLIndex);
        int startSlot = (int) (maxIdx * 0.8);
        double sum = 0;
        int count = 0;
        for (int t = startSlot; t <= maxIdx; t++) {
            sum += data.getValue(iLIndex, t);
            count++;
        }
        double avgIL = sum / count;

        assertTrue(avgIL > 0, String.format("Average inductor current should be positive, got %.4fA", avgIL));
    }
}

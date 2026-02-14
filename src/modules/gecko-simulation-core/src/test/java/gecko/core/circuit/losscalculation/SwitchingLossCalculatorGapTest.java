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
package gecko.core.circuit.losscalculation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Gap coverage tests for SwitchingLossCalculator.
 * This test class targets uncovered branches and edge cases to bring coverage from 85% to 100%.
 *
 * Specifically targets:
 * - Line 225 (detectTurnOn): Missing branch where current stays below threshold
 * - Line 236 (detectTurnOff): Missing branch where current stays above threshold
 * - Lines 305-306 (getTurnOnToTurnOffRatio): Zero turn-off energy case
 */
public class SwitchingLossCalculatorGapTest {

    private static final double TOLERANCE = 1e-10;

    // Reference values for testing
    private static final double E_ON_REF = 1e-3;      // 1 mJ
    private static final double E_OFF_REF = 0.5e-3;   // 0.5 mJ
    private static final double I_REF = 10.0;          // 10 A
    private static final double V_REF = 600.0;         // 600 V

    // ===========================================
    // Turn-On Detection - Missing Branch Coverage
    // ===========================================

    /**
     * Tests detectTurnOn when both old and new currents are below threshold.
     * This covers the missing branch: oldCurrent < threshold AND newCurrent < threshold.
     */
    @Test
    public void testDetectTurnOn_BothBelowThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Both currents below threshold - no turn-on should be detected
        assertFalse(calc.detectTurnOn(0.001, 0.005));  // 1mA -> 5mA, both < 10mA threshold
    }

    /**
     * Tests detectTurnOn at exactly the threshold boundary.
     * Verifies that current must cross the threshold, not just reach it.
     */
    @Test
    public void testDetectTurnOn_OldAtThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Old current exactly at threshold, new current above - should NOT detect turn-on
        // because oldCurrent is not < threshold
        assertFalse(calc.detectTurnOn(SwitchingLossCalculator.CURRENT_THRESHOLD, 1.0));
    }

    /**
     * Tests detectTurnOn with new current exactly at threshold boundary.
     * Current at threshold should be considered "on" (>= threshold).
     */
    @Test
    public void testDetectTurnOn_NewAtThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Old current below threshold, new current exactly at threshold - should detect turn-on
        assertTrue(calc.detectTurnOn(0.001, SwitchingLossCalculator.CURRENT_THRESHOLD));
    }

    // ===========================================
    // Turn-Off Detection - Missing Branch Coverage
    // ===========================================

    /**
     * Tests detectTurnOff when both old and new currents are above threshold.
     * This covers the missing branch: oldCurrent >= threshold AND newCurrent >= threshold.
     */
    @Test
    public void testDetectTurnOff_BothAboveThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Both currents above threshold - no turn-off should be detected
        assertFalse(calc.detectTurnOff(5.0, 10.0));  // 5A -> 10A, both > 10mA threshold
    }

    /**
     * Tests detectTurnOff when current decreases but stays above threshold.
     * Verifies turn-off is only detected when crossing below threshold.
     */
    @Test
    public void testDetectTurnOff_DecreasesButStaysOn() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Current decreases from 10A to 1A, but both still above threshold
        assertFalse(calc.detectTurnOff(10.0, 1.0));
    }

    /**
     * Tests detectTurnOff at exactly the threshold boundary.
     */
    @Test
    public void testDetectTurnOff_NewAtThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Old current above threshold, new current exactly at threshold - should NOT detect turn-off
        // because newCurrent is not < threshold
        assertFalse(calc.detectTurnOff(1.0, SwitchingLossCalculator.CURRENT_THRESHOLD));
    }

    /**
     * Tests detectTurnOff with old current exactly at threshold.
     * Current at threshold should be considered "on" so turning off from there is valid.
     */
    @Test
    public void testDetectTurnOff_OldAtThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Old current exactly at threshold, new current below - should detect turn-off
        assertTrue(calc.detectTurnOff(SwitchingLossCalculator.CURRENT_THRESHOLD, 0.001));
    }

    // ===========================================
    // Turn-On to Turn-Off Ratio - Zero Denominator
    // ===========================================

    /**
     * Tests getTurnOnToTurnOffRatio when turn-off energy is zero.
     * This covers the missing branch at lines 305-306.
     * Should return POSITIVE_INFINITY when dividing by zero.
     */
    @Test
    public void testTurnOnToTurnOffRatio_ZeroTurnOffEnergy() {
        // Create calculator with zero turn-off energy
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, 0.0, I_REF, V_REF);

        // Should return positive infinity when turn-off energy is zero
        double ratio = calc.getTurnOnToTurnOffRatio();
        assertEquals(Double.POSITIVE_INFINITY, ratio, TOLERANCE);
        assertTrue(Double.isInfinite(ratio));
        assertTrue(ratio > 0);
    }

    /**
     * Tests getTurnOnToTurnOffRatio when both energies are zero.
     * This is an edge case: 0/0 should still return POSITIVE_INFINITY based on the implementation.
     */
    @Test
    public void testTurnOnToTurnOffRatio_BothEnergiesZero() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            0.0, 0.0, I_REF, V_REF);

        // When both are zero, 0/0 -> POSITIVE_INFINITY (due to turnOffEnergyRef == 0 check)
        double ratio = calc.getTurnOnToTurnOffRatio();
        assertEquals(Double.POSITIVE_INFINITY, ratio, TOLERANCE);
    }

    /**
     * Tests getTurnOnToTurnOffRatio when only turn-on energy is zero.
     * Should return 0.0 (zero divided by non-zero).
     */
    @Test
    public void testTurnOnToTurnOffRatio_ZeroTurnOnEnergy() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            0.0, E_OFF_REF, I_REF, V_REF);

        // 0 / 0.5mJ = 0
        assertEquals(0.0, calc.getTurnOnToTurnOffRatio(), TOLERANCE);
    }

    // ===========================================
    // Additional Edge Cases for Completeness
    // ===========================================

    /**
     * Tests detectTurnOn and detectTurnOff with very small negative currents.
     * Ensures absolute value is used correctly in detection logic.
     */
    @Test
    public void testDetection_VerySmallNegativeCurrents() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Both small negative currents below threshold
        assertFalse(calc.detectTurnOn(-0.001, -0.005));

        // Both large negative currents above threshold
        assertFalse(calc.detectTurnOff(-5.0, -10.0));
    }

    /**
     * Tests detectTurnOn with negative to positive current transition.
     */
    @Test
    public void testDetectTurnOn_NegativeToPositiveAboveThreshold() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // From negative small current to positive large current - should detect turn-on
        assertTrue(calc.detectTurnOn(-0.001, 1.0));
    }

    /**
     * Tests detectTurnOff with positive to negative current transition.
     */
    @Test
    public void testDetectTurnOff_PositiveToNegativeSmall() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // From positive large current to negative small current - should detect turn-off
        assertTrue(calc.detectTurnOff(1.0, -0.001));
    }

    /**
     * Tests detectTurnOn and detectTurnOff do not trigger on opposite transitions.
     */
    @Test
    public void testDetection_NoFalsePositives() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);

        // Turn-on should not be detected when current increases above threshold from already above
        assertFalse(calc.detectTurnOn(1.0, 2.0));

        // Turn-off should not be detected when current decreases below threshold from already below
        assertFalse(calc.detectTurnOff(0.001, 0.0));
    }
}

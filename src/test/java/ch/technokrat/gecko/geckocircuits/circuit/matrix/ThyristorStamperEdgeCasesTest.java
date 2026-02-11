/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Edge case tests for ThyristorStamper - covers boundary conditions, null parameters,
 * state transitions at thresholds, and rapid switching scenarios.
 */
public class ThyristorStamperEdgeCasesTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-6;
    private ThyristorStamper stamper;

    @Before
    public void setUp() {
        stamper = new ThyristorStamper();
    }

    // ===== Edge Cases: Firing Angle Threshold Behavior =====

    @Test
    public void testFiringAngle_ExactThresholdVoltage() {
        // Test behavior at exact forward voltage threshold (1.5V)
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // At exact threshold: 1.5V (perturbation factor = 0.99 so actual = 1.485V)
        // Voltage slightly above threshold should trigger ON
        stamper.updateStateWithGate(1.0, 1.485, 0.0, 0.001);

        assertTrue("thyristor should turn ON at threshold", stamper.isOn());
    }

    @Test
    public void testFiringAngle_JustBelowThreshold() {
        // Test just below the firing angle threshold
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // Voltage just below threshold (accounting for perturbation)
        stamper.updateStateWithGate(1.0, 1.48, 0.0, 0.001);

        assertFalse("thyristor should NOT turn ON just below threshold", stamper.isOn());
    }

    @Test
    public void testFiringAngle_VerySmallPositiveVoltage() {
        // Test with very small forward voltage (below threshold)
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        stamper.updateStateWithGate(1.0, 1e-6, 0.0, 0.001);

        assertFalse("thyristor should not trigger with tiny voltage", stamper.isOn());
    }

    @Test
    public void testFiringAngle_HighVoltageGuaranteesTrigger() {
        // Test that sufficiently high voltage always triggers when gate is present
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        stamper.updateStateWithGate(1.0, 1000.0, 0.0, 0.001);

        assertTrue("thyristor should trigger at high voltage", stamper.isOn());
    }

    // ===== Edge Cases: updateStateFromParameters with null/short arrays =====

    @Test
    public void testUpdateStateFromParameters_NullArray() {
        // Should handle null parameter array gracefully
        stamper.setState(false);
        stamper.resetStateChange();

        // This should not throw and should not change state
        stamper.updateStateFromParameters(null, 3.0, 0.0, 0.001);

        assertFalse("state should not change with null parameters", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateFromParameters_ShortArrayTooShort() {
        // Array shorter than PARAM_GATE index should be handled
        stamper.setState(false);
        stamper.resetStateChange();

        double[] shortArray = new double[5]; // Too short, PARAM_GATE = 8

        // Should not throw and should not change state
        stamper.updateStateFromParameters(shortArray, 3.0, 0.0, 0.001);

        assertFalse("state should not change with short array", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateFromParameters_ArrayExactlyAtGateIndex() {
        // Array with exactly enough length for PARAM_GATE (index 8)
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        double[] array = new double[9]; // Index 0-8
        array[8] = 1.0; // Gate signal

        stamper.updateStateFromParameters(array, 3.0, 0.0, 0.001);

        assertTrue("should process gate signal at minimum length", stamper.isOn());
    }

    @Test
    public void testUpdateStateFromParameters_ArrayTooShortForRecoveryTime() {
        // Array has gate but not recovery time - should not crash
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        double[] array = new double[9]; // No PARAM_RECOVERY_TIME (index 9)
        array[8] = 1.0;

        // Should not throw
        stamper.updateStateFromParameters(array, 3.0, 0.0, 0.001);

        assertTrue("should turn ON even without recovery time in array", stamper.isOn());
    }

    @Test
    public void testUpdateStateFromParameters_WithRecoveryTimeUpdate() {
        // Array includes recovery time - should update internal recovery time
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.setRecoveryTime(10e-6);

        double[] array = new double[12]; // Include PARAM_RECOVERY_TIME (9) and PARAM_LAST_SWITCH_TIME (11)
        array[8] = 1.0; // Gate
        array[9] = 5e-6; // Different recovery time
        array[11] = Double.NEGATIVE_INFINITY;

        stamper.updateStateFromParameters(array, 3.0, 0.0, 0.001);

        // Verify recovery time was updated from parameters
        assertEquals("recovery time should be updated from parameters", 5e-6, stamper.getRecoveryTime(), TOLERANCE);
    }

    @Test
    public void testUpdateStateFromParameters_LastSwitchTimeUpdate() {
        // Array should update last switch-off time
        stamper.setState(true);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);

        double[] array = new double[12];
        array[8] = 1.0; // Gate ON
        array[11] = 0.005; // Last switch time

        stamper.updateStateFromParameters(array, 1.0, 0.0, 0.01); // Will try to turn OFF

        // Since lastSwitchOffTime was set to 0.005 and time is 0.01,
        // with default recovery of 10e-6, turn-off should be possible
        // (0.01 - 0.005 = 5e-3 >> 10e-6)
    }

    // ===== Edge Cases: State Transitions at Exact Threshold Values =====

    @Test
    public void testVoltageThresholdTransition_OnToOff() {
        // Test that voltage below threshold during ON state attempts turn-off
        // The actual turn-off depends on recovery time logic
        stamper.setState(true);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY); // Very long ago
        stamper.resetStateChange();

        // Voltage just below turn-off threshold - should attempt turn-off
        // With lastSwitchOffTime = -INF, time - last = 0.001 - (-INF) = INF > 3*recovery
        // So will reset lastSwitchOffTime = 0.001
        // Then next check: 0.001 - 0.001 = 0 < recoveryTime, so stays ON
        // This tests the recovery window logic
        stamper.updateStateWithGate(1.0, stamper.getUForward() * 0.95, 0.0, 0.001);

        // Should still be ON because recovery window hasn't passed yet
        assertTrue("should stay ON until recovery passes", stamper.isOn());
    }

    @Test
    public void testVoltageThresholdTransition_OffToOn() {
        // Test exact transition from OFF to ON as voltage rises
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // Very small forward voltage - should stay OFF
        stamper.updateStateWithGate(1.0, 1.4, 0.0, 0.001);
        assertFalse("low voltage stays OFF", stamper.isOn());

        // Jump to just above threshold
        stamper.updateStateWithGate(1.0, 1.50, 0.0, 0.001);
        assertTrue("voltage above threshold turns ON", stamper.isOn());
    }

    @Test
    public void testMultipleTransitionsAroundThreshold() {
        // Rapid voltage changes around the threshold - tests latching behavior
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);

        // Start: OFF, no gate
        stamper.updateStateWithGate(0.0, 1.0, 0.0, 0.001);
        assertFalse("initial OFF", stamper.isOn());

        // Jump to threshold: ON (gate present, voltage high)
        stamper.updateStateWithGate(1.0, 2.0, 0.0, 0.002);
        assertTrue("crosses threshold, turns ON", stamper.isOn());

        // Gate removed but stays ON (latching behavior)
        stamper.updateStateWithGate(0.0, 1.4, 0.0, 0.003);
        assertTrue("stays ON without gate (latching)", stamper.isOn());

        // Voltage still forward, no turn-off yet
        stamper.updateStateWithGate(0.0, 1.5, 0.0, 0.004);
        assertTrue("stays ON with forward voltage", stamper.isOn());
    }

    // ===== Edge Cases: Zero Forward Voltage =====

    @Test
    public void testZeroForwardVoltageCompensation() {
        // Test with zero forward voltage drop
        stamper.setUForward(0.0);
        stamper.setState(true);

        double[] parameter = new double[12];
        parameter[0] = 1e-3; // Current resistance
        parameter[1] = 0.0; // Zero forward voltage
        parameter[2] = 1e-3; // rOn
        parameter[3] = 1e9; // rOff

        double[] b = new double[3];
        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, new double[10]);

        // With zero forward voltage, B vector contribution should be zero
        assertEquals("zero forward voltage gives no compensation", 0.0, b[1], TOLERANCE);
        assertEquals("zero forward voltage gives no compensation", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testNegativeForwardVoltageHandling() {
        // Test with negative forward voltage (edge case)
        // Note: stampVectorB checks if uf > 0 before adding compensation
        stamper.setUForward(-0.5);
        stamper.setState(true);

        double[] parameter = new double[12];
        parameter[0] = 1e-3;
        parameter[1] = -0.5; // Negative forward voltage
        parameter[2] = 1e-3;
        parameter[3] = 1e9;

        double[] b = new double[3];
        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, new double[10]);

        // Negative forward voltage: uf < 0, so condition 'uf > 0' is false
        // No compensation is added
        assertEquals("negative forward voltage gives no compensation", 0.0, b[1], TOLERANCE);
    }

    // ===== Edge Cases: Multiple Rapid State Changes =====

    @Test
    public void testRapidGatePulses() {
        // Simulate rapid gate pulses - thyristor latches on first pulse
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);

        double time = 0.0;
        double dt = 1e-7; // Very small time step

        // First pulse
        stamper.updateStateWithGate(1.0, 3.0, 0.0, time);
        assertTrue("turns ON on first pulse", stamper.isOn());

        time += dt;

        // Multiple subsequent pulses - gate removed
        for (int i = 0; i < 5; i++) {
            stamper.updateStateWithGate(0.0, 3.0, 0.0, time);
            assertTrue("stays ON (latching behavior)", stamper.isOn());
            time += dt;
        }
    }

    @Test
    public void testRapidRecurrenceAfterTurnOff() {
        // Test rapid turn-off and re-trigger cycles
        stamper.setState(false);
        stamper.setRecoveryTime(5e-6);

        double time = 0.0;

        // Turn ON
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.updateStateWithGate(1.0, 3.0, 0.0, time);
        assertTrue("first turn-on", stamper.isOn());

        // Trigger turn-off (current zero detected)
        time = 1e-6;
        stamper.setLastSwitchOffTime(time); // Mark when turn-off starts
        // Now at time + recovery, we should turn off
        stamper.updateStateWithGate(1.0, 0.5, 0.0, time + stamper.getRecoveryTime() + 1e-6);
        assertFalse("turns off", stamper.isOn());

        // At exactly the turn-off time, try to retrigger during recovery window
        // Since lastSwitchOffTime was set to the turn-off time, retrigger window hasn't elapsed
        double lastTurnOffTime = time + stamper.getRecoveryTime() + 1e-6;
        stamper.setLastSwitchOffTime(lastTurnOffTime);
        stamper.updateStateWithGate(1.0, 3.0, 0.0, lastTurnOffTime + 0.5e-6);
        assertFalse("cannot retrigger within recovery window", stamper.isOn());

        // After recovery time passes from last turn-off, should trigger
        stamper.updateStateWithGate(1.0, 3.0, 0.0, lastTurnOffTime + stamper.getRecoveryTime() + 1e-6);
        assertTrue("retriggering after recovery period", stamper.isOn());
    }

    @Test
    public void testStateChangeFlag_RapidToggles() {
        // Verify state change flag is properly set during rapid transitions
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // Change 1: OFF -> ON
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 0.001);
        assertTrue("first change detected", stamper.isStateChanged());

        stamper.resetStateChange();
        assertFalse("flag reset", stamper.isStateChanged());

        // Change 2: Still ON (gate + voltage both present)
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 0.002);
        assertFalse("no change when staying ON", stamper.isStateChanged());

        // Change 3: Voltage drop doesn't immediately turn off due to recovery time
        stamper.resetStateChange();
        stamper.updateStateWithGate(1.0, 0.5, 0.0, 0.003);
        // State stays ON (recovery time prevents immediate turn-off)
        assertTrue("still ON until recovery expires", stamper.isOn());
        assertFalse("no state change yet", stamper.isStateChanged());
    }

    // ===== Edge Cases: Extreme Parameter Values =====

    @Test
    public void testMinimumResistanceClamping() {
        // Verify minimum resistance prevents numerical issues
        stamper.setROn(1e-20); // Try to set extremely small

        assertTrue("rOn should be clamped to minimum", stamper.getROn() >= 1e-12);
    }

    @Test
    public void testMaximumResistanceClamping() {
        // Verify maximum resistance prevents overflow
        stamper.setROff(1e30); // Try to set extremely large

        assertTrue("rOff should be clamped to maximum", stamper.getROff() <= 1e15);
    }

    @Test
    public void testVerySmallRecoveryTime() {
        // Test with extremely small recovery time
        stamper.setRecoveryTime(1e-12);
        stamper.setState(false);
        stamper.setLastSwitchOffTime(0.0);

        // Should retrigger almost immediately
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 1e-12 + 0.1e-12);

        assertTrue("should retrigger with tiny recovery time", stamper.isOn());
    }

    @Test
    public void testVeryLargeRecoveryTime() {
        // Test with very large recovery time
        stamper.setRecoveryTime(1000.0); // 1000 seconds
        stamper.setState(false);
        stamper.setLastSwitchOffTime(0.0);

        // Should NOT retrigger within short window
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 1.0);

        assertFalse("should not retrigger with huge recovery time", stamper.isOn());

        // But should retrigger after recovery time
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 1001.0);

        assertTrue("should retrigger after large recovery time", stamper.isOn());
    }

    // ===== Edge Cases: Current Calculation Edge Cases =====

    @Test
    public void testCalculateCurrent_VeryHighResistance() {
        // Test current calculation with extremely high resistance
        stamper.setState(false); // OFF state, very high resistance
        double[] parameter = ThyristorStamper.createParameters(1e15, 1.5, 1e-3, 1e15, 0, 10e-6, Double.NEGATIVE_INFINITY);

        double current = stamper.calculateCurrent(10.0, 0.0, parameter, 1e-6, 0.0);

        // I = V / R = 10 / 1e15 = 1e-14
        assertEquals("leakage with huge resistance", 1e-14, Math.abs(current), 1e-15);
    }

    @Test
    public void testCalculateCurrent_AtForwardVoltageThreshold() {
        // Test current when voltage exactly equals forward drop
        stamper.setState(true);
        double[] parameter = ThyristorStamper.createParameters(1e-3, 1.5, 1e-3, 1e9, 1, 10e-6, Double.NEGATIVE_INFINITY);

        // Voltage = forward voltage = 1.5V
        double current = stamper.calculateCurrent(1.5, 0.0, parameter, 1e-6, 0.0);

        // I = (V - Uf) / rOn = (1.5 - 1.5) / 1e-3 = 0
        assertEquals("current at forward voltage equals zero", 0.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_BelowForwardVoltageThreshold() {
        // Test current when ON but voltage below forward drop
        stamper.setState(true);
        double[] parameter = ThyristorStamper.createParameters(1e-3, 1.5, 1e-3, 1e9, 1, 10e-6, Double.NEGATIVE_INFINITY);

        // Voltage = 1.0V < forward voltage = 1.5V (shouldn't happen in reality)
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);

        // I = (V - Uf) / rOn = (1.0 - 1.5) / 1e-3 = -500 (negative!)
        assertTrue("current can be negative when V < Uf", current < 0);
    }

    // ===== Edge Cases: Matrix Stamping with Boundary Conditions =====

    @Test
    public void testStampMatrixA_WithMinimalResistance() {
        // Stamping with minimum allowed resistance
        double[][] a = new double[3][3];
        stamper.setState(true);
        stamper.setROn(1e-12 + 0.1e-12); // Just above minimum

        double[] parameter = new double[9];
        parameter[0] = stamper.getROn();

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // With very low resistance, admittance should be very high
        double expectedAdmittance = 1.0 / stamper.getROn();
        assertEquals("stamping with min resistance", expectedAdmittance, a[1][1], LOOSE_TOLERANCE);
    }

    @Test
    public void testStampVectorB_StateTransitionBoundary() {
        // Test B vector stamping right at state transition boundary
        stamper.setState(true);
        stamper.setROn(1e-3);
        stamper.setROff(1e9);

        // Create parameter that's right at the 0.5*rOff threshold
        double[] parameter = new double[12];
        parameter[0] = 0.5 * stamper.getROff(); // Threshold resistance
        parameter[1] = 1.5; // Forward voltage

        double[] b = new double[3];
        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, new double[10]);

        // At threshold, should have some B vector contribution
        // (resistance < 0.5*rOff when ON)
    }

    // ===== Edge Cases: Recovery Time Window Logic =====

    @Test
    public void testRecoveryTime_ExactlyAtBoundary() {
        // Test at exact recovery time boundary
        stamper.setState(false);
        stamper.setRecoveryTime(10e-6);
        stamper.setLastSwitchOffTime(0.0);
        stamper.resetStateChange();

        // Exactly at recovery time
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 10e-6);

        assertTrue("should retrigger exactly at recovery time", stamper.isOn());
    }

    @Test
    public void testRecoveryTime_JustBeforeBoundary() {
        // Test just before recovery time expires
        stamper.setState(false);
        stamper.setRecoveryTime(10e-6);
        stamper.setLastSwitchOffTime(0.0);
        stamper.resetStateChange();

        // Just before recovery time
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 10e-6 - 1e-12);

        assertFalse("should not retrigger before recovery time", stamper.isOn());
    }

    @Test
    public void testRecoveryTime_3xMultiplierBehavior() {
        // Test the 3x multiplier logic for recovery window
        stamper.setState(false);
        stamper.setRecoveryTime(10e-6);
        stamper.setLastSwitchOffTime(1e-3);

        // time - lastSwitchOffTime = 1.05e-3 - 1e-3 = 50e-6
        // 50e-6 > 3*10e-6 = 30e-6, so lastSwitchOffTime resets
        stamper.resetStateChange();
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 1.05e-3);

        assertTrue("should allow retrigger after 3x window", stamper.isOn());
    }

    @Test
    public void testFireTrigger_WithNullGateSignal() {
        // fireTrigger should work as shorthand for updateStateWithGate(1.0, ...)
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        stamper.fireTrigger(3.0, 0.0, 0.001);

        assertTrue("fire trigger should turn on", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }
}

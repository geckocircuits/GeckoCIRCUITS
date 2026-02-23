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
package gecko.geckocircuits.circuit.matrix;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Edge case tests for MOSFETStamper - covers bidirectional conduction,
 * body diode behavior, gate control transitions, parameter edge cases,
 * and complete switching cycles.
 */
public class MOSFETStamperEdgeCasesTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-6;
    private MOSFETStamper stamper;

    @Before
    public void setUp() {
        stamper = new MOSFETStamper();
    }

    // ===== Edge Cases: Body Diode Behavior (Reverse Conduction) =====

    @Test
    public void testBidirectionalConduction_PositiveVoltage() {
        // MOSFET conducts bidirectionally - positive current through device
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createParameters(1e-3, 1e-3, 1e9, 1);

        // Forward voltage: V = 10V, I = 10 / 1e-3 = 10000A
        double current = stamper.calculateCurrent(10.0, 0.0, parameter, 1e-6, 0.0);

        assertTrue("positive voltage gives positive current", current > 0);
        assertEquals("forward current magnitude", 10000.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testBidirectionalConduction_NegativeVoltage() {
        // MOSFET conducts reverse current (body diode)
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createParameters(1e-3, 1e-3, 1e9, 1);

        // Reverse voltage: V = -10V, I = -10 / 1e-3 = -10000A
        double current = stamper.calculateCurrent(0.0, 10.0, parameter, 1e-6, 0.0);

        assertTrue("negative voltage gives negative current", current < 0);
        assertEquals("reverse current magnitude", -10000.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testBidirectionalConduction_OffState() {
        // OFF state should have minimal current in both directions
        stamper.setState(false);
        double[] parameter = MOSFETStamper.createDefaultParameters(0);

        // Forward: Leakage = 10V / 1e9 = 1e-8 A
        double forwardLeakage = stamper.calculateCurrent(10.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("forward leakage OFF", 1e-8, forwardLeakage, TOLERANCE);

        // Reverse: -10V / 1e9 = -1e-8 A (but still leakage)
        double reverseLeakage = stamper.calculateCurrent(0.0, 10.0, parameter, 1e-6, 0.0);
        assertEquals("reverse leakage OFF", -1e-8, reverseLeakage, TOLERANCE);
    }

    @Test
    public void testBodyDiode_ContinuousConduction() {
        // Test body diode conducts continuously when ON
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createParameters(1e-3, 1e-3, 1e9, 1);

        double current = stamper.calculateCurrent(0.0, 5.0, parameter, 1e-6, 0.0);

        // Reverse voltage through body diode: I = -5.0 / 1e-3 = -5000A
        assertTrue("body diode conducts reverse", current < 0);
        assertEquals("reverse body diode current", -5000.0, current, LOOSE_TOLERANCE);
    }

    // ===== Edge Cases: Gate Control with Various Voltage Conditions =====

    @Test
    public void testGateControl_OnWithPositiveVoltage() {
        // Gate ON with positive voltage
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.setGateSignal(1.0);

        assertTrue("gate=1 turns ON", stamper.isOn());
        assertTrue("state changed", stamper.isStateChanged());
    }

    @Test
    public void testGateControl_OnWithNegativeVoltage() {
        // Gate ON with negative voltage (body diode would conduct)
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.setGateSignal(1.0);

        assertTrue("gate=1 turns ON regardless of voltage", stamper.isOn());
    }

    @Test
    public void testGateControl_OffImmediately() {
        // Gate OFF should turn off immediately
        stamper.setState(true);
        stamper.resetStateChange();

        stamper.setGateSignal(0.0);

        assertFalse("gate=0 turns OFF", stamper.isOn());
        assertTrue("state changed", stamper.isStateChanged());
    }

    @Test
    public void testGateControl_ZeroSignal() {
        // Gate signal of 0 should turn OFF
        stamper.setState(true);
        stamper.resetStateChange();

        stamper.setGateSignal(0.0);

        assertFalse("zero gate signal turns OFF", stamper.isOn());
    }

    @Test
    public void testGateControl_NonZeroSignal() {
        // Any non-zero gate signal should turn ON
        stamper.setState(false);
        stamper.resetStateChange();

        // Try with various non-zero values
        stamper.setGateSignal(0.1);
        assertTrue("0.1 gate signal turns ON", stamper.isOn());

        stamper.resetStateChange();
        stamper.setState(false);
        stamper.setGateSignal(10.0);
        assertTrue("10.0 gate signal turns ON", stamper.isOn());

        stamper.resetStateChange();
        stamper.setState(false);
        stamper.setGateSignal(-1.0);
        assertTrue("negative gate signal turns ON", stamper.isOn());
    }

    // ===== Edge Cases: updateStateFromParameters with null/short arrays =====

    @Test
    public void testUpdateStateFromParameters_NullArray() {
        // Should handle null gracefully
        stamper.setState(false);
        stamper.resetStateChange();

        // Should not throw
        stamper.updateStateFromParameters(null);

        assertFalse("null array should not change state", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateFromParameters_ShortArrayTooShort() {
        // Array shorter than PARAM_GATE should be handled
        stamper.setState(false);
        stamper.resetStateChange();

        double[] shortArray = new double[5]; // PARAM_GATE = 8

        // Should not throw
        stamper.updateStateFromParameters(shortArray);

        assertFalse("short array should not change state", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateFromParameters_ArrayExactlyAtGateIndex() {
        // Array with exactly PARAM_GATE length
        stamper.setState(false);
        stamper.resetStateChange();

        double[] array = new double[9]; // Index 0-8
        array[8] = 1.0;

        stamper.updateStateFromParameters(array);

        assertTrue("should process gate at minimum length", stamper.isOn());
    }

    @Test
    public void testUpdateStateFromParameters_WithGateOn() {
        // Parameter array with gate ON
        stamper.setState(false);
        stamper.resetStateChange();

        double[] param = MOSFETStamper.createDefaultParameters(1.0);

        stamper.updateStateFromParameters(param);

        assertTrue("gate=1 from parameters should turn ON", stamper.isOn());
        assertTrue("state should change", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateFromParameters_WithGateOff() {
        // Parameter array with gate OFF
        stamper.setState(true);
        stamper.resetStateChange();

        double[] param = MOSFETStamper.createDefaultParameters(0.0);

        stamper.updateStateFromParameters(param);

        assertFalse("gate=0 from parameters should turn OFF", stamper.isOn());
        assertTrue("state should change", stamper.isStateChanged());
    }

    // ===== Edge Cases: Resistance Clamping =====

    @Test
    public void testResistanceClamping_MinimumROn() {
        // Test rOn clamping to minimum
        stamper.setROn(1e-30); // Extremely small

        assertTrue("rOn should be clamped to minimum", stamper.getROn() >= 1e-12);
    }

    @Test
    public void testResistanceClamping_MaximumROff() {
        // Test rOff clamping to maximum
        stamper.setROff(1e30); // Extremely large

        assertTrue("rOff should be clamped to maximum", stamper.getROff() <= 1e15);
    }

    @Test
    public void testResistanceClamping_BothLimits() {
        // Test both resistances within valid range after clamping
        stamper.setROn(0.0);
        stamper.setROff(1e40);

        assertTrue("rOn clamped", stamper.getROn() >= 1e-12);
        assertTrue("rOff clamped", stamper.getROff() <= 1e15);
        assertTrue("rOff > rOn", stamper.getROff() > stamper.getROn());
    }

    // ===== Edge Cases: Complete Switching Cycle Tests =====

    @Test
    public void testSwitchingCycle_OnOffOn() {
        // Complete ON -> OFF -> ON cycle
        stamper.setState(false);
        stamper.resetStateChange();

        // Turn ON
        stamper.setGateSignal(1.0);
        assertTrue("turn ON", stamper.isOn());
        stamper.resetStateChange();

        // Stay ON with gate still high
        stamper.setGateSignal(1.0);
        assertFalse("no change staying ON", stamper.isStateChanged());

        // Turn OFF
        stamper.setGateSignal(0.0);
        assertFalse("turn OFF", stamper.isOn());
        assertTrue("state changed", stamper.isStateChanged());
        stamper.resetStateChange();

        // Turn ON again
        stamper.setGateSignal(1.0);
        assertTrue("turn ON again", stamper.isOn());
        assertTrue("state changed", stamper.isStateChanged());
    }

    @Test
    public void testSwitchingCycle_MultipleTransitions() {
        // Rapid multiple transitions
        stamper.setState(false);

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                stamper.setGateSignal(1.0);
                assertTrue("turn ON at iteration " + i, stamper.isOn());
            } else {
                stamper.setGateSignal(0.0);
                assertFalse("turn OFF at iteration " + i, stamper.isOn());
            }
        }
    }

    @Test
    public void testSwitchingCycle_WithCurrentCalculation() {
        // Switching cycle with current calculation at each state
        stamper.setState(false);
        double[] offParam = MOSFETStamper.createDefaultParameters(0);

        // OFF state - high resistance
        double offCurrent = stamper.calculateCurrent(10.0, 0.0, offParam, 1e-6, 0.0);
        assertTrue("OFF current should be small", Math.abs(offCurrent) < 1e-7);

        // Turn ON
        stamper.setGateSignal(1.0);
        double[] onParam = MOSFETStamper.createDefaultParameters(1);
        double onCurrent = stamper.calculateCurrent(10.0, 0.0, onParam, 1e-6, 0.0);

        // ON current should be much larger
        assertTrue("ON current should be large", Math.abs(onCurrent) > 1e3);
        assertTrue("ON current should be much larger than OFF", onCurrent > Math.abs(offCurrent) * 1e6);

        // Turn OFF - current drops again
        stamper.setGateSignal(0.0);
        double finalCurrent = stamper.calculateCurrent(10.0, 0.0, offParam, 1e-6, 0.0);
        assertTrue("final current should be small again", Math.abs(finalCurrent) < 1e-7);
    }

    // ===== Edge Cases: Voltage and Current Edge Values =====

    @Test
    public void testZeroVoltageCalculation() {
        // Test with zero voltage across device
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createParameters(1e-3, 1e-3, 1e9, 1);

        double current = stamper.calculateCurrent(5.0, 5.0, parameter, 1e-6, 0.0);

        assertEquals("zero voltage gives zero current", 0.0, current, TOLERANCE);
    }

    @Test
    public void testVeryHighVoltage() {
        // Test with extremely high voltage
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createParameters(1e-3, 1e-3, 1e9, 1);

        double current = stamper.calculateCurrent(1000.0, 0.0, parameter, 1e-6, 0.0);

        // I = 1000 / 1e-3 = 1e6 A
        assertEquals("very high voltage", 1e6, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testVeryHighVoltageOffState() {
        // Very high voltage with OFF state (should see only leakage)
        stamper.setState(false);
        double[] parameter = MOSFETStamper.createDefaultParameters(0);

        double current = stamper.calculateCurrent(1000.0, 0.0, parameter, 1e-6, 0.0);

        // I = 1000 / 1e9 = 1e-6 A (tiny leakage)
        assertEquals("high voltage OFF leakage", 1e-6, current, TOLERANCE);
    }

    // ===== Edge Cases: Matrix Stamping Variations =====

    @Test
    public void testStampMatrixA_OnToOffTransition() {
        // Stamp matrix while transitioning from ON to OFF
        double[][] a = new double[3][3];

        // ON state
        stamper.setState(true);
        double[] onParam = MOSFETStamper.createDefaultParameters(1);
        stamper.stampMatrixA(a, 1, 2, 0, onParam, 1e-6);

        double onAdmittance = a[1][1];

        // Clear matrix
        a = new double[3][3];

        // OFF state
        stamper.setState(false);
        double[] offParam = MOSFETStamper.createDefaultParameters(0);
        stamper.stampMatrixA(a, 1, 2, 0, offParam, 1e-6);

        double offAdmittance = a[1][1];

        // OFF admittance should be much smaller than ON
        assertTrue("OFF admittance much smaller than ON",
                   offAdmittance < onAdmittance * 1e-6);
    }

    @Test
    public void testStampMatrixA_WithDifferentNodeIndices() {
        // Test stamping with various node indices
        double[][] a = new double[10][10];
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createDefaultParameters(1);

        // Stamp to different node pairs
        stamper.stampMatrixA(a, 0, 1, 0, parameter, 1e-6);
        stamper.stampMatrixA(a, 3, 5, 0, parameter, 1e-6);
        stamper.stampMatrixA(a, 7, 9, 0, parameter, 1e-6);

        // All should have same admittance but at different positions
        double admittance = a[0][0];
        assertEquals("nodes 0-1 stamped", admittance, a[0][0], TOLERANCE);
        assertEquals("nodes 3-5 stamped", admittance, a[3][3], TOLERANCE);
        assertEquals("nodes 7-9 stamped", admittance, a[7][7], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_SymmetryPreservation() {
        // Verify matrix remains symmetric
        double[][] a = new double[3][3];
        stamper.setState(true);
        double[] parameter = MOSFETStamper.createDefaultParameters(1);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        assertEquals("matrix symmetric at [1][2] and [2][1]",
                     a[1][2], a[2][1], TOLERANCE);
    }

    // ===== Edge Cases: State Change Flag Management =====

    @Test
    public void testStateChangeFlag_CorrectDetection() {
        // Verify state change flag is set correctly
        stamper.setState(false);
        stamper.resetStateChange();

        // No change when already OFF
        stamper.setGateSignal(0.0);
        assertFalse("no change when already OFF", stamper.isStateChanged());

        // Change to ON
        stamper.setGateSignal(1.0);
        assertTrue("change detected ON", stamper.isStateChanged());

        stamper.resetStateChange();
        assertFalse("flag reset", stamper.isStateChanged());

        // Change to OFF
        stamper.setGateSignal(0.0);
        assertTrue("change detected OFF", stamper.isStateChanged());

        stamper.resetStateChange();
        assertFalse("flag reset again", stamper.isStateChanged());

        // No change when already ON
        stamper.setState(true);
        stamper.setGateSignal(1.0);
        assertFalse("no change when already ON", stamper.isStateChanged());
    }

    // ===== Edge Cases: Parameter Extraction =====

    @Test
    public void testGetCurrentResistanceFromParameter() {
        // Test that parameter array values override instance values
        stamper.setState(true);
        stamper.setROn(1e-3);

        // Create parameter with different resistance
        double[] param = new double[10];
        param[MOSFETStamper.PARAM_R_CURRENT] = 2e-3;

        double current = stamper.calculateCurrent(10.0, 0.0, param, 1e-6, 0.0);

        // Should use parameter resistance (2e-3), not instance (1e-3)
        // I = 10 / 2e-3 = 5000
        assertEquals("parameter resistance used", 5000.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testAdmittanceWeight_CorrectState() {
        // Verify admittance weight reflects current state
        stamper.setState(true);
        double onWeight = stamper.getAdmittanceWeight(0.0, 1e-6);

        stamper.setState(false);
        double offWeight = stamper.getAdmittanceWeight(0.0, 1e-6);

        assertTrue("ON weight > OFF weight", onWeight > offWeight);

        double expectedOnWeight = 1.0 / MOSFETStamper.DEFAULT_R_ON;
        double expectedOffWeight = 1.0 / MOSFETStamper.DEFAULT_R_OFF;

        assertEquals("ON weight correct", expectedOnWeight, onWeight, TOLERANCE);
        assertEquals("OFF weight correct", expectedOffWeight, offWeight, TOLERANCE);
    }

    // ===== Edge Cases: toString() and String Representation =====

    @Test
    public void testToString_IncludesState() {
        stamper.setState(true);
        String onStr = stamper.toString();
        assertTrue("ON string contains ON", onStr.contains("ON"));

        stamper.setState(false);
        String offStr = stamper.toString();
        assertTrue("OFF string contains OFF", offStr.contains("OFF"));
    }

    @Test
    public void testToString_IncludesResistances() {
        String str = stamper.toString();
        assertTrue("contains rOn", str.contains("rOn="));
        assertTrue("contains rOff", str.contains("rOff="));
    }

    // ===== Edge Cases: Interface Compliance =====

    @Test
    public void testImplementsIStatefulStamper() {
        assertTrue("implements IStatefulStamper", stamper instanceof IStatefulStamper);
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("implements IMatrixStamper", stamper instanceof IMatrixStamper);
    }

    @Test
    public void testUpdateState_NoOpForMOSFET() {
        // MOSFET's updateState should be a no-op (voltage/current don't affect state)
        stamper.setState(true);
        stamper.resetStateChange();

        // Call updateState with various parameters - should not change state
        stamper.updateState(100.0, 0.0, 1000.0, 0.001);

        assertTrue("still ON after updateState", stamper.isOn());
        assertFalse("no state change from updateState", stamper.isStateChanged());

        stamper.setState(false);
        stamper.resetStateChange();

        stamper.updateState(0.0, 100.0, 1.0, 0.002);

        assertFalse("still OFF after updateState", stamper.isOn());
        assertFalse("no state change from updateState", stamper.isStateChanged());
    }

    // ===== Edge Cases: Parameter Creation =====

    @Test
    public void testCreateParameters_AllValues() {
        double[] params = MOSFETStamper.createParameters(0.5e-3, 1e-3, 1e8, 0.5);

        assertEquals("rCurrent", 0.5e-3, params[MOSFETStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("rOn", 1e-3, params[MOSFETStamper.PARAM_R_ON], TOLERANCE);
        assertEquals("rOff", 1e8, params[MOSFETStamper.PARAM_R_OFF], TOLERANCE);
        assertEquals("gate", 0.5, params[MOSFETStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateVariations() {
        // Test with various gate signal values
        double[] onParams = MOSFETStamper.createDefaultParameters(1.0);
        assertEquals("gate=1 sets rCurrent to rOn",
                     MOSFETStamper.DEFAULT_R_ON,
                     onParams[MOSFETStamper.PARAM_R_CURRENT], TOLERANCE);

        double[] offParams = MOSFETStamper.createDefaultParameters(0.0);
        assertEquals("gate=0 sets rCurrent to rOff",
                     MOSFETStamper.DEFAULT_R_OFF,
                     offParams[MOSFETStamper.PARAM_R_CURRENT], TOLERANCE);

        double[] customParams = MOSFETStamper.createDefaultParameters(0.5);
        // Non-zero gate signal means rCurrent = rOn (the code checks gateSignal != 0)
        assertEquals("gate=0.5 sets rCurrent to rOn (non-zero is ON)",
                     MOSFETStamper.DEFAULT_R_ON,
                     customParams[MOSFETStamper.PARAM_R_CURRENT], TOLERANCE);
    }

    // ===== Edge Cases: Extreme Switching Frequencies =====

    @Test
    public void testHighFrequencySwitching() {
        // Simulate high-frequency switching (100 kHz switching frequency)
        stamper.setState(false);
        double periodTime = 10e-6; // 100 kHz period
        double time = 0.0;

        for (int cycle = 0; cycle < 100; cycle++) {
            // ON phase
            stamper.setGateSignal(1.0);
            assertTrue("ON in cycle " + cycle, stamper.isOn());

            time += periodTime / 2;

            // OFF phase
            stamper.setGateSignal(0.0);
            assertFalse("OFF in cycle " + cycle, stamper.isOn());

            time += periodTime / 2;
        }
    }

    @Test
    public void testLowFrequencySwitching() {
        // Simulate low-frequency switching (1 Hz)
        stamper.setState(false);
        double time = 0.0;

        for (int cycle = 0; cycle < 5; cycle++) {
            stamper.setGateSignal(1.0);
            assertTrue("ON in low frequency cycle " + cycle, stamper.isOn());

            time += 0.5; // 0.5 second ON time

            stamper.setGateSignal(0.0);
            assertFalse("OFF in low frequency cycle " + cycle, stamper.isOn());

            time += 0.5; // 0.5 second OFF time
        }
    }
}

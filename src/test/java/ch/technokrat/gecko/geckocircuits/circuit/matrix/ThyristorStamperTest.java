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
 * Tests for ThyristorStamper - verifies correct MNA matrix stamping and state management.
 */
public class ThyristorStamperTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-6;
    private ThyristorStamper stamper;

    @Before
    public void setUp() {
        stamper = new ThyristorStamper();
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("ThyristorStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testImplementsIStatefulStamper() {
        assertTrue("ThyristorStamper should implement IStatefulStamper",
                stamper instanceof IStatefulStamper);
    }

    @Test
    public void testDefaultConstructor_InitiallyOff() {
        assertFalse("thyristor should be OFF initially", stamper.isOn());
    }

    @Test
    public void testDefaultConstructor_DefaultParameters() {
        assertEquals("default rOn", ThyristorStamper.DEFAULT_R_ON, stamper.getROn(), TOLERANCE);
        assertEquals("default rOff", ThyristorStamper.DEFAULT_R_OFF, stamper.getROff(), TOLERANCE);
        assertEquals("default uForward", ThyristorStamper.DEFAULT_U_FORWARD, stamper.getUForward(), TOLERANCE);
        assertEquals("default recoveryTime", ThyristorStamper.DEFAULT_RECOVERY_TIME, stamper.getRecoveryTime(), TOLERANCE);
    }

    @Test
    public void testParameterizedConstructor() {
        ThyristorStamper customStamper = new ThyristorStamper(1e-4, 1e8, 1.2, 20e-6);
        assertEquals("custom rOn", 1e-4, customStamper.getROn(), TOLERANCE);
        assertEquals("custom rOff", 1e8, customStamper.getROff(), TOLERANCE);
        assertEquals("custom uForward", 1.2, customStamper.getUForward(), TOLERANCE);
        assertEquals("custom recoveryTime", 20e-6, customStamper.getRecoveryTime(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_Off() {
        stamper.setState(false);
        assertEquals("OFF resistance", ThyristorStamper.DEFAULT_R_OFF, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_On() {
        stamper.setState(true);
        assertEquals("ON resistance", ThyristorStamper.DEFAULT_R_ON, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testStampMatrixA_Off() {
        double[][] a = new double[3][3];
        double[] parameter = ThyristorStamper.createDefaultParameters(0);
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / ThyristorStamper.DEFAULT_R_OFF;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
        assertEquals("a[1][2]", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1]", -expectedAdmittance, a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_On() {
        double[][] a = new double[3][3];
        double[] parameter = ThyristorStamper.createDefaultParameters(1);
        stamper.setState(true);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / ThyristorStamper.DEFAULT_R_ON;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_Off_NoContribution() {
        double[] b = new double[3];
        double[] parameter = ThyristorStamper.createDefaultParameters(0);
        double[] previousValues = new double[10];
        stamper.setState(false);

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // When OFF (high resistance), no B vector contribution
        assertEquals("b[1] should be 0 when OFF", 0.0, b[1], TOLERANCE);
        assertEquals("b[2] should be 0 when OFF", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_On_ForwardVoltageCompensation() {
        double[] b = new double[3];
        double rOn = 1e-3;
        double uForward = 1.5;
        double[] parameter = ThyristorStamper.createParameters(rOn, uForward, rOn, 1e9, 1, 10e-6, Double.NEGATIVE_INFINITY);
        double[] previousValues = new double[10];
        stamper.setState(true);

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // Compensation: G * uForward = (1/rOn) * uForward
        double expectedCompensation = (1.0 / rOn) * uForward;
        assertEquals("b[1] compensation", expectedCompensation, b[1], TOLERANCE);
        assertEquals("b[2] compensation", -expectedCompensation, b[2], TOLERANCE);
    }

    @Test
    public void testUpdateStateWithGate_TurnOn() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // Gate trigger with sufficient forward voltage
        stamper.updateStateWithGate(1.0, 2.0, 0.0, 0.001); // vForward = 2V > 1.5V

        assertTrue("thyristor should turn ON", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_StayOff_NoGate() {
        stamper.setState(false);
        stamper.resetStateChange();

        // No gate trigger, even with sufficient voltage
        stamper.updateStateWithGate(0.0, 3.0, 0.0, 0.001);

        assertFalse("thyristor should stay OFF without gate", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_StayOff_LowVoltage() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // Gate trigger but forward voltage below threshold
        stamper.updateStateWithGate(1.0, 1.0, 0.0, 0.001); // vForward = 1V < 1.5V

        assertFalse("thyristor should stay OFF with low voltage", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_LatchingBehavior() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        // Turn ON
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 0.001);
        assertTrue("thyristor should turn ON", stamper.isOn());

        stamper.resetStateChange();

        // Gate removed, but thyristor stays ON (latching)
        stamper.updateStateWithGate(0.0, 3.0, 0.0, 0.002);
        assertTrue("thyristor should stay ON (latching)", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_TurnOff_CurrentZero() {
        stamper.setState(true);
        // Set lastSwitchOffTime to be within the "continuing turn-off sequence" window
        // The logic is: if time - lastSwitchOffTime > 3*recoveryTime, reset lastSwitchOffTime = time
        // Then check if time - lastSwitchOffTime >= recoveryTime
        // We want: NOT reset (time - last <= 3*recovery) AND allow turn-off (time - last >= recovery)
        // With recoveryTime = 10e-6 and time = 0.001:
        // Need: 10e-6 <= 0.001 - lastSwitchOffTime <= 30e-6
        // So lastSwitchOffTime = 0.001 - 20e-6 = 0.00098
        double time = 0.001;
        double recoveryTime = stamper.getRecoveryTime(); // 10e-6
        stamper.setLastSwitchOffTime(time - 2 * recoveryTime); // Within window
        stamper.resetStateChange();

        // Forward voltage drops (current reaches zero)
        stamper.updateStateWithGate(1.0, 1.0, 0.0, time); // vForward = 1V < 1.5V

        assertFalse("thyristor should turn OFF at current zero", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_CannotTurnOffByGate() {
        stamper.setState(true);
        stamper.resetStateChange();

        // Gate turned OFF, but voltage still forward - thyristor stays ON
        stamper.updateStateWithGate(0.0, 3.0, 0.0, 0.001);

        assertTrue("thyristor should NOT turn OFF by gate alone", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testRecoveryTime_BlocksRetrigger() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(0.0);
        stamper.setRecoveryTime(10e-6);
        stamper.resetStateChange();

        // Try to trigger during recovery time
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 5e-6); // Within 10us recovery

        assertFalse("thyristor should not turn ON during recovery", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testRecoveryTime_AllowsRetriggerAfter() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(0.0);
        stamper.setRecoveryTime(10e-6);
        stamper.resetStateChange();

        // Try to trigger after recovery time
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 15e-6); // After 10us recovery

        assertTrue("thyristor should turn ON after recovery", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testFireTrigger() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.resetStateChange();

        stamper.fireTrigger(3.0, 0.0, 0.001);

        assertTrue("thyristor should turn ON with fire trigger", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testResetStateChange() {
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 0.001); // Turn ON
        assertTrue("state should be changed", stamper.isStateChanged());

        stamper.resetStateChange();
        assertFalse("state change flag should be reset", stamper.isStateChanged());
    }

    @Test
    public void testSetState() {
        stamper.setState(true);
        assertTrue("should be ON", stamper.isOn());

        stamper.setState(false);
        assertFalse("should be OFF", stamper.isOn());
    }

    @Test
    public void testSetState_StateChanged() {
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.setState(true);
        assertTrue("setState should set stateChanged", stamper.isStateChanged());
    }

    @Test
    public void testSetState_NoChange() {
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.setState(false);
        assertFalse("setState with same value should not set stateChanged", stamper.isStateChanged());
    }

    @Test
    public void testCalculateCurrent_On() {
        stamper.setState(true);
        double[] parameter = ThyristorStamper.createParameters(1e-3, 1.5, 1e-3, 1e9, 1, 10e-6, Double.NEGATIVE_INFINITY);

        // I = (V - Uf) / rOn = (3.0 - 1.5) / 1e-3 = 1500
        double current = stamper.calculateCurrent(3.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("ON current", 1500.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_Off() {
        stamper.setState(false);
        double[] parameter = ThyristorStamper.createDefaultParameters(0);

        // Leakage: I = V / rOff = 1.0 / 1e9 = 1e-9
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("OFF leakage current", 1e-9, current, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_On() {
        stamper.setState(true);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("ON admittance", 1.0 / ThyristorStamper.DEFAULT_R_ON, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_Off() {
        stamper.setState(false);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("OFF admittance", 1.0 / ThyristorStamper.DEFAULT_R_OFF, admittance, TOLERANCE);
    }

    @Test
    public void testSetROn() {
        stamper.setROn(5e-4);
        assertEquals("rOn should be updated", 5e-4, stamper.getROn(), TOLERANCE);
    }

    @Test
    public void testSetROn_ClampsToMinimum() {
        stamper.setROn(0.0);
        assertTrue("rOn should be clamped to minimum", stamper.getROn() > 0);
    }

    @Test
    public void testSetROff() {
        stamper.setROff(1e10);
        assertEquals("rOff should be updated", 1e10, stamper.getROff(), TOLERANCE);
    }

    @Test
    public void testSetROff_ClampsToMaximum() {
        stamper.setROff(1e20);
        assertTrue("rOff should be clamped to maximum", stamper.getROff() <= 1e15);
    }

    @Test
    public void testSetUForward() {
        stamper.setUForward(1.2);
        assertEquals("uForward should be updated", 1.2, stamper.getUForward(), TOLERANCE);
    }

    @Test
    public void testSetRecoveryTime() {
        stamper.setRecoveryTime(20e-6);
        assertEquals("recoveryTime should be updated", 20e-6, stamper.getRecoveryTime(), TOLERANCE);
    }

    @Test
    public void testSetRecoveryTime_ClampsToZero() {
        stamper.setRecoveryTime(-1e-6);
        assertEquals("recoveryTime should be clamped to 0", 0.0, stamper.getRecoveryTime(), TOLERANCE);
    }

    @Test
    public void testCreateParameters() {
        double[] params = ThyristorStamper.createParameters(1e-3, 1.2, 1e-4, 1e8, 1.0, 15e-6, 0.001);

        assertEquals("rCurrent", 1e-3, params[ThyristorStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("uForward", 1.2, params[ThyristorStamper.PARAM_U_FORWARD], TOLERANCE);
        assertEquals("rOn", 1e-4, params[ThyristorStamper.PARAM_R_ON], TOLERANCE);
        assertEquals("rOff", 1e8, params[ThyristorStamper.PARAM_R_OFF], TOLERANCE);
        assertEquals("gate", 1.0, params[ThyristorStamper.PARAM_GATE], TOLERANCE);
        assertEquals("recoveryTime", 15e-6, params[ThyristorStamper.PARAM_RECOVERY_TIME], TOLERANCE);
        assertEquals("lastSwitchTime", 0.001, params[ThyristorStamper.PARAM_LAST_SWITCH_TIME], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateOn() {
        double[] params = ThyristorStamper.createDefaultParameters(1.0);

        assertEquals("rCurrent should be rOn when gate=1", 
                     ThyristorStamper.DEFAULT_R_ON, params[ThyristorStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("uForward", ThyristorStamper.DEFAULT_U_FORWARD, params[ThyristorStamper.PARAM_U_FORWARD], TOLERANCE);
        assertEquals("gate", 1.0, params[ThyristorStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateOff() {
        double[] params = ThyristorStamper.createDefaultParameters(0.0);

        assertEquals("rCurrent should be rOff when gate=0", 
                     ThyristorStamper.DEFAULT_R_OFF, params[ThyristorStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("gate", 0.0, params[ThyristorStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testParameterConstants() {
        assertEquals("PARAM_R_CURRENT", 0, ThyristorStamper.PARAM_R_CURRENT);
        assertEquals("PARAM_U_FORWARD", 1, ThyristorStamper.PARAM_U_FORWARD);
        assertEquals("PARAM_R_ON", 2, ThyristorStamper.PARAM_R_ON);
        assertEquals("PARAM_R_OFF", 3, ThyristorStamper.PARAM_R_OFF);
        assertEquals("PARAM_GATE", 8, ThyristorStamper.PARAM_GATE);
        assertEquals("PARAM_RECOVERY_TIME", 9, ThyristorStamper.PARAM_RECOVERY_TIME);
        assertEquals("PARAM_LAST_SWITCH_TIME", 11, ThyristorStamper.PARAM_LAST_SWITCH_TIME);
    }

    @Test
    public void testDefaultConstants() {
        assertEquals("DEFAULT_R_ON", 1e-3, ThyristorStamper.DEFAULT_R_ON, TOLERANCE);
        assertEquals("DEFAULT_R_OFF", 1e9, ThyristorStamper.DEFAULT_R_OFF, TOLERANCE);
        assertEquals("DEFAULT_U_FORWARD", 1.5, ThyristorStamper.DEFAULT_U_FORWARD, TOLERANCE);
        assertEquals("DEFAULT_RECOVERY_TIME", 10e-6, ThyristorStamper.DEFAULT_RECOVERY_TIME, TOLERANCE);
    }

    @Test
    public void testToString_Off() {
        stamper.setState(false);
        String str = stamper.toString();
        assertTrue("should contain OFF", str.contains("OFF"));
    }

    @Test
    public void testToString_On() {
        stamper.setState(true);
        String str = stamper.toString();
        assertTrue("should contain ON", str.contains("ON"));
    }

    @Test
    public void testToString_ContainsParameters() {
        String str = stamper.toString();
        assertTrue("should contain rOn", str.contains("rOn="));
        assertTrue("should contain rOff", str.contains("rOff="));
        assertTrue("should contain uFwd", str.contains("uFwd="));
        assertTrue("should contain tq", str.contains("tq="));
    }

    @Test
    public void testStampMatrixA_SymmetricMatrix() {
        double[][] a = new double[3][3];
        double[] parameter = ThyristorStamper.createDefaultParameters(1);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        assertEquals("matrix should be symmetric", a[1][2], a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExisting() {
        double[][] a = new double[3][3];
        a[1][1] = 0.001;
        double[] parameter = ThyristorStamper.createDefaultParameters(0);
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / ThyristorStamper.DEFAULT_R_OFF;
        assertEquals("should add to existing", 0.001 + expectedAdmittance, a[1][1], TOLERANCE);
    }

    @Test
    public void testThyristorStateCycle() {
        // Test complete ON/OFF cycle with latching behavior
        double recoveryTime = stamper.getRecoveryTime(); // 10e-6
        
        stamper.setState(false);
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        assertFalse("initially OFF", stamper.isOn());

        // Turn ON: Gate trigger + sufficient voltage
        // No recovery time check on turn-ON in original LKMatrices code
        stamper.updateStateWithGate(1.0, 3.0, 0.0, 0.001);
        assertTrue("turned ON", stamper.isOn());

        // Stay ON even without gate (latching)
        stamper.updateStateWithGate(0.0, 3.0, 0.0, 0.002);
        assertTrue("stays ON (latching)", stamper.isOn());

        // First detection of current zero at t=0.003
        // Since lastSwitchOffTime is NEGATIVE_INFINITY, it resets to 0.003
        // Then 0.003 - 0.003 = 0 < recoveryTime, so doesn't turn off yet
        stamper.updateStateWithGate(0.0, 1.0, 0.0, 0.003);
        assertTrue("stays ON (recovery delay starting)", stamper.isOn());
        
        // Second call at current zero, after recovery time
        // Now 0.003 + 15e-6 - 0.003 = 15e-6 >= 10e-6, so turns off
        double turnOffTime = 0.003 + 15e-6;
        stamper.updateStateWithGate(0.0, 1.0, 0.0, turnOffTime);
        assertFalse("turned OFF at current zero after recovery delay", stamper.isOn());

        // Cannot retrigger immediately - this tests that lastSwitchOffTime works for re-triggering
        // After turn-off, lastSwitchOffTime should be updated
        // Actually, looking at the code, lastSwitchOffTime is only set when the turn-off
        // condition is FIRST detected (when time - last > 3*recovery), not when actually turning off
        // So lastSwitchOffTime = 0.003, and now we try to turn on at 0.003 + 20e-6
        // Check: 0.003 + 20e-6 - 0.003 = 20e-6 >= 10e-6, recovery passed, can retrigger!
        double retriggerTime = turnOffTime + 5e-6;
        stamper.updateStateWithGate(1.0, 3.0, 0.0, retriggerTime);
        assertTrue("can retrigger after recovery (lastSwitchOffTime was 0.003)", stamper.isOn());
    }

    @Test
    public void testThyristorvsIGBT_DifferentTurnOffBehavior() {
        // Key difference: IGBT turns off by gate, thyristor by current zero
        ThyristorStamper thyr = new ThyristorStamper();
        IGBTStamper igbt = new IGBTStamper();

        // Both start ON
        thyr.setState(true);
        igbt.setState(true);

        // Try to turn off by gate with forward voltage present
        thyr.updateStateWithGate(0.0, 3.0, 0.0, 0.001);
        igbt.updateStateWithGate(0.0, 3.0, 0.0);

        assertTrue("Thyristor should NOT turn OFF by gate", thyr.isOn());
        assertFalse("IGBT should turn OFF by gate", igbt.isOn());
    }
}

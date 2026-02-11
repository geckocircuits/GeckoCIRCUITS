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
 * Tests for DiodeStamper - verifies correct MNA matrix stamping and state management.
 */
public class DiodeStamperTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-6;
    private DiodeStamper stamper;

    @Before
    public void setUp() {
        stamper = new DiodeStamper();
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("DiodeStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testImplementsIStatefulStamper() {
        assertTrue("DiodeStamper should implement IStatefulStamper",
                stamper instanceof IStatefulStamper);
    }

    @Test
    public void testDefaultConstructor_InitiallyOff() {
        assertFalse("diode should be OFF initially", stamper.isOn());
    }

    @Test
    public void testDefaultConstructor_DefaultParameters() {
        assertEquals("default rOn", DiodeStamper.DEFAULT_R_ON, stamper.getROn(), TOLERANCE);
        assertEquals("default rOff", DiodeStamper.DEFAULT_R_OFF, stamper.getROff(), TOLERANCE);
        assertEquals("default uForward", DiodeStamper.DEFAULT_U_FORWARD, stamper.getUForward(), TOLERANCE);
    }

    @Test
    public void testParameterizedConstructor() {
        DiodeStamper customStamper = new DiodeStamper(1e-4, 1e8, 0.6);
        assertEquals("custom rOn", 1e-4, customStamper.getROn(), TOLERANCE);
        assertEquals("custom rOff", 1e8, customStamper.getROff(), TOLERANCE);
        assertEquals("custom uForward", 0.6, customStamper.getUForward(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_Off() {
        stamper.setState(false);
        assertEquals("OFF resistance", DiodeStamper.DEFAULT_R_OFF, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_On() {
        stamper.setState(true);
        assertEquals("ON resistance", DiodeStamper.DEFAULT_R_ON, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testStampMatrixA_Off() {
        double[][] a = new double[3][3];
        double[] parameter = DiodeStamper.createDefaultParameters();
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / DiodeStamper.DEFAULT_R_OFF;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
        assertEquals("a[1][2]", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1]", -expectedAdmittance, a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_On() {
        double[][] a = new double[3][3];
        double[] parameter = DiodeStamper.createDefaultParameters();
        stamper.setState(true);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / DiodeStamper.DEFAULT_R_ON;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_Off_NoContribution() {
        double[] b = new double[3];
        double[] parameter = DiodeStamper.createDefaultParameters();
        double[] previousValues = new double[10];
        stamper.setState(false);

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        assertEquals("b[1] should be 0 when OFF", 0.0, b[1], TOLERANCE);
        assertEquals("b[2] should be 0 when OFF", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_On_ForwardVoltageCompensation() {
        double[] b = new double[3];
        double rOn = 1e-3;
        double rOff = 1e9;
        double uForward = 0.7;
        double[] parameter = DiodeStamper.createParameters(rOn, rOff, uForward);
        double[] previousValues = new double[10];
        stamper.setState(true);

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // Compensation: G * uForward = (1/rOn) * uForward
        double expectedCompensation = (1.0 / rOn) * uForward;
        assertEquals("b[1] compensation", expectedCompensation, b[1], TOLERANCE);
        assertEquals("b[2] compensation", -expectedCompensation, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_On_ZeroForwardVoltage() {
        double[] b = new double[3];
        double[] parameter = DiodeStamper.createParameters(1e-3, 1e9, 0.0);
        double[] previousValues = new double[10];
        stamper.setState(true);

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        assertEquals("b[1] with zero uForward", 0.0, b[1], TOLERANCE);
    }

    @Test
    public void testUpdateState_TurnOn_ForwardBias() {
        stamper.setState(false);
        stamper.resetStateChange();

        // Forward voltage exceeds threshold
        stamper.updateState(1.0, 0.0, 0.0, 0.0); // vx=1V, vy=0V, vForward=1V > 0.7V

        assertTrue("diode should turn ON", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateState_StayOff_BelowThreshold() {
        stamper.setState(false);
        stamper.resetStateChange();

        // Forward voltage below threshold
        stamper.updateState(0.5, 0.0, 0.0, 0.0); // vx=0.5V < 0.7V threshold

        assertFalse("diode should stay OFF", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateState_TurnOff_ReverseCurrent() {
        stamper.setState(true);
        stamper.resetStateChange();

        // Negative current (reverse)
        stamper.updateState(1.0, 0.0, -0.1, 0.0);

        assertFalse("diode should turn OFF with reverse current", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateState_StayOn_PositiveCurrent() {
        stamper.setState(true);
        stamper.resetStateChange();

        // Positive current, stays ON
        stamper.updateState(1.0, 0.0, 0.1, 0.0);

        assertTrue("diode should stay ON", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateState_ReverseBias() {
        stamper.setState(false);
        stamper.resetStateChange();

        // Reverse bias (negative forward voltage)
        stamper.updateState(0.0, 1.0, 0.0, 0.0); // vForward = -1V

        assertFalse("diode should stay OFF in reverse bias", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testResetStateChange() {
        stamper.setState(false);
        stamper.updateState(1.0, 0.0, 0.0, 0.0); // Turn ON
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

        stamper.setState(false); // Same state
        assertFalse("setState with same value should not set stateChanged", stamper.isStateChanged());
    }

    @Test
    public void testCalculateCurrent_On() {
        stamper.setState(true);
        double[] parameter = DiodeStamper.createParameters(1e-3, 1e9, 0.7);

        // I = (V - Uf) / rOn = (1.0 - 0.7) / 1e-3 = 300
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("ON current", 300.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_Off() {
        stamper.setState(false);
        double[] parameter = DiodeStamper.createParameters(1e-3, 1e9, 0.7);

        // Leakage: I = V / rOff = 1.0 / 1e9 = 1e-9
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("OFF leakage current", 1e-9, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ReverseVoltage() {
        stamper.setState(false);
        double[] parameter = DiodeStamper.createDefaultParameters();

        double current = stamper.calculateCurrent(0.0, 1.0, parameter, 1e-6, 0.0);
        assertTrue("reverse current should be negative", current < 0);
    }

    @Test
    public void testGetAdmittanceWeight_On() {
        stamper.setState(true);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("ON admittance", 1.0 / DiodeStamper.DEFAULT_R_ON, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_Off() {
        stamper.setState(false);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("OFF admittance", 1.0 / DiodeStamper.DEFAULT_R_OFF, admittance, TOLERANCE);
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
        stamper.setUForward(0.6);
        assertEquals("uForward should be updated", 0.6, stamper.getUForward(), TOLERANCE);
    }

    @Test
    public void testCreateParameters() {
        double[] params = DiodeStamper.createParameters(1e-4, 1e8, 0.65);

        assertEquals("should have 3 elements", 3, params.length);
        assertEquals("rOn", 1e-4, params[DiodeStamper.PARAM_R_ON], TOLERANCE);
        assertEquals("rOff", 1e8, params[DiodeStamper.PARAM_R_OFF], TOLERANCE);
        assertEquals("uForward", 0.65, params[DiodeStamper.PARAM_U_FORWARD], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters() {
        double[] params = DiodeStamper.createDefaultParameters();

        assertEquals("should have 3 elements", 3, params.length);
        assertEquals("default rOn", DiodeStamper.DEFAULT_R_ON, params[0], TOLERANCE);
        assertEquals("default rOff", DiodeStamper.DEFAULT_R_OFF, params[1], TOLERANCE);
        assertEquals("default uForward", DiodeStamper.DEFAULT_U_FORWARD, params[2], TOLERANCE);
    }

    @Test
    public void testParameterConstants() {
        assertEquals("PARAM_R_ON", 0, DiodeStamper.PARAM_R_ON);
        assertEquals("PARAM_R_OFF", 1, DiodeStamper.PARAM_R_OFF);
        assertEquals("PARAM_U_FORWARD", 2, DiodeStamper.PARAM_U_FORWARD);
    }

    @Test
    public void testDefaultConstants() {
        assertEquals("DEFAULT_R_ON", 1e-3, DiodeStamper.DEFAULT_R_ON, TOLERANCE);
        assertEquals("DEFAULT_R_OFF", 1e9, DiodeStamper.DEFAULT_R_OFF, TOLERANCE);
        assertEquals("DEFAULT_U_FORWARD", 0.7, DiodeStamper.DEFAULT_U_FORWARD, TOLERANCE);
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
    }

    @Test
    public void testStampMatrixA_SymmetricMatrix() {
        double[][] a = new double[3][3];
        double[] parameter = DiodeStamper.createDefaultParameters();

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        assertEquals("matrix should be symmetric", a[1][2], a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExisting() {
        double[][] a = new double[3][3];
        a[1][1] = 0.001;
        double[] parameter = DiodeStamper.createDefaultParameters();
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / DiodeStamper.DEFAULT_R_OFF;
        assertEquals("should add to existing", 0.001 + expectedAdmittance, a[1][1], TOLERANCE);
    }

    @Test
    public void testDiodeStateCycle() {
        // Test complete ON/OFF cycle
        stamper.setState(false);
        assertFalse("initially OFF", stamper.isOn());

        // Turn ON
        stamper.updateState(1.0, 0.0, 0.0, 0.0);
        assertTrue("turned ON", stamper.isOn());

        // Stay ON with positive current
        stamper.updateState(1.0, 0.0, 1.0, 0.001);
        assertTrue("stays ON", stamper.isOn());

        // Turn OFF with reverse current
        stamper.updateState(1.0, 0.0, -0.1, 0.002);
        assertFalse("turned OFF", stamper.isOn());

        // Stay OFF below threshold
        stamper.updateState(0.5, 0.0, 0.0, 0.003);
        assertFalse("stays OFF", stamper.isOn());
    }

    @Test
    public void testNullParameterArray_UsesInstanceValues() {
        stamper.setState(true);
        stamper.setROn(2e-3);

        double current = stamper.calculateCurrent(1.0, 0.0, null, 1e-6, 0.0);

        // With null params, should use instance rOn and uForward
        double expected = (1.0 - stamper.getUForward()) / stamper.getROn();
        assertEquals("should use instance values", expected, current, LOOSE_TOLERANCE);
    }
}

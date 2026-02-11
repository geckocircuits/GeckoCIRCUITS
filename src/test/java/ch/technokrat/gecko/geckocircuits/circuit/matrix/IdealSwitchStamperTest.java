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
 * Tests for IdealSwitchStamper - verifies correct MNA matrix stamping and state management.
 */
public class IdealSwitchStamperTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-6;
    private IdealSwitchStamper stamper;

    @Before
    public void setUp() {
        stamper = new IdealSwitchStamper();
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("IdealSwitchStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testImplementsIStatefulStamper() {
        assertTrue("IdealSwitchStamper should implement IStatefulStamper",
                stamper instanceof IStatefulStamper);
    }

    @Test
    public void testDefaultConstructor_InitiallyOff() {
        assertFalse("switch should be OFF initially", stamper.isOn());
    }

    @Test
    public void testDefaultConstructor_DefaultParameters() {
        assertEquals("default rOn", IdealSwitchStamper.DEFAULT_R_ON, stamper.getROn(), TOLERANCE);
        assertEquals("default rOff", IdealSwitchStamper.DEFAULT_R_OFF, stamper.getROff(), TOLERANCE);
    }

    @Test
    public void testParameterizedConstructor() {
        IdealSwitchStamper customStamper = new IdealSwitchStamper(1e-4, 1e8);
        assertEquals("custom rOn", 1e-4, customStamper.getROn(), TOLERANCE);
        assertEquals("custom rOff", 1e8, customStamper.getROff(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_Off() {
        stamper.setState(false);
        assertEquals("OFF resistance", IdealSwitchStamper.DEFAULT_R_OFF, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_On() {
        stamper.setState(true);
        assertEquals("ON resistance", IdealSwitchStamper.DEFAULT_R_ON, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testStampMatrixA_Off() {
        double[][] a = new double[3][3];
        double[] parameter = IdealSwitchStamper.createDefaultParameters(0);
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / IdealSwitchStamper.DEFAULT_R_OFF;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
        assertEquals("a[1][2]", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1]", -expectedAdmittance, a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_On() {
        double[][] a = new double[3][3];
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);
        stamper.setState(true);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / IdealSwitchStamper.DEFAULT_R_ON;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_NoContribution() {
        double[] b = new double[3];
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);
        double[] previousValues = new double[10];
        stamper.setState(true);

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // Ideal switch has no forward voltage, so no B vector contribution
        assertEquals("b[1] should be 0", 0.0, b[1], TOLERANCE);
        assertEquals("b[2] should be 0", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testSetGateSignal_TurnOn() {
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.setGateSignal(1.0);

        assertTrue("switch should turn ON", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testSetGateSignal_TurnOff() {
        stamper.setState(true);
        stamper.resetStateChange();

        stamper.setGateSignal(0.0);

        assertFalse("switch should turn OFF", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testSetGateSignal_StayOn() {
        stamper.setState(true);
        stamper.resetStateChange();

        stamper.setGateSignal(1.0);

        assertTrue("switch should stay ON", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testSetGateSignal_StayOff() {
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.setGateSignal(0.0);

        assertFalse("switch should stay OFF", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateFromParameters() {
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);
        stamper.setState(false);
        stamper.resetStateChange();

        stamper.updateStateFromParameters(parameter);

        assertTrue("switch should turn ON from parameters", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testResetStateChange() {
        stamper.setState(false);
        stamper.setGateSignal(1.0); // Turn ON
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
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);

        // I = V / rOn = 1.0 / 1e-3 = 1000
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("ON current", 1000.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_Off() {
        stamper.setState(false);
        double[] parameter = IdealSwitchStamper.createDefaultParameters(0);

        // Leakage: I = V / rOff = 1.0 / 1e9 = 1e-9
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("OFF leakage current", 1e-9, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ReverseVoltage() {
        stamper.setState(true);
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);

        double current = stamper.calculateCurrent(0.0, 1.0, parameter, 1e-6, 0.0);
        assertTrue("reverse current should be negative", current < 0);
    }

    @Test
    public void testGetAdmittanceWeight_On() {
        stamper.setState(true);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("ON admittance", 1.0 / IdealSwitchStamper.DEFAULT_R_ON, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_Off() {
        stamper.setState(false);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("OFF admittance", 1.0 / IdealSwitchStamper.DEFAULT_R_OFF, admittance, TOLERANCE);
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
    public void testCreateParameters() {
        double[] params = IdealSwitchStamper.createParameters(1e-3, 1e-4, 1e8, 1.0);

        assertEquals("rCurrent", 1e-3, params[IdealSwitchStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("rOn", 1e-4, params[IdealSwitchStamper.PARAM_R_ON], TOLERANCE);
        assertEquals("rOff", 1e8, params[IdealSwitchStamper.PARAM_R_OFF], TOLERANCE);
        assertEquals("gate", 1.0, params[IdealSwitchStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateOn() {
        double[] params = IdealSwitchStamper.createDefaultParameters(1.0);

        assertEquals("rCurrent should be rOn when gate=1", 
                     IdealSwitchStamper.DEFAULT_R_ON, params[IdealSwitchStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("gate", 1.0, params[IdealSwitchStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateOff() {
        double[] params = IdealSwitchStamper.createDefaultParameters(0.0);

        assertEquals("rCurrent should be rOff when gate=0", 
                     IdealSwitchStamper.DEFAULT_R_OFF, params[IdealSwitchStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("gate", 0.0, params[IdealSwitchStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testParameterConstants() {
        assertEquals("PARAM_R_CURRENT", 0, IdealSwitchStamper.PARAM_R_CURRENT);
        assertEquals("PARAM_R_ON", 1, IdealSwitchStamper.PARAM_R_ON);
        assertEquals("PARAM_R_OFF", 2, IdealSwitchStamper.PARAM_R_OFF);
        assertEquals("PARAM_GATE", 8, IdealSwitchStamper.PARAM_GATE);
    }

    @Test
    public void testDefaultConstants() {
        assertEquals("DEFAULT_R_ON", 1e-3, IdealSwitchStamper.DEFAULT_R_ON, TOLERANCE);
        assertEquals("DEFAULT_R_OFF", 1e9, IdealSwitchStamper.DEFAULT_R_OFF, TOLERANCE);
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
    }

    @Test
    public void testStampMatrixA_SymmetricMatrix() {
        double[][] a = new double[3][3];
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        assertEquals("matrix should be symmetric", a[1][2], a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExisting() {
        double[][] a = new double[3][3];
        a[1][1] = 0.001;
        double[] parameter = IdealSwitchStamper.createDefaultParameters(0);
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / IdealSwitchStamper.DEFAULT_R_OFF;
        assertEquals("should add to existing", 0.001 + expectedAdmittance, a[1][1], TOLERANCE);
    }

    @Test
    public void testSwitchStateCycle() {
        // Test complete ON/OFF cycle via gate signal
        stamper.setState(false);
        assertFalse("initially OFF", stamper.isOn());

        // Turn ON via gate
        stamper.setGateSignal(1.0);
        assertTrue("turned ON", stamper.isOn());

        // Stay ON
        stamper.setGateSignal(1.0);
        assertTrue("stays ON", stamper.isOn());

        // Turn OFF via gate
        stamper.setGateSignal(0.0);
        assertFalse("turned OFF", stamper.isOn());

        // Stay OFF
        stamper.setGateSignal(0.0);
        assertFalse("stays OFF", stamper.isOn());
    }

    @Test
    public void testBidirectionalConduction() {
        // Ideal switch conducts in both directions when ON
        stamper.setState(true);
        double[] parameter = IdealSwitchStamper.createDefaultParameters(1);

        double forwardCurrent = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        double reverseCurrent = stamper.calculateCurrent(0.0, 1.0, parameter, 1e-6, 0.0);

        assertEquals("forward and reverse current magnitudes should be equal",
                     Math.abs(forwardCurrent), Math.abs(reverseCurrent), LOOSE_TOLERANCE);
        assertTrue("forward current should be positive", forwardCurrent > 0);
        assertTrue("reverse current should be negative", reverseCurrent < 0);
    }

    @Test
    public void testNullParameterArray_UsesInstanceValues() {
        stamper.setState(true);
        stamper.setROn(2e-3);

        double current = stamper.calculateCurrent(1.0, 0.0, null, 1e-6, 0.0);

        // With null params, should use instance rOn
        double expected = 1.0 / stamper.getROn();
        assertEquals("should use instance values", expected, current, LOOSE_TOLERANCE);
    }
}

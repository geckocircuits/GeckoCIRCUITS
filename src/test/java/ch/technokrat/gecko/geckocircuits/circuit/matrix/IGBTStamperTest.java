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
 * Tests for IGBTStamper - verifies correct MNA matrix stamping and state management.
 */
public class IGBTStamperTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-6;
    private IGBTStamper stamper;

    @Before
    public void setUp() {
        stamper = new IGBTStamper();
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("IGBTStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testImplementsIStatefulStamper() {
        assertTrue("IGBTStamper should implement IStatefulStamper",
                stamper instanceof IStatefulStamper);
    }

    @Test
    public void testDefaultConstructor_InitiallyOff() {
        assertFalse("IGBT should be OFF initially", stamper.isOn());
    }

    @Test
    public void testDefaultConstructor_DefaultParameters() {
        assertEquals("default rOn", IGBTStamper.DEFAULT_R_ON, stamper.getROn(), TOLERANCE);
        assertEquals("default rOff", IGBTStamper.DEFAULT_R_OFF, stamper.getROff(), TOLERANCE);
        assertEquals("default uForward", IGBTStamper.DEFAULT_U_FORWARD, stamper.getUForward(), TOLERANCE);
    }

    @Test
    public void testParameterizedConstructor() {
        IGBTStamper customStamper = new IGBTStamper(1e-4, 1e8, 1.2);
        assertEquals("custom rOn", 1e-4, customStamper.getROn(), TOLERANCE);
        assertEquals("custom rOff", 1e8, customStamper.getROff(), TOLERANCE);
        assertEquals("custom uForward", 1.2, customStamper.getUForward(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_Off() {
        stamper.setState(false);
        assertEquals("OFF resistance", IGBTStamper.DEFAULT_R_OFF, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testGetCurrentResistance_On() {
        stamper.setState(true);
        assertEquals("ON resistance", IGBTStamper.DEFAULT_R_ON, stamper.getCurrentResistance(), TOLERANCE);
    }

    @Test
    public void testStampMatrixA_Off() {
        double[][] a = new double[3][3];
        double[] parameter = IGBTStamper.createDefaultParameters(0);
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / IGBTStamper.DEFAULT_R_OFF;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
        assertEquals("a[1][2]", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1]", -expectedAdmittance, a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_On() {
        double[][] a = new double[3][3];
        double[] parameter = IGBTStamper.createDefaultParameters(1);
        stamper.setState(true);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / IGBTStamper.DEFAULT_R_ON;
        assertEquals("a[1][1]", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2]", expectedAdmittance, a[2][2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_Off_NoContribution() {
        double[] b = new double[3];
        double[] parameter = IGBTStamper.createDefaultParameters(0);
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
        double[] parameter = IGBTStamper.createParameters(rOn, uForward, rOn, 1e9, 1);
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
        stamper.resetStateChange();

        // Gate ON with sufficient forward voltage
        stamper.updateStateWithGate(1.0, 2.0, 0.0); // vForward = 2V > 1.5V

        assertTrue("IGBT should turn ON", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_StayOff_GateOn_LowVoltage() {
        stamper.setState(false);
        stamper.resetStateChange();

        // Gate ON but forward voltage below threshold
        stamper.updateStateWithGate(1.0, 1.0, 0.0); // vForward = 1V < 1.5V

        assertFalse("IGBT should stay OFF with low voltage", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_StayOff_GateOff() {
        stamper.setState(false);
        stamper.resetStateChange();

        // Gate OFF, even with sufficient voltage
        stamper.updateStateWithGate(0.0, 3.0, 0.0);

        assertFalse("IGBT should stay OFF without gate", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_TurnOff_GateOff() {
        stamper.setState(true);
        stamper.resetStateChange();

        // Gate turned OFF -> IGBT turns OFF immediately
        stamper.updateStateWithGate(0.0, 3.0, 0.0);

        assertFalse("IGBT should turn OFF when gate OFF", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testUpdateStateWithGate_TurnOff_LowVoltage() {
        stamper.setState(true);
        stamper.resetStateChange();

        // Gate still ON but forward voltage dropped (current reversing)
        stamper.updateStateWithGate(1.0, 1.0, 0.0); // vForward = 1V < 1.5V

        assertFalse("IGBT should turn OFF with low voltage", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testSetGateSignal_TurnOff() {
        stamper.setState(true);
        stamper.resetStateChange();

        stamper.setGateSignal(0.0);

        assertFalse("IGBT should turn OFF", stamper.isOn());
        assertTrue("state should have changed", stamper.isStateChanged());
    }

    @Test
    public void testSetGateSignal_GateOnAloneDoesNotTurnOn() {
        stamper.setState(false);
        stamper.resetStateChange();

        // Gate ON alone doesn't turn on IGBT (needs voltage condition)
        stamper.setGateSignal(1.0);

        assertFalse("IGBT should not turn ON with gate alone", stamper.isOn());
        assertFalse("state should not have changed", stamper.isStateChanged());
    }

    @Test
    public void testResetStateChange() {
        stamper.setState(false);
        stamper.updateStateWithGate(1.0, 3.0, 0.0); // Turn ON
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
        double[] parameter = IGBTStamper.createParameters(1e-3, 1.5, 1e-3, 1e9, 1);

        // I = (V - Uf) / rOn = (3.0 - 1.5) / 1e-3 = 1500
        double current = stamper.calculateCurrent(3.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("ON current", 1500.0, current, LOOSE_TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_Off() {
        stamper.setState(false);
        double[] parameter = IGBTStamper.createDefaultParameters(0);

        // Leakage: I = V / rOff = 1.0 / 1e9 = 1e-9
        double current = stamper.calculateCurrent(1.0, 0.0, parameter, 1e-6, 0.0);
        assertEquals("OFF leakage current", 1e-9, current, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_On() {
        stamper.setState(true);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("ON admittance", 1.0 / IGBTStamper.DEFAULT_R_ON, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_Off() {
        stamper.setState(false);
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        assertEquals("OFF admittance", 1.0 / IGBTStamper.DEFAULT_R_OFF, admittance, TOLERANCE);
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
    public void testCreateParameters() {
        double[] params = IGBTStamper.createParameters(1e-3, 1.2, 1e-4, 1e8, 1.0);

        assertEquals("rCurrent", 1e-3, params[IGBTStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("uForward", 1.2, params[IGBTStamper.PARAM_U_FORWARD], TOLERANCE);
        assertEquals("rOn", 1e-4, params[IGBTStamper.PARAM_R_ON], TOLERANCE);
        assertEquals("rOff", 1e8, params[IGBTStamper.PARAM_R_OFF], TOLERANCE);
        assertEquals("gate", 1.0, params[IGBTStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateOn() {
        double[] params = IGBTStamper.createDefaultParameters(1.0);

        assertEquals("rCurrent should be rOn when gate=1", 
                     IGBTStamper.DEFAULT_R_ON, params[IGBTStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("uForward", IGBTStamper.DEFAULT_U_FORWARD, params[IGBTStamper.PARAM_U_FORWARD], TOLERANCE);
        assertEquals("gate", 1.0, params[IGBTStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testCreateDefaultParameters_GateOff() {
        double[] params = IGBTStamper.createDefaultParameters(0.0);

        assertEquals("rCurrent should be rOff when gate=0", 
                     IGBTStamper.DEFAULT_R_OFF, params[IGBTStamper.PARAM_R_CURRENT], TOLERANCE);
        assertEquals("gate", 0.0, params[IGBTStamper.PARAM_GATE], TOLERANCE);
    }

    @Test
    public void testParameterConstants() {
        assertEquals("PARAM_R_CURRENT", 0, IGBTStamper.PARAM_R_CURRENT);
        assertEquals("PARAM_U_FORWARD", 1, IGBTStamper.PARAM_U_FORWARD);
        assertEquals("PARAM_R_ON", 2, IGBTStamper.PARAM_R_ON);
        assertEquals("PARAM_R_OFF", 3, IGBTStamper.PARAM_R_OFF);
        assertEquals("PARAM_GATE", 8, IGBTStamper.PARAM_GATE);
    }

    @Test
    public void testDefaultConstants() {
        assertEquals("DEFAULT_R_ON", 1e-3, IGBTStamper.DEFAULT_R_ON, TOLERANCE);
        assertEquals("DEFAULT_R_OFF", 1e9, IGBTStamper.DEFAULT_R_OFF, TOLERANCE);
        assertEquals("DEFAULT_U_FORWARD", 1.5, IGBTStamper.DEFAULT_U_FORWARD, TOLERANCE);
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
        double[] parameter = IGBTStamper.createDefaultParameters(1);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        assertEquals("matrix should be symmetric", a[1][2], a[2][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExisting() {
        double[][] a = new double[3][3];
        a[1][1] = 0.001;
        double[] parameter = IGBTStamper.createDefaultParameters(0);
        stamper.setState(false);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / IGBTStamper.DEFAULT_R_OFF;
        assertEquals("should add to existing", 0.001 + expectedAdmittance, a[1][1], TOLERANCE);
    }

    @Test
    public void testIGBTStateCycle() {
        // Test complete ON/OFF cycle
        stamper.setState(false);
        assertFalse("initially OFF", stamper.isOn());

        // Turn ON: Gate + sufficient voltage
        stamper.updateStateWithGate(1.0, 3.0, 0.0);
        assertTrue("turned ON", stamper.isOn());

        // Stay ON with gate and voltage
        stamper.updateStateWithGate(1.0, 3.0, 0.0);
        assertTrue("stays ON", stamper.isOn());

        // Turn OFF via gate
        stamper.updateStateWithGate(0.0, 3.0, 0.0);
        assertFalse("turned OFF via gate", stamper.isOn());

        // Try to turn ON again
        stamper.updateStateWithGate(1.0, 3.0, 0.0);
        assertTrue("turned ON again", stamper.isOn());

        // Turn OFF via low voltage (current reversal)
        stamper.updateStateWithGate(1.0, 0.5, 0.0);
        assertFalse("turned OFF via low voltage", stamper.isOn());
    }

    @Test
    public void testIGBTvsDiode_SimilarBVector() {
        // IGBT and Diode should have similar B vector stamping when ON
        IGBTStamper igbt = new IGBTStamper(1e-3, 1e9, 0.7);
        DiodeStamper diode = new DiodeStamper(1e-3, 1e9, 0.7);

        igbt.setState(true);
        diode.setState(true);

        double[] igbtParams = IGBTStamper.createParameters(1e-3, 0.7, 1e-3, 1e9, 1);
        double[] diodeParams = DiodeStamper.createParameters(1e-3, 1e9, 0.7);

        double[] igbtB = new double[3];
        double[] diodeB = new double[3];

        igbt.stampVectorB(igbtB, 1, 2, 0, igbtParams, 1e-6, 0.0, new double[10]);
        diode.stampVectorB(diodeB, 1, 2, 0, diodeParams, 1e-6, 0.0, new double[10]);

        assertEquals("B vector contribution should be similar",
                     diodeB[1], igbtB[1], LOOSE_TOLERANCE);
    }

    @Test
    public void testUnidirectionalConduction() {
        // IGBT only conducts in forward direction
        stamper.setState(true);
        double[] parameter = IGBTStamper.createParameters(1e-3, 1.5, 1e-3, 1e9, 1);

        // Forward current with sufficient voltage
        double forwardCurrent = stamper.calculateCurrent(3.0, 0.0, parameter, 1e-6, 0.0);
        assertTrue("forward current should be positive", forwardCurrent > 0);

        // With reverse voltage, IGBT should turn off (handled by updateState)
        // Here we just verify current calculation
        double reverseCurrent = stamper.calculateCurrent(0.0, 3.0, parameter, 1e-6, 0.0);
        assertTrue("reverse calculation gives negative value", reverseCurrent < 0);
        // Note: In real operation, IGBT would turn OFF before conducting reverse
    }
}

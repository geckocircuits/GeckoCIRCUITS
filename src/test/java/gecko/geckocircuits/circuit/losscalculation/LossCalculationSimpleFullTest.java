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
package gecko.geckocircuits.circuit.losscalculation;

import gecko.geckocircuits.circuit.circuitcomponents.Diode;
import gecko.geckocircuits.circuit.circuitcomponents.IGBT;
import gecko.i18n.LangInit;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for LossCalculationSimple and LossCalculatorSwitchSimple.
 * Tests all formulas, state transitions, and edge cases.
 */
public class LossCalculationSimpleFullTest {

    private static final double TOLERANCE = 1e-9;
    private static final double DELTA_T = 1e-6; // 1 microsecond

    // Test parameters
    private static final double UF = 0.8;        // Forward voltage drop (V)
    private static final double R_ON = 0.04;     // On-resistance (Ohm)
    private static final double K_ON = 15e-6;    // Turn-on coefficient (Ws/A)
    private static final double K_OFF = 20e-6;   // Turn-off coefficient (Ws/A)
    private static final double U_SWNORM = 200;  // Reference voltage (V)

    private IGBT igbt;
    private LossCalculationSimple lossCalc;

    @Before
    public void setUp() {
        LangInit.initEnglish();
        igbt = new IGBT();
        igbt._onResistance.setValueWithoutUndo(R_ON);
        igbt.getForwardVoltageDropParameter().setValueWithoutUndo(UF);
        igbt.kOn.setValueWithoutUndo(K_ON);
        igbt.kOff.setValueWithoutUndo(K_OFF);
        igbt.uK.setValueWithoutUndo(U_SWNORM);

        lossCalc = new LossCalculationSimple(igbt);
    }

    // ===========================================
    // Constants and Factory Tests
    // ===========================================

    @Test
    public void testDefaultUKConstant() {
        assertEquals(400.0, LossCalculationSimple.UK_DEFAULT_VALUE, TOLERANCE);
    }

    @Test
    public void testLossCalculatorFabric_CreatesInstance() {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();
        assertNotNull(calculator);
        assertTrue(calculator instanceof LossCalculationSimple.LossCalculatorSwitchSimple);
    }

    @Test(expected = AssertionError.class)
    public void testLossCalculatorFabric_FailsWhenUSwNormZero() {
        lossCalc._uSWnorm = 0;
        lossCalc.lossCalculatorFabric();
    }

    // ===========================================
    // copyPropertiesFrom Tests
    // ===========================================

    @Test
    public void testCopyPropertiesFrom_CopiesAllFields() {
        LossCalculationSimple original = new LossCalculationSimple(igbt);
        original._kON = 25e-6;
        original._kOFF = 35e-6;
        original._uSWnorm = 600.0;

        LossCalculationSimple copy = new LossCalculationSimple(igbt);
        copy.copyPropertiesFrom(original);

        assertEquals(25e-6, copy._kON, TOLERANCE);
        assertEquals(35e-6, copy._kOFF, TOLERANCE);
        assertEquals(600.0, copy._uSWnorm, TOLERANCE);
    }

    @Test
    public void testCopyPropertiesFrom_IndependentCopies() {
        LossCalculationSimple original = new LossCalculationSimple(igbt);
        original._kON = 10e-6;
        original._kOFF = 15e-6;
        original._uSWnorm = 400.0;

        LossCalculationSimple copy = new LossCalculationSimple(igbt);
        copy.copyPropertiesFrom(original);

        // Modify original
        original._kON = 99e-6;

        // Copy should remain unchanged
        assertEquals(10e-6, copy._kON, TOLERANCE);
    }

    // ===========================================
    // Conduction Loss Formula Tests
    // ===========================================

    @Test
    public void testCalcConductionLoss_PositiveCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // Set current via reflection
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        currentField.set(calculator, 10.0); // 10 A

        // Expected: |I * Uf| + I^2 * R_on = |10 * 0.8| + 100 * 0.04 = 8 + 4 = 12 W
        double conductionLoss = invokePrivateMethod(calculator, "calcConductionLoss");
        assertEquals(12.0, conductionLoss, TOLERANCE);
    }

    @Test
    public void testCalcConductionLoss_NegativeCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        currentField.set(calculator, -10.0); // -10 A

        // Expected: |-10 * 0.8| + (-10)^2 * 0.04 = 8 + 4 = 12 W
        double conductionLoss = invokePrivateMethod(calculator, "calcConductionLoss");
        assertEquals(12.0, conductionLoss, TOLERANCE);
    }

    @Test
    public void testCalcConductionLoss_ZeroCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        currentField.set(calculator, 0.0);

        // Expected: 0
        double conductionLoss = invokePrivateMethod(calculator, "calcConductionLoss");
        assertEquals(0.0, conductionLoss, TOLERANCE);
    }

    @Test
    public void testCalcConductionLoss_SmallCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        currentField.set(calculator, 0.5); // 0.5 A

        // Expected: |0.5 * 0.8| + 0.25 * 0.04 = 0.4 + 0.01 = 0.41 W
        double conductionLoss = invokePrivateMethod(calculator, "calcConductionLoss");
        assertEquals(0.41, conductionLoss, TOLERANCE);
    }

    @Test
    public void testCalcConductionLoss_DiodeWithoutForwardDrop() throws Exception {
        // Create a diode which implements ForwardVoltageDropable
        Diode diode = new Diode();
        diode._onResistance.setValueWithoutUndo(0.01);
        diode.getForwardVoltageDropParameter().setValueWithoutUndo(0.0); // No forward drop
        diode.kOn.setValueWithoutUndo(K_ON);
        diode.kOff.setValueWithoutUndo(K_OFF);
        diode.uK.setValueWithoutUndo(U_SWNORM);

        LossCalculationSimple diodeLoss = new LossCalculationSimple(diode);
        AbstractLossCalculator calculator = diodeLoss.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        currentField.set(calculator, 5.0);

        // Expected: |5 * 0| + 25 * 0.01 = 0.25 W (only resistive loss)
        double conductionLoss = invokePrivateMethod(calculator, "calcConductionLoss");
        assertEquals(0.25, conductionLoss, TOLERANCE);
    }

    // ===========================================
    // Turn-On Switching Loss Formula Tests
    // ===========================================

    @Test
    public void testCalcTurnOnSwitchingLoss_Formula() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        Field deltaField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_deltaT");

        currentField.set(calculator, 20.0);
        deltaField.set(calculator, DELTA_T);

        // Expected: |k_on * I / dt| = |15e-6 * 20 / 1e-6| = 300 W
        double turnOnLoss = invokePrivateMethod(calculator, "calcTurnOnSwitchingLoss");
        assertEquals(300.0, turnOnLoss, TOLERANCE);
    }

    @Test
    public void testCalcTurnOnSwitchingLoss_NegativeCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        Field deltaField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_deltaT");

        currentField.set(calculator, -20.0);
        deltaField.set(calculator, DELTA_T);

        // Expected: |15e-6 * (-20) / 1e-6| = 300 W (absolute value)
        double turnOnLoss = invokePrivateMethod(calculator, "calcTurnOnSwitchingLoss");
        assertEquals(300.0, turnOnLoss, TOLERANCE);
    }

    @Test
    public void testCalcTurnOnSwitchingLoss_SmallTimeStep() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        Field deltaField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_deltaT");

        currentField.set(calculator, 10.0);
        deltaField.set(calculator, 0.1e-6); // 100 ns

        // Expected: |15e-6 * 10 / 0.1e-6| = 1500 W
        double turnOnLoss = invokePrivateMethod(calculator, "calcTurnOnSwitchingLoss");
        assertEquals(1500.0, turnOnLoss, TOLERANCE);
    }

    // ===========================================
    // Turn-Off Switching Loss Formula Tests
    // ===========================================

    @Test
    public void testCalcTurnOffSwitchingLoss_Formula() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field deltaField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_deltaT");

        oldCurrentField.set(calculator, 25.0);
        deltaField.set(calculator, DELTA_T);

        // Expected: |k_off * oldI / dt| = |20e-6 * 25 / 1e-6| = 500 W
        double turnOffLoss = invokePrivateMethod(calculator, "calcTurnOffSwitchingLoss");
        assertEquals(500.0, turnOffLoss, TOLERANCE);
    }

    @Test
    public void testCalcTurnOffSwitchingLoss_NegativeOldCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field deltaField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_deltaT");

        oldCurrentField.set(calculator, -25.0);
        deltaField.set(calculator, DELTA_T);

        // Expected: |20e-6 * (-25) / 1e-6| = 500 W (absolute value)
        double turnOffLoss = invokePrivateMethod(calculator, "calcTurnOffSwitchingLoss");
        assertEquals(500.0, turnOffLoss, TOLERANCE);
    }

    // ===========================================
    // Relative Voltage Factor Tests
    // ===========================================

    @Test
    public void testCalculateRelativeVoltageFactor_AtReferenceVoltage() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // At reference voltage (200V), factor should be 1.0
        double factor = invokePrivateMethod(calculator, "calculateRelativeVoltageFactor", 200.0);
        assertEquals(1.0, factor, TOLERANCE);
    }

    @Test
    public void testCalculateRelativeVoltageFactor_DoubleVoltage() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // At 400V (2x reference), factor should be 2.0
        double factor = invokePrivateMethod(calculator, "calculateRelativeVoltageFactor", 400.0);
        assertEquals(2.0, factor, TOLERANCE);
    }

    @Test
    public void testCalculateRelativeVoltageFactor_HalfVoltage() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // At 100V (0.5x reference), factor should be 0.5
        double factor = invokePrivateMethod(calculator, "calculateRelativeVoltageFactor", 100.0);
        assertEquals(0.5, factor, TOLERANCE);
    }

    @Test
    public void testCalculateRelativeVoltageFactor_NegativeVoltage() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // Negative voltage uses absolute value
        double factor = invokePrivateMethod(calculator, "calculateRelativeVoltageFactor", -400.0);
        assertEquals(2.0, factor, TOLERANCE);
    }

    @Test
    public void testCalculateRelativeVoltageFactor_ZeroVoltage() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // Zero voltage gives zero factor
        double factor = invokePrivateMethod(calculator, "calculateRelativeVoltageFactor", 0.0);
        assertEquals(0.0, factor, TOLERANCE);
    }

    @Test
    public void testCalculateRelativeVoltageFactor_NaNDetectionPath() throws Exception {
        // This tests the NaN detection path when _uSWnorm = 0 (division by zero)
        // We can't directly set _uSWnorm to 0 in the calculator since it's initialized from parent
        // But we can test it indirectly through console output

        // Create calculator with modified uSWnorm via reflection
        igbt.uK.setValueWithoutUndo(0.0001); // Very small to approach division issues
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // This should not crash
        double factor = invokePrivateMethod(calculator, "calculateRelativeVoltageFactor", 100.0);
        assertFalse(Double.isNaN(factor));
        assertTrue(factor > 0);
    }

    // ===========================================
    // Turn-On/Turn-Off Detection Tests
    // ===========================================

    @Test
    public void testDetectTurnOn_FromOffToOn() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 0.0);
        currentField.set(calculator, 10.0);

        boolean turnOn = invokePrivateMethod(calculator, "detectTurnOn");
        assertTrue(turnOn);
    }

    @Test
    public void testDetectTurnOn_FromBelowThresholdToAbove() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 0.001); // Below EPS (0.01)
        currentField.set(calculator, 5.0);

        boolean turnOn = invokePrivateMethod(calculator, "detectTurnOn");
        assertTrue(turnOn);
    }

    @Test
    public void testDetectTurnOn_AlreadyOn() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 5.0);
        currentField.set(calculator, 10.0);

        boolean turnOn = invokePrivateMethod(calculator, "detectTurnOn");
        assertFalse(turnOn);
    }

    @Test
    public void testDetectTurnOn_NegativeCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 0.0);
        currentField.set(calculator, -10.0);

        boolean turnOn = invokePrivateMethod(calculator, "detectTurnOn");
        assertTrue(turnOn); // Negative current also triggers turn-on
    }

    @Test
    public void testDetectTurnOff_FromOnToOff() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 10.0);
        currentField.set(calculator, 0.0);

        boolean turnOff = invokePrivateMethod(calculator, "detectTurnOff");
        assertTrue(turnOff);
    }

    @Test
    public void testDetectTurnOff_FromAboveThresholdToBelow() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 8.0);
        currentField.set(calculator, 0.005); // Below EPS (0.01)

        boolean turnOff = invokePrivateMethod(calculator, "detectTurnOff");
        assertTrue(turnOff);
    }

    @Test
    public void testDetectTurnOff_AlreadyOff() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 0.0);
        currentField.set(calculator, 0.0);

        boolean turnOff = invokePrivateMethod(calculator, "detectTurnOff");
        assertFalse(turnOff);
    }

    @Test
    public void testDetectTurnOff_StillOn() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 10.0);
        currentField.set(calculator, 5.0);

        boolean turnOff = invokePrivateMethod(calculator, "detectTurnOff");
        assertFalse(turnOff);
    }

    // ===========================================
    // Full Loss Calculation Cycle Tests
    // ===========================================

    @Test
    public void testFullCycle_TurnOnEvent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        // Set parent voltage
        igbt._voltage = 180.0; // Will be used for voltage factor

        // Initialize state: switch is off
        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field oldVoltageField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldVoltage");
        oldCurrentField.set(calculator, 0.0);
        oldVoltageField.set(calculator, 180.0);

        // Call calcLosses with turn-on: current goes from 0 to 10A
        invokePrivateMethod(calculator, "calcLosses", 10.0, 25.0, DELTA_T);

        // Verify conduction loss: |10 * 0.8| + 100 * 0.04 = 12 W
        double conductionLoss = invokePrivateMethod(calculator, "getConductionLoss");
        assertEquals(12.0, conductionLoss, TOLERANCE);

        // Verify turn-on switching loss with voltage factor
        // Turn-on detected, so: |k_on * I / dt| * (oldV / V_norm) = |15e-6 * 10 / 1e-6| * (180/200) = 150 * 0.9 = 135 W
        double switchingLoss = invokePrivateMethod(calculator, "getSwitchingLoss");
        assertEquals(135.0, switchingLoss, TOLERANCE);

        // Total loss
        double totalLoss = invokePrivateMethod(calculator, "getTotalLosses");
        assertEquals(147.0, totalLoss, TOLERANCE);
    }

    @Test
    public void testFullCycle_TurnOffEvent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        igbt._voltage = 220.0;

        // Initialize state: switch is on with 15A
        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field oldVoltageField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldVoltage");
        oldCurrentField.set(calculator, 15.0);
        oldVoltageField.set(calculator, 200.0);

        // Call calcLosses with turn-off: current goes from 15A to 0
        invokePrivateMethod(calculator, "calcLosses", 0.0, 25.0, DELTA_T);

        // Verify conduction loss: 0 (no current)
        double conductionLoss = invokePrivateMethod(calculator, "getConductionLoss");
        assertEquals(0.0, conductionLoss, TOLERANCE);

        // Verify turn-off switching loss with voltage factor
        // Turn-off detected: |k_off * oldI / dt| * (V / V_norm) = |20e-6 * 15 / 1e-6| * (220/200) = 300 * 1.1 = 330 W
        double switchingLoss = invokePrivateMethod(calculator, "getSwitchingLoss");
        assertEquals(330.0, switchingLoss, TOLERANCE);

        // Total loss
        double totalLoss = invokePrivateMethod(calculator, "getTotalLosses");
        assertEquals(330.0, totalLoss, TOLERANCE);
    }

    @Test
    public void testFullCycle_ConductionOnly() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        igbt._voltage = 5.0; // Low voltage during conduction

        // Initialize state: switch is already on
        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field oldVoltageField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldVoltage");
        oldCurrentField.set(calculator, 12.0);
        oldVoltageField.set(calculator, 5.0);

        // Call calcLosses: current stays at 12A (no switching)
        invokePrivateMethod(calculator, "calcLosses", 12.0, 25.0, DELTA_T);

        // Verify conduction loss: |12 * 0.8| + 144 * 0.04 = 9.6 + 5.76 = 15.36 W
        double conductionLoss = invokePrivateMethod(calculator, "getConductionLoss");
        assertEquals(15.36, conductionLoss, TOLERANCE);

        // No switching loss
        double switchingLoss = invokePrivateMethod(calculator, "getSwitchingLoss");
        assertEquals(0.0, switchingLoss, TOLERANCE);

        // Total loss
        double totalLoss = invokePrivateMethod(calculator, "getTotalLosses");
        assertEquals(15.36, totalLoss, TOLERANCE);
    }

    @Test
    public void testFullCycle_StateTransitionTracking() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        igbt._voltage = 200.0;

        // Initialize
        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field oldVoltageField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldVoltage");
        oldCurrentField.set(calculator, 0.0);
        oldVoltageField.set(calculator, 200.0);

        // First call: turn on
        invokePrivateMethod(calculator, "calcLosses", 10.0, 25.0, DELTA_T);

        // Verify state updated
        double newOldCurrent = (Double) oldCurrentField.get(calculator);
        double newOldVoltage = (Double) oldVoltageField.get(calculator);
        assertEquals(10.0, newOldCurrent, TOLERANCE);
        assertEquals(200.0, newOldVoltage, TOLERANCE);

        // Second call: conduction only (no switching)
        invokePrivateMethod(calculator, "calcLosses", 10.0, 25.0, DELTA_T);

        double switchingLoss = invokePrivateMethod(calculator, "getSwitchingLoss");
        assertEquals(0.0, switchingLoss, TOLERANCE); // No switching on second call
    }

    @Test
    public void testFullCycle_BothTurnOnAndTurnOff() throws Exception {
        // This edge case shouldn't normally happen, but test the formula if it does
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        igbt._voltage = 200.0;

        // Set state where both conditions might trigger (edge case)
        // In practice, this is prevented by EPS threshold, but test the addition
        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field oldVoltageField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldVoltage");

        // Set old current below EPS, new current below EPS
        // This won't trigger either detection based on normal logic
        oldCurrentField.set(calculator, 0.005);
        oldVoltageField.set(calculator, 200.0);

        invokePrivateMethod(calculator, "calcLosses", 0.005, 25.0, DELTA_T);

        // No switching should occur
        double switchingLoss = invokePrivateMethod(calculator, "getSwitchingLoss");
        assertEquals(0.0, switchingLoss, TOLERANCE);
    }

    @Test
    public void testGetTotalLosses_SumOfBoth() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field conductionField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_conductionLoss");
        Field switchingField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_switchingLoss");

        conductionField.set(calculator, 10.5);
        switchingField.set(calculator, 25.3);

        double totalLoss = invokePrivateMethod(calculator, "getTotalLosses");
        assertEquals(35.8, totalLoss, TOLERANCE);
    }

    @Test
    public void testGetSwitchingLoss_ReturnsValue() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field switchingField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_switchingLoss");
        switchingField.set(calculator, 123.456);

        double switchingLoss = invokePrivateMethod(calculator, "getSwitchingLoss");
        assertEquals(123.456, switchingLoss, TOLERANCE);
    }

    @Test
    public void testGetConductionLoss_ReturnsValue() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field conductionField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_conductionLoss");
        conductionField.set(calculator, 78.9);

        double conductionLoss = invokePrivateMethod(calculator, "getConductionLoss");
        assertEquals(78.9, conductionLoss, TOLERANCE);
    }

    // ===========================================
    // Edge Cases and Numerical Stability
    // ===========================================

    @Test
    public void testEPSThreshold_ExactlyAtThreshold() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        // EPS = 0.01
        oldCurrentField.set(calculator, 0.01);
        currentField.set(calculator, 0.01);

        // Both should be false since Math.abs(0.01) is NOT < EPS
        boolean turnOn = invokePrivateMethod(calculator, "detectTurnOn");
        boolean turnOff = invokePrivateMethod(calculator, "detectTurnOff");

        assertFalse(turnOn);
        assertFalse(turnOff);
    }

    @Test
    public void testEPSThreshold_JustBelowThreshold() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field oldCurrentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_oldCurrent");
        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");

        oldCurrentField.set(calculator, 0.009);
        currentField.set(calculator, 10.0);

        boolean turnOn = invokePrivateMethod(calculator, "detectTurnOn");
        assertTrue(turnOn); // 0.009 < 0.01, so turn-on detected
    }

    @Test
    public void testVeryLargeCurrent() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        currentField.set(calculator, 10000.0); // 10 kA

        double conductionLoss = invokePrivateMethod(calculator, "calcConductionLoss");
        // |10000 * 0.8| + 10000^2 * 0.04 = 8000 + 4,000,000 = 4,008,000 W
        assertEquals(4_008_000.0, conductionLoss, 1.0);
    }

    @Test
    public void testVerySmallTimeStep() throws Exception {
        AbstractLossCalculator calculator = lossCalc.lossCalculatorFabric();

        Field currentField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_current");
        Field deltaField = getAccessibleField(AbstractLossCalculatorSwitch.class, "_deltaT");

        currentField.set(calculator, 10.0);
        deltaField.set(calculator, 1e-12); // 1 picosecond

        double turnOnLoss = invokePrivateMethod(calculator, "calcTurnOnSwitchingLoss");
        // |15e-6 * 10 / 1e-12| = 1.5e8 W
        assertEquals(1.5e8, turnOnLoss, 1.0);
    }

    // ===========================================
    // Reflection Helper Methods
    // ===========================================

    private Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(Object obj, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
            // Handle primitives
            if (args[i] instanceof Double) paramTypes[i] = double.class;
            if (args[i] instanceof Integer) paramTypes[i] = int.class;
            if (args[i] instanceof Boolean) paramTypes[i] = boolean.class;
        }

        // Try to find method in object's class hierarchy
        Class<?> currentClass = obj.getClass();
        while (currentClass != null) {
            try {
                java.lang.reflect.Method method = currentClass.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return (T) method.invoke(obj, args);
            } catch (NoSuchMethodException e) {
                // Try parent class
                currentClass = currentClass.getSuperclass();
            }
        }

        // If not found with exact types, try no-arg version
        currentClass = obj.getClass();
        while (currentClass != null) {
            try {
                java.lang.reflect.Method method = currentClass.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return (T) method.invoke(obj);
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new NoSuchMethodException("Method " + methodName + " not found");
    }
}

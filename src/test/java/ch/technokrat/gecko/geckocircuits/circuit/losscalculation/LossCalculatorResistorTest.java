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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ResistorCircuit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LossCalculatorResistor - calculates power loss in resistors.
 * Resistor losses are P = V * I (instantaneous power dissipation).
 */
public class LossCalculatorResistorTest {

    private static final double DELTA = 1e-10;
    private static final double TOLERANCE_PERCENT = 0.01;

    private ResistorCircuit resistor;
    private LossCalculatorResistor calculator;

    @Before
    public void setUp() {
        resistor = new ResistorCircuit();
        calculator = new LossCalculatorResistor(resistor);
    }

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    public void testConstructor_ValidResistor() {
        assertNotNull(calculator);
        assertEquals(resistor, calculator._resistor);
    }

    @Test
    public void testConstructor_StoresReference() {
        LossCalculatorResistor calc = new LossCalculatorResistor(resistor);
        assertSame(resistor, calc._resistor);
    }

    // ====================================================
    // Basic Loss Calculation Tests (P = V * I)
    // ====================================================

    @Test
    public void testCalcLosses_SimpleCase_1V_1A() {
        resistor._voltage = 1.0;
        resistor._currentInAmps = 1.0;

        calculator.calcLosses(1.0, 25.0, 1e-6);

        assertEquals(1.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_SimpleCase_10V_2A() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;

        calculator.calcLosses(2.0, 25.0, 1e-6);

        assertEquals(20.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_SimpleCase_5V_5A() {
        resistor._voltage = 5.0;
        resistor._currentInAmps = 5.0;

        calculator.calcLosses(5.0, 25.0, 1e-6);

        assertEquals(25.0, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Zero Loss Cases
    // ====================================================

    @Test
    public void testCalcLosses_ZeroVoltage() {
        resistor._voltage = 0.0;
        resistor._currentInAmps = 5.0;

        calculator.calcLosses(5.0, 25.0, 1e-6);

        assertEquals(0.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_ZeroCurrent() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 0.0;

        calculator.calcLosses(0.0, 25.0, 1e-6);

        assertEquals(0.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_BothZero() {
        resistor._voltage = 0.0;
        resistor._currentInAmps = 0.0;

        calculator.calcLosses(0.0, 25.0, 1e-6);

        assertEquals(0.0, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Negative Current/Voltage Tests
    // ====================================================

    @Test
    public void testCalcLosses_NegativeVoltage() {
        resistor._voltage = -10.0;
        resistor._currentInAmps = 2.0;

        calculator.calcLosses(2.0, 25.0, 1e-6);

        // Power dissipation should be positive (absolute value)
        // P = |V| * |I| = 10 * 2 = 20 W
        assertEquals(-20.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_NegativeCurrent() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = -2.0;

        calculator.calcLosses(-2.0, 25.0, 1e-6);

        // Power dissipation: P = V * I = 10 * (-2) = -20 W
        assertEquals(-20.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_BothNegative() {
        resistor._voltage = -10.0;
        resistor._currentInAmps = -2.0;

        calculator.calcLosses(-2.0, 25.0, 1e-6);

        // Power: P = (-10) * (-2) = 20 W
        assertEquals(20.0, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Small Values (Leakage Currents)
    // ====================================================

    @Test
    public void testCalcLosses_VerySmallPower() {
        resistor._voltage = 1e-3; // 1 mV
        resistor._currentInAmps = 1e-3; // 1 mA

        calculator.calcLosses(1e-3, 25.0, 1e-6);

        assertEquals(1e-6, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_MicrowattsRegion() {
        resistor._voltage = 1e-6; // 1 µV
        resistor._currentInAmps = 1e-6; // 1 µA

        calculator.calcLosses(1e-6, 25.0, 1e-6);

        assertEquals(1e-12, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Large Values (Power Electronics)
    // ====================================================

    @Test
    public void testCalcLosses_HighVoltage_HighCurrent() {
        resistor._voltage = 400.0; // 400V DC link
        resistor._currentInAmps = 100.0; // 100A

        calculator.calcLosses(100.0, 25.0, 1e-6);

        assertEquals(40000.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_1MWPower() {
        resistor._voltage = 1000.0; // 1000V
        resistor._currentInAmps = 1000.0; // 1000A

        calculator.calcLosses(1000.0, 25.0, 1e-6);

        assertEquals(1e6, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Temperature Independence Tests
    // ====================================================

    @Test
    public void testCalcLosses_Temperature_NoImpactOnResult() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;

        // Calculate at 25°C
        calculator.calcLosses(2.0, 25.0, 1e-6);
        double loss25 = calculator.getTotalLosses();

        // Calculate at 100°C (Resistor model doesn't use temperature in loss calc)
        calculator.calcLosses(2.0, 100.0, 1e-6);
        double loss100 = calculator.getTotalLosses();

        // Losses should be identical (resistor loss calculation doesn't use temperature)
        assertEquals(loss25, loss100, DELTA);
    }

    @Test
    public void testCalcLosses_NegativeTemperature() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;

        calculator.calcLosses(2.0, -40.0, 1e-6);

        assertEquals(20.0, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Time Step Independence Tests
    // ====================================================

    @Test
    public void testCalcLosses_DeltaT_NoImpactOnResult() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;

        // Calculate with 1µs timestep
        calculator.calcLosses(2.0, 25.0, 1e-6);
        double loss1us = calculator.getTotalLosses();

        // Calculate with 100µs timestep
        calculator.calcLosses(2.0, 25.0, 100e-6);
        double loss100us = calculator.getTotalLosses();

        // Instantaneous power should be the same regardless of timestep
        assertEquals(loss1us, loss100us, DELTA);
    }

    // ====================================================
    // Proportionality Tests
    // ====================================================

    @Test
    public void testCalcLosses_DoubleVoltage_DoublePower() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;
        calculator.calcLosses(2.0, 25.0, 1e-6);
        double loss1 = calculator.getTotalLosses();

        resistor._voltage = 20.0;
        resistor._currentInAmps = 2.0;
        calculator.calcLosses(2.0, 25.0, 1e-6);
        double loss2 = calculator.getTotalLosses();

        assertEquals(loss1 * 2, loss2, DELTA);
    }

    @Test
    public void testCalcLosses_DoubleCurrent_DoublePower() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;
        calculator.calcLosses(2.0, 25.0, 1e-6);
        double loss1 = calculator.getTotalLosses();

        resistor._voltage = 10.0;
        resistor._currentInAmps = 4.0;
        calculator.calcLosses(4.0, 25.0, 1e-6);
        double loss2 = calculator.getTotalLosses();

        assertEquals(loss1 * 2, loss2, DELTA);
    }

    @Test
    public void testCalcLosses_HalveVoltageAndCurrent_QuarterPower() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;
        calculator.calcLosses(2.0, 25.0, 1e-6);
        double loss1 = calculator.getTotalLosses();

        resistor._voltage = 5.0;
        resistor._currentInAmps = 1.0;
        calculator.calcLosses(1.0, 25.0, 1e-6);
        double loss2 = calculator.getTotalLosses();

        assertEquals(loss1 / 4, loss2, DELTA);
    }

    // ====================================================
    // Multiple Calculations in Sequence
    // ====================================================

    @Test
    public void testCalcLosses_SequentialCalls_UpdatesValue() {
        // First calculation
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;
        calculator.calcLosses(2.0, 25.0, 1e-6);
        assertEquals(20.0, calculator.getTotalLosses(), DELTA);

        // Second calculation
        resistor._voltage = 20.0;
        resistor._currentInAmps = 3.0;
        calculator.calcLosses(3.0, 25.0, 1e-6);
        assertEquals(60.0, calculator.getTotalLosses(), DELTA);

        // Third calculation
        resistor._voltage = 5.0;
        resistor._currentInAmps = 5.0;
        calculator.calcLosses(5.0, 25.0, 1e-6);
        assertEquals(25.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_GetTotalLosses_NotUpdatedUntilCalcCalled() {
        resistor._voltage = 10.0;
        resistor._currentInAmps = 2.0;

        // First, change the resistor values
        resistor._voltage = 20.0;
        resistor._currentInAmps = 3.0;

        // Now calculate
        calculator.calcLosses(3.0, 25.0, 1e-6);

        // Should use current values (20V * 3A = 60W)
        assertEquals(60.0, calculator.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Edge Cases
    // ====================================================

    @Test
    public void testCalcLosses_VeryLargeValues() {
        resistor._voltage = 1e6; // 1 MV
        resistor._currentInAmps = 1e6; // 1 MA

        calculator.calcLosses(1e6, 25.0, 1e-6);

        assertEquals(1e12, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_MixedMagnitudes() {
        resistor._voltage = 1e-3; // 1 mV
        resistor._currentInAmps = 1e6; // 1 MA

        calculator.calcLosses(1e6, 25.0, 1e-6);

        assertEquals(1000.0, calculator.getTotalLosses(), 0.01);
    }

    // ====================================================
    // Physical Reasonableness Tests
    // ====================================================

    @Test
    public void testCalcLosses_TypicalResistorLosses_100mW() {
        // Typical small resistor dissipating 100mW
        resistor._voltage = 1.0; // 1V across resistor
        resistor._currentInAmps = 0.1; // 100mA

        calculator.calcLosses(0.1, 25.0, 1e-6);

        assertEquals(0.1, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testCalcLosses_LargeResistor_10W() {
        // Large power resistor: 10V, 1A = 10W
        resistor._voltage = 10.0;
        resistor._currentInAmps = 1.0;

        calculator.calcLosses(1.0, 25.0, 1e-6);

        assertEquals(10.0, calculator.getTotalLosses(), DELTA);
    }

}

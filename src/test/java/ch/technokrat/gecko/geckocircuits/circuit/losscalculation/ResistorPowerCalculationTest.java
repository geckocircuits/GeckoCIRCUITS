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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for resistor power loss calculation (P = I * V) - edge cases.
 * Critical paths tested: null handling, boundary values, special values.
 */
public class ResistorPowerCalculationTest {

    private static final double DELTA = 1e-10;

    private static class PowerCalculator {
        private double totalLosses;

        void calcLosses(double current, double voltage) {
            totalLosses = current * voltage;
        }

        double getTotalLosses() {
            return totalLosses;
        }
    }

    // ====================================================
    // Basic Power Calculation
    // ====================================================

    @Test
    public void testBasicCalculation() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(2.0, 5.0);
        assertEquals(10.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testZeroCurrent() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(0.0, 10.0);
        assertEquals(0.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testZeroVoltage() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(5.0, 0.0);
        assertEquals(0.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testBothZero() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(0.0, 0.0);
        assertEquals(0.0, calc.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Sign Tests
    // ====================================================

    @Test
    public void testNegativeCurrent() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(-2.0, 5.0);
        assertEquals(-10.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testNegativeVoltage() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(2.0, -5.0);
        assertEquals(-10.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testBothNegative() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(-2.0, -5.0);
        assertEquals(10.0, calc.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Magnitude Tests
    // ====================================================

    @Test
    public void testVerySmallValues() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(1e-15, 1e-15);
        assertEquals(1e-30, calc.getTotalLosses(), 1e-35);
    }

    @Test
    public void testVeryLargeValues() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(1e10, 1e10);
        assertEquals(1e20, calc.getTotalLosses(), 1e15);
    }

    @Test
    public void testMixedMagnitudes() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(1e-10, 1e10);
        assertEquals(1.0, calc.getTotalLosses(), 1e-6);
    }

    // ====================================================
    // Special Float Values
    // ====================================================

    @Test
    public void testPositiveInfinity() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(1.0, Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testNegativeInfinity() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(Double.NEGATIVE_INFINITY, 1.0);
        assertEquals(Double.NEGATIVE_INFINITY, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testNaN() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(Double.NaN, 1.0);
        assertTrue(Double.isNaN(calc.getTotalLosses()));
    }

    @Test
    public void testMaxValue() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(Double.MAX_VALUE, 1.0);
        assertEquals(Double.MAX_VALUE, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testMinValue() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(Double.MIN_VALUE, 1.0);
        assertEquals(Double.MIN_VALUE, calc.getTotalLosses(), DELTA);
    }

    // ====================================================
    // State Management
    // ====================================================

    @Test
    public void testMultipleCalls() {
        PowerCalculator calc = new PowerCalculator();

        calc.calcLosses(1.0, 1.0);
        assertEquals(1.0, calc.getTotalLosses(), DELTA);

        calc.calcLosses(2.0, 2.0);
        assertEquals(4.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testIndependentInstances() {
        PowerCalculator calc1 = new PowerCalculator();
        PowerCalculator calc2 = new PowerCalculator();

        calc1.calcLosses(1.0, 1.0);
        calc2.calcLosses(2.0, 2.0);

        assertEquals(1.0, calc1.getTotalLosses(), DELTA);
        assertEquals(4.0, calc2.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Realistic Power Scenarios
    // ====================================================

    @Test
    public void testRealistic_100mA_12V() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(0.1, 12.0);
        assertEquals(1.2, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testRealistic_10A_500mV() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(10.0, 0.5);
        assertEquals(5.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testRealistic_50A_400V() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(50.0, 400.0);
        assertEquals(20000.0, calc.getTotalLosses(), DELTA);
    }

    @Test
    public void testRealistic_HighCurrent_LowVoltage() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(100.0, 0.01);
        assertEquals(1.0, calc.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Precision and Floating Point
    // ====================================================

    @Test
    public void testPrecision_FractionalSum() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(0.1 + 0.2, 3.0);
        assertEquals(0.9, calc.getTotalLosses(), 1e-6);
    }

    @Test
    public void testPrecision_ExtremeScaleRatio() {
        PowerCalculator calc = new PowerCalculator();
        calc.calcLosses(1e-10, 1e10);
        assertEquals(1.0, calc.getTotalLosses(), 1e-6);
    }

}

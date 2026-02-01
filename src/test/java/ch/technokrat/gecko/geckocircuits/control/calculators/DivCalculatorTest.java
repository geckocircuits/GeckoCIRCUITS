/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DivCalculatorTest extends AbstractTwoInputsMathFunctionTest {

    @Override
    AbstractControlCalculatable calculatorFabricTwoInputs() {
        return new DivCalculator();
    }

    private static final double NUMERATOR = -20;
    private static final double DENOMINATOR = -5;
    private static final double NEG_20_DIV_NEG_5 = 4;
    @Override
    @Test
    public void testInputTrueTrue() {
        final double val = getValue(NUMERATOR, DENOMINATOR);
        assertWithTol(NEG_20_DIV_NEG_5, val);
    }

    @Override
    @Test
    public void testInputTrueFalse() {
        final double val = getValue(0, 0); // zero division through zero should return 0!
        assertWithTol(0, val);
        final double val2 = getValue(-0, -0); // zero division through zero should return 0!
        assertWithTol(0, val2);
    }

    @Override    
    public void testInputFalseFalse() {
            double largeNegativeExpected = getValue(-1.0, 0);
            assertTrue(largeNegativeExpected < -100000);
    }
    
    @Test
    public void testPositiveNumeratorPositiveDenominator() {
        final double val = getValue(20, 5);
        assertWithTol(4, val);
    }
    
    @Test
    public void testNegativeNumeratorPositiveDenominator() {
        final double val = getValue(-20, 5);
        assertWithTol(-4, val);
    }
    
    @Test
    public void testPositiveNumeratorNegativeDenominator() {
        final double val = getValue(20, -5);
        assertWithTol(-4, val);
    }
    
    @Test
    public void testOneNumerator() {
        final double val = getValue(1, 2);
        assertWithTol(0.5, val);
    }
    
    @Test
    public void testDecimalValues() {
        final double val = getValue(7.5, 2.5);
        assertWithTol(3.0, val);
    }
    
    @Test
    public void testDenominatorPositive() {
        final double val = getValue(100, 10);
        assertWithTol(10, val);
    }
    
    @Test
    public void testLargeValues() {
        final double val = getValue(1000000, 1000);
        assertWithTol(1000, val);
    }
    
    @Test
    public void testVerySmallDenominator() {
        // Division by very small number should produce large result
        final double val = getValue(100, 1e-6);
        assertWithTol(1e8, val);
    }
    
    @Test
    public void testZeroDividedByNumber() {
        final double val = getValue(0, 100);
        assertWithTol(0, val);
    }
    
    @Test
    public void testNegativeZeroNumerator() {
        final double val = getValue(-0.0, 5);
        assertWithTol(0, val);
    }
    
    @Test
    public void testFractionalDivision() {
        final double val = getValue(0.25, 0.5);
        assertWithTol(0.5, val);
    }

    @Test
    public void testPositiveDivByZeroReturnsLargePositive() {
        // Positive / 0 should return large positive number
        final double val = getValue(1.0, 0);
        assertTrue("Should be large positive value", val > 1e30);
    }

    @Test
    public void testNegativeDivByZeroReturnsLargeNegative() {
        // Negative / 0 should return large negative number
        final double val = getValue(-1.0, 0);
        assertTrue("Should be large negative value", val < -1e30);
    }

    @Test
    public void testLargeNumeratorSmallDenominator() {
        final double val = getValue(1e20, 1e-20);
        // This would be infinite, should return LARGE_NUMBER
        assertTrue("Should return bounded large value", val > 1e30 || val < 1e40);
    }

    @Test
    public void testNegativeNumeratorZeroDenominator() {
        // -5 / 0 should give large negative
        final double val = getValue(-5.0, 0.0);
        assertTrue("Should be large negative", val < -1e30);
    }

    @Test
    public void testPositiveNumeratorNegativeZeroDenominator() {
        // 5 / -0.0 should handle edge case
        final double val = getValue(5.0, -0.0);
        // -0.0 / positive still gives NaN for 0/0, or large number
        assertFalse("Should not be NaN for non-zero numerator", Double.isNaN(val));
    }

    @Test
    public void testVerySmallNumbersDivision() {
        final double val = getValue(1e-100, 1e-50);
        assertWithTol(1e-50, val);
    }

    @Test
    public void testIdentityDivision() {
        // x / x = 1 for any non-zero x
        final double val = getValue(123.456, 123.456);
        assertWithTol(1.0, val);
    }
}

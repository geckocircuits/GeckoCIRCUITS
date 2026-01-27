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
}

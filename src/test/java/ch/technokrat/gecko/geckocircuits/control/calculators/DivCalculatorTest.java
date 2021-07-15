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
    
}

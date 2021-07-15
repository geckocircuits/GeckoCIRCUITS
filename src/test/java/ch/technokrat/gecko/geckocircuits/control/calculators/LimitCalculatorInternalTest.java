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

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class LimitCalculatorInternalTest {

    LimitCalculatorInternal _limitCalculator;

    @Before
    public void setUp() {
        _limitCalculator = new LimitCalculatorInternal(-5, 5);
    }

    @After
    public void tearDown() {
        _limitCalculator = null;
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetMinMaxValueWithError() {
                _limitCalculator.setMinMaxValues(-10, -6000);
    }            

    @Test
    public void testBerechneYOUT() {
        _limitCalculator.setMinMaxValues(-20, 10);
        
        // test the upper limit
        _limitCalculator._inputSignal[0] = new double[]{20};
        _limitCalculator.berechneYOUT(1e-9);
        assertEquals(10, _limitCalculator._outputSignal[0][0], 1e-9);
        
        // test the lower limit
        _limitCalculator._inputSignal[0] = new double[]{-30};
        _limitCalculator.berechneYOUT(1e-9);
        assertEquals(-20, _limitCalculator._outputSignal[0][0], 1e-9);
        
        // test without limiting signal:
        _limitCalculator._inputSignal[0] = new double[]{-3};
        _limitCalculator.berechneYOUT(1e-9);
        assertEquals(-3, _limitCalculator._outputSignal[0][0], 1e-9);
    }
}

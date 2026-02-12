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
package gecko.geckocircuits.control.calculators;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andreas
 */
public final class ConstantCalculatorTest {
    private static final double CONST_TEST = -1.234;
    private static final double ERROR_THRESHOLD = 1e-10;
    
    private ConstantCalculator _constCalc;                
    
    @Before
    public void setUp() {
        _constCalc = new ConstantCalculator(0);
    }
    

    @Test
    public void testSetConst() {
        _constCalc.setConst(CONST_TEST);
        assertEquals(CONST_TEST, _constCalc._outputSignal[0][0], ERROR_THRESHOLD);
    }

    @Test(expected=AssertionError.class)
    public void testBerechneYOUT() {
            _constCalc.berechneYOUT(1e-9);
    }
}


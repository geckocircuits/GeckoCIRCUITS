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
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andreas
 */
public class LimitCalculatorExternalTest {
    
    private LimitCalculatorExternal limitExternal;
    
    
    
    @Before
    public void setUp() {
        limitExternal = new LimitCalculatorExternal();
        limitExternal._inputSignal[0] = new double[1];
        limitExternal._inputSignal[1] = new double[1];
        limitExternal._inputSignal[2] = new double[1];
    }
    
    @After
    public void tearDown() {
        limitExternal = null;
    }

    @Test
    public void testBerechneYOUT() {
        // test a signal within the limits:
        limitExternal._inputSignal[0][0] = 10;
        limitExternal._inputSignal[1][0] = -20; // minimum
        limitExternal._inputSignal[2][0] = 20; // maximum
        limitExternal.berechneYOUT(1e-9);
        assertEquals(10, limitExternal._outputSignal[0][0], 1e-9);
        
        // test a signal at lower limit:
        limitExternal._inputSignal[0][0] = -20;
        limitExternal._inputSignal[1][0] = -15; // minimum
        limitExternal._inputSignal[2][0] = 20; // maximum
        limitExternal.berechneYOUT(1e-9);
        assertEquals(-15, limitExternal._outputSignal[0][0], 1e-9);
        
        // test a signal at upper limit:
        limitExternal._inputSignal[0][0] = 40;
        limitExternal._inputSignal[1][0] = -15; // minimum
        limitExternal._inputSignal[2][0] = 25; // maximum
        limitExternal.berechneYOUT(1e-9);
        assertEquals(25, limitExternal._outputSignal[0][0], 1e-9);
        
        
    }
}

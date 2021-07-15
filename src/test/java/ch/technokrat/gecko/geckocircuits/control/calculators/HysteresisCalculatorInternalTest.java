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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class HysteresisCalculatorInternalTest {
    private static final double DEFAULT_H_VALUE = -2.0;
    private HysteresisCalculatorInternal hysCalc;
        
    
    @Before
    public void setUp() {
        hysCalc = new HysteresisCalculatorInternal(DEFAULT_H_VALUE);
        hysCalc._inputSignal[0] = new double[]{0};
    }

    @Test
    public void testBerechneYOUT() {
        hysCalc._inputSignal[0][0] = -2.0;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(-1, hysCalc._outputSignal[0][0], 1e-9);
        
        hysCalc._inputSignal[0][0] = -3.0;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(-1, hysCalc._outputSignal[0][0], 1e-9);
        
        // do transition
        hysCalc._inputSignal[0][0] = -1.0;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(1, hysCalc._outputSignal[0][0], 1e-9);
        
        // not yet a transition
        hysCalc._inputSignal[0][0] = -1.9;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(1, hysCalc._outputSignal[0][0], 1e-9);
        
        // not yet a transition
        hysCalc._inputSignal[0][0] = -2;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(-1, hysCalc._outputSignal[0][0], 1e-9);
        
    }

    @Test
    public void testSetHValue() {
        hysCalc.setHValue(1);
        hysCalc._inputSignal[0][0] = 1;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(1, hysCalc._outputSignal[0][0], 1e-9);
        
        hysCalc._inputSignal[0][0] = 2;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(1, hysCalc._outputSignal[0][0], 1e-9);
        
        // do transition
        hysCalc._inputSignal[0][0] = -1;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(1, hysCalc._outputSignal[0][0], 1e-9);
        
        // no transition!
        hysCalc._inputSignal[0][0] = -1.1;
        hysCalc.berechneYOUT(1e-8);
        assertEquals(-1, hysCalc._outputSignal[0][0], 1e-9);
                
    }
}

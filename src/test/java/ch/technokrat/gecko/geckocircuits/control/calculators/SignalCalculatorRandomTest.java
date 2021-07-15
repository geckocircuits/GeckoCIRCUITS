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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author andy
 */
public class SignalCalculatorRandomTest {
    private SignalCalculatorRandom _randomCalculator;
    
    @Before
    public void setUp() {
        _randomCalculator = new SignalCalculatorRandom();
    }
    
    @Test
    public void testBerechneYOUT() {
        
        double oldValue = -1;
        for(int i = 0; i < 200; i++) {
            _randomCalculator.berechneYOUT(1);
            double value = _randomCalculator._outputSignal[0][0];
            // very basic test: the output should never stay constant...
            assertTrue(value != oldValue);
            oldValue = value;
        }                    
    }
}

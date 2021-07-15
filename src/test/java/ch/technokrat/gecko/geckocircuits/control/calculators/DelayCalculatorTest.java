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

public final class DelayCalculatorTest {
    private static final double INIT_DEL_TIME = 4.0; // seconds
    private DelayCalculator _delCalculator;

    @Before
    public void setUp() {
        _delCalculator = new DelayCalculator(INIT_DEL_TIME);
        _delCalculator._inputSignal[0] = new double[]{0};
    }

    @Test
    public void testInitWithNewDt() {
        // same caculation / test as in testBerechneYOUT
        _delCalculator.initializeAtSimulationStart(1);
        for (int i = 0; i < 10; i++) {
            _delCalculator._inputSignal[0][0] = 10 - i;
            _delCalculator.berechneYOUT(1);
        }
        
        assertEquals(5, _delCalculator._outputSignal[0][0], 1e-20);
        
        // continue with other delay value:
        double newDelayTime = 0.6;
        _delCalculator.initWithNewDt(newDelayTime);

        for (int i = 0; i < 11; i++) {
            _delCalculator._inputSignal[0][0] = 10 - i;
            if(i == 3) { // test if values from the previous delay period are translated correctly!
                assertEquals(3, _delCalculator._outputSignal[0][0], 1e-20);
            }
            _delCalculator.berechneYOUT(1);
        }
        assertEquals(6, _delCalculator._outputSignal[0][0], 1e-20);
        
    }

    @Test
    public void testBerechneYOUT() {
        _delCalculator.initializeAtSimulationStart(1);
        for (int i = 0; i < 10; i++) {
            _delCalculator._inputSignal[0][0] = 10 - i;
            _delCalculator.berechneYOUT(1);
            if(i < 4) { // test if the values are zero at the beginning of delay calculation (t < tDelay)
                assertEquals(0, _delCalculator._outputSignal[0][0], 1e-20);
            } else {
                assertEquals(10 - (i - 4), _delCalculator._outputSignal[0][0], 1e-20);
            }
            
        }        
        assertEquals(5, _delCalculator._outputSignal[0][0], 1e-20);

    }

    @Test(expected=Exception.class)
    public void setNegativeDelay() {
            _delCalculator.setDelayTime(-1); // negative values not allowed!
    }
    
    
    @Test
    public void testSetDelayTime() {
        double newDelayTime = 0.6;
        _delCalculator.initializeAtSimulationStart(newDelayTime);

        for (int i = 0; i < 11; i++) {
            _delCalculator._inputSignal[0][0] = 10 - i;
            
            if(i == 5) { // setting the delay during the simulation should NOT change the result, since
                // the delay must be given at the simulation start!
                _delCalculator.setDelayTime(4);
            }
            
            _delCalculator.berechneYOUT(1);
        }
        assertEquals(6, _delCalculator._outputSignal[0][0], 1e-20);       

    }
}

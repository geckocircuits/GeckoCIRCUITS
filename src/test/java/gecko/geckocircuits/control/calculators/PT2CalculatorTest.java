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

public class PT2CalculatorTest {
    private static final double DELTA_T = 1e-4;
    private PT2Calculator _calculator;
    private static final double END_TIME = 1;
    
    @Before
    public void setUp() {
        _calculator = new PT2Calculator(1, 1);
        _calculator._inputSignal[0] = new double[]{0};
    }
    

    @Test
    public void testBerechneYOUT() {
        _calculator._inputSignal[0][0] = 1;
        for(double time = 0; time < END_TIME; time+= DELTA_T) {
            _calculator.berechneYOUT(DELTA_T);
            final double result = _calculator._outputSignal[0][0];
            final double expected = 1 - Math.cos(time);            
            if(time > END_TIME/2) { // the first value is nonsense!                
                assertEquals(expected, result, 1e-4);
            }            
        }        
    }

    @Test
    public void testSetTimeConstant() {
        final double newTimeConstant = 0.5;
        _calculator.setTimeConstant(newTimeConstant);
        _calculator._inputSignal[0][0] = 1;
        for(double time = 0; time < END_TIME; time+= DELTA_T) {
            _calculator.berechneYOUT(DELTA_T);
            final double result = _calculator._outputSignal[0][0];
            final double expected = 1 - Math.cos(time / newTimeConstant);            
            if(time > END_TIME/2) { // the first value is nonsense!                
                assertEquals(expected, result, 1e-3);
            }            
        }
        
    }

    @Test
    public void testSetGain() {
        _calculator._inputSignal[0][0] = 1;
        final double newGain = -2;
        _calculator.setGain(newGain);
        for(double time = 0; time < END_TIME; time+= DELTA_T) {
            _calculator.berechneYOUT(DELTA_T);
            final double result = _calculator._outputSignal[0][0];
            final double expected = newGain * (1 - Math.cos(time));            
            if(time > END_TIME/2) { // the first value is nonsense!                
                assertEquals(expected, result, 1e-4);
            }            
        }
        
    }
}

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

public class PDCalculatorTest {
    private static final double DELTA_T = 1e-6;
    private static final double ACCURACY = 1e-4;
    private static final double END_TIME = 0.2;
    private PDCalculator _pdCalculator;
        
    
    @Before
    public void setUp() {
        _pdCalculator = new PDCalculator(1);
        _pdCalculator._inputSignal[0] = new double[]{0};
    }


    @Test
    public void testBerechneYOUT() {
        // use 2*sin(5 * time) as input signal
        // the result should be 10 * cos(time) (differentiation)
        for(double time = 0; time < END_TIME; time+= DELTA_T) {
            final double value = 2 * Math.sin(5 * time);
            _pdCalculator._inputSignal[0][0] = value;            
            _pdCalculator.berechneYOUT(DELTA_T);
            final double result = _pdCalculator._outputSignal[0][0];            
            final double expected = 10 * Math.cos(5 * time);                        
            
            if(time > 0) { // the first value is nonsense...!
                assertEquals(expected, result, ACCURACY);
            }            
        }
        
    }

    @Test
    public void testSetGain() {
        _pdCalculator.setGain(-2);
        // use -2 * 2*sin(5 * time) as input signal
        // the result should be -2 * 10 * cos(time) (differentiation)
        for(double time = 0; time < END_TIME; time+= DELTA_T) {
            double value = 2 * Math.sin(5 * time);
            _pdCalculator._inputSignal[0][0] = value;            
            _pdCalculator.berechneYOUT(DELTA_T);
            double result = _pdCalculator._outputSignal[0][0];            
            double expected = -20 * Math.cos(5 * time);                        
            
            if(time > 0) { // the first value is nonsense...!
                assertEquals(expected, result, ACCURACY);
            }            
        }
    }
}

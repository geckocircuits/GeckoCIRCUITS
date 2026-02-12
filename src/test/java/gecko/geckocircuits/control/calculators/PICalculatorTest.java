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
import org.junit.Test;

/**
 *
 * @author andreas
 */
public class PICalculatorTest extends AbstractSimpleMathFunctionTest {
    private PICalculator _calculator;

    @Override
    AbstractControlCalculatable calculatorFabric() {
        _calculator = new PICalculator(1, 1);
        return _calculator;
    }

    @Override
    @Test
    public void testBerechneYOUTResult0() {
        for(int i = 0; i < 10; i++) {
            double val = getValue(0);
            assertWithTol(0, val);            
        }
    }

    @Override
    public void testBerechneYOUTValue() {
        _calculator.setA1(0);
        _calculator.setR0(2);
        for(int i = 0; i < 10; i++) { // should behave like constant gain of R0 = 2;
            double val = getValue(3);
            assertWithTol(6, val); // 3 * 2 = 6           
        }
        
        _calculator.setA1(1);
        _calculator.setR0(0);
        double dt = 1e-6;
        for(int i = 0; i < 100; i++) { // pure integration
            double val = getValue(i);
            _calculator._inputSignal[0][0] = i;
            _calculator.berechneYOUT(dt);
            double expected = 0.5 * dt * i * (i-1); // integral over i is 0.5 * i*i
            assertEquals(expected, val, 1e-5); // 3 * 2 = 6           
        }
        
    }

    @Override
    public void testErrorValue() {
        // no error possible in PI calculator
    }
    
    
}

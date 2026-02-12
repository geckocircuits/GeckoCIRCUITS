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

import org.junit.Test;

public class GainCalculatorTest extends AbstractSimpleMathFunctionTest {        
    private static double DEFAULT_GAIN = 3.0;
    private GainCalculator _calculator;
    
    @Override            
    AbstractControlCalculatable calculatorFabric() {
        _calculator = new GainCalculator(DEFAULT_GAIN);
        return _calculator;
    }

    @Override
    @Test
    public void testBerechneYOUTResult0() {
        double val = getValue(0);
        assertWithTol(0, 0);
    }

    @Override
    @Test
    public void testBerechneYOUTValue() {
        double val = getValue(10);
        assertWithTol(10 * DEFAULT_GAIN, val);
    }

    @Override    
    public void testErrorValue() {
        // no error value possible for gain!
    }
    
    @Test
    public void setGainValue() {
        _calculator.setGain(20);
        double val = getValue(10);
        assertWithTol(10 * 20, val);
        
        // reset, so that the other test are not influenced!
        _calculator.setGain(DEFAULT_GAIN);
    }
    
}
